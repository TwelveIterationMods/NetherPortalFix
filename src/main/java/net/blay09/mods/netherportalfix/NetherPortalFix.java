package net.blay09.mods.netherportalfix;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;

@Mod("netherportalfix")
public class NetherPortalFix {

    private static final int MAX_PORTAL_DISTANCE_SQ = 16;
    private static final String NETHER_PORTAL_FIX = "NetherPortalFix";
    private static final String SCHEDULED_TELEPORT = "NPFScheduledTeleport";
    private static final String FROM = "From";
    private static final String FROM_DIM = "FromDim";
    private static final String TO = "To";
    private static final String TO_DIM = "ToDim";

    public NetherPortalFix() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if (player.getEntityData().hasKey(SCHEDULED_TELEPORT)) {
                return;
            }

            BlockPos fromPos = player.lastPortalPos;
            if (fromPos == null || player.getPosition().getDistance(fromPos.getX(), fromPos.getY(), fromPos.getZ()) > 2) {
                player.lastPortalPos = null;
                return;
            }

            DimensionType fromDim = event.getEntity().dimension;
            DimensionType toDim = event.getDimension();
            if ((fromDim == DimensionType.OVERWORLD && toDim == DimensionType.NETHER) || (fromDim == DimensionType.NETHER && toDim == DimensionType.OVERWORLD)) {
                NBTTagList portalList = getPlayerPortalList(player);
                NBTTagCompound returnPortal = findReturnPortal(portalList, fromPos, fromDim);
                if (returnPortal != null) {
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server != null) {
                        World toWorld = server.getWorld(toDim);
                        BlockPos toPos = BlockPos.fromLong(returnPortal.getLong(TO));

                        // Find the lowest possible portal block to prevent any (literal) headaches
                        BlockPos tryPos;
                        while (true) {
                            tryPos = toPos.offset(EnumFacing.DOWN);
                            if (toWorld.getBlockState(tryPos).getBlock() == Blocks.NETHER_PORTAL) {
                                toPos = tryPos;
                            } else {
                                break;
                            }
                        }

                        if (toWorld.getBlockState(toPos).getBlock() == Blocks.NETHER_PORTAL) {
                            NBTTagCompound tagCompound = new NBTTagCompound();

                            ResourceLocation dimensionRegistryName = DimensionType.func_212678_a(toDim);
                            tagCompound.setString(TO_DIM, String.valueOf(dimensionRegistryName));

                            tagCompound.setLong(TO, toPos.toLong());
                            player.getEntityData().setTag(SCHEDULED_TELEPORT, tagCompound);
                            event.setCanceled(true);
                        } else {
                            player.sendStatusMessage(new TextComponentTranslation("netherportalfix:portal_destroyed"), true);
                            removeReturnPortal(portalList, returnPortal);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            NBTTagCompound entityData = event.player.getEntityData();
            if (entityData.hasKey(SCHEDULED_TELEPORT)) {
                NBTTagCompound data = entityData.getCompound(SCHEDULED_TELEPORT);
                DimensionType toDim = DimensionType.byName(new ResourceLocation(data.getString(TO_DIM)));
                if (toDim == null) {
                    toDim = DimensionType.OVERWORLD;
                }

                // Fire Forge event - our event handler will ignore it due to SCHEDULED_TELEPORT tag. If this is cancelled, do not teleport at all.
                EntityTravelToDimensionEvent travelEvent = new EntityTravelToDimensionEvent(event.player, toDim);
                if (MinecraftForge.EVENT_BUS.post(travelEvent)) {
                    entityData.removeTag(SCHEDULED_TELEPORT);
                    return;
                }

                entityData.removeTag(SCHEDULED_TELEPORT);

                MinecraftServer server = event.player.getEntityWorld().getServer();
                if (server != null) {
                    NetherPortalFixTeleporter teleporter = new NetherPortalFixTeleporter(BlockPos.fromLong(data.getLong(TO)), toDim);
                    teleporter.teleport(event.player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if ((event.getFrom() == DimensionType.OVERWORLD && event.getTo() == DimensionType.NETHER) || (event.getFrom() == DimensionType.NETHER && event.getTo() == DimensionType.OVERWORLD)) {
            EntityPlayer player = event.getPlayer();
            BlockPos fromPos = player.lastPortalPos;
            if (fromPos == null) {
                return;
            }

            BlockPos toPos = new BlockPos(player.posX, player.posY, player.posZ);
            NBTTagList portalList = getPlayerPortalList(player);
            storeReturnPortal(portalList, toPos, event.getTo(), fromPos);
        }
    }

    private NBTTagList getPlayerPortalList(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData().getCompound(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList list = data.getList(NETHER_PORTAL_FIX, Constants.NBT.TAG_COMPOUND);
        data.setTag(NETHER_PORTAL_FIX, list);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, data);
        return list;
    }

    @Nullable
    private NBTTagCompound findReturnPortal(NBTTagList portalList, BlockPos triggerPos, DimensionType triggerDim) {
        for (INBTBase entry : portalList) {
            NBTTagCompound portal = (NBTTagCompound) entry;
            DimensionType fromDim = DimensionType.byName(new ResourceLocation(portal.getString(FROM_DIM)));
            if (fromDim == triggerDim) {
                BlockPos portalTrigger = BlockPos.fromLong(portal.getLong(FROM));
                if (portalTrigger.distanceSq(triggerPos) <= MAX_PORTAL_DISTANCE_SQ) {
                    return portal;
                }
            }
        }

        return null;
    }

    private void storeReturnPortal(NBTTagList portalList, BlockPos triggerPos, DimensionType triggerDim, BlockPos returnPos) {
        NBTTagCompound found = findReturnPortal(portalList, triggerPos, triggerDim);
        if (found == null) {
            NBTTagCompound portalCompound = new NBTTagCompound();
            portalCompound.setLong(FROM, triggerPos.toLong());

            // getRegistryName returns null for Vanilla dimensions, so we need to use this instead
            ResourceLocation dimensionRegistryName = DimensionType.func_212678_a(triggerDim);
            portalCompound.setString(FROM_DIM, String.valueOf(dimensionRegistryName));

            portalCompound.setLong(TO, returnPos.toLong());
            portalList.add(portalCompound);
        } else {
            BlockPos portalReturnPos = BlockPos.fromLong(found.getLong(TO));
            if (portalReturnPos.distanceSq(returnPos) > MAX_PORTAL_DISTANCE_SQ) {
                found.setLong(TO, returnPos.toLong());
            }
        }
    }

    private void removeReturnPortal(NBTTagList portalList, NBTTagCompound portal) {
        for (int i = 0; i < portalList.size(); i++) {
            if (portalList.get(i) == portal) {
                portalList.removeTag(i);
                break;
            }
        }
    }
}
