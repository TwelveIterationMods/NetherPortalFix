package net.blay09.mods.netherportalfix;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

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
        if (event.getEntity() instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            if (player.getPersistentData().contains(SCHEDULED_TELEPORT)) {
                return;
            }

            BlockPos fromPos = player.lastPortalPos;
            if (fromPos == null || player.getPosition().distanceSq(fromPos) > 2) {
                player.lastPortalPos = null;
                return;
            }

            DimensionType fromDim = event.getEntity().dimension;
            DimensionType toDim = event.getDimension();
            if ((fromDim == DimensionType.OVERWORLD && toDim == DimensionType.THE_NETHER) || (fromDim == DimensionType.THE_NETHER && toDim == DimensionType.OVERWORLD)) {
                ListNBT portalList = getPlayerPortalList(player);
                CompoundNBT returnPortal = findReturnPortal(portalList, fromPos, fromDim);
                if (returnPortal != null) {
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server != null) {
                        World toWorld = server.getWorld(toDim);
                        BlockPos toPos = BlockPos.fromLong(returnPortal.getLong(TO));

                        // Find the lowest possible portal block to prevent any (literal) headaches
                        BlockPos tryPos;
                        while (true) {
                            tryPos = toPos.offset(Direction.DOWN);
                            if (toWorld.getBlockState(tryPos).getBlock() == Blocks.NETHER_PORTAL) {
                                toPos = tryPos;
                            } else {
                                break;
                            }
                        }

                        if (toWorld.getBlockState(toPos).getBlock() == Blocks.NETHER_PORTAL) {
                            CompoundNBT tagCompound = new CompoundNBT();

                            ResourceLocation dimensionRegistryName = DimensionType.getKey(toDim);
                            tagCompound.putString(TO_DIM, String.valueOf(dimensionRegistryName));

                            tagCompound.putLong(TO, toPos.toLong());
                            player.getPersistentData().put(SCHEDULED_TELEPORT, tagCompound);
                            event.setCanceled(true);
                        } else {
                            player.sendStatusMessage(new TranslationTextComponent("netherportalfix:portal_destroyed"), true);
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
            CompoundNBT entityData = event.player.getPersistentData();
            if (entityData.contains(SCHEDULED_TELEPORT)) {
                CompoundNBT data = entityData.getCompound(SCHEDULED_TELEPORT);
                DimensionType toDim = DimensionType.byName(new ResourceLocation(data.getString(TO_DIM)));
                if (toDim == null) {
                    toDim = DimensionType.OVERWORLD;
                }

                // Fire Forge event - our event handler will ignore it due to SCHEDULED_TELEPORT tag. If this is cancelled, do not teleport at all.
                EntityTravelToDimensionEvent travelEvent = new EntityTravelToDimensionEvent(event.player, toDim);
                if (MinecraftForge.EVENT_BUS.post(travelEvent)) {
                    entityData.remove(SCHEDULED_TELEPORT);
                    return;
                }

                entityData.remove(SCHEDULED_TELEPORT);

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
        if ((event.getFrom() == DimensionType.OVERWORLD && event.getTo() == DimensionType.THE_NETHER) || (event.getFrom() == DimensionType.THE_NETHER && event.getTo() == DimensionType.OVERWORLD)) {
            PlayerEntity player = event.getPlayer();
            BlockPos fromPos = player.lastPortalPos;
            if (fromPos == null) {
                return;
            }

            BlockPos toPos = new BlockPos(player.posX, player.posY, player.posZ);
            ListNBT portalList = getPlayerPortalList(player);
            storeReturnPortal(portalList, toPos, event.getTo(), fromPos);
        }
    }

    private ListNBT getPlayerPortalList(PlayerEntity player) {
        CompoundNBT data = player.getPersistentData().getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        ListNBT list = data.getList(NETHER_PORTAL_FIX, Constants.NBT.TAG_COMPOUND);
        data.put(NETHER_PORTAL_FIX, list);
        player.getPersistentData().put(PlayerEntity.PERSISTED_NBT_TAG, data);
        return list;
    }

    @Nullable
    private CompoundNBT findReturnPortal(ListNBT portalList, BlockPos triggerPos, DimensionType triggerDim) {
        for (INBT entry : portalList) {
            CompoundNBT portal = (CompoundNBT) entry;
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

    private void storeReturnPortal(ListNBT portalList, BlockPos triggerPos, DimensionType triggerDim, BlockPos returnPos) {
        CompoundNBT found = findReturnPortal(portalList, triggerPos, triggerDim);
        if (found == null) {
            CompoundNBT portalCompound = new CompoundNBT();
            portalCompound.putLong(FROM, triggerPos.toLong());

            // getRegistryName returns null for Vanilla dimensions, so we need to use this instead
            ResourceLocation dimensionRegistryName = DimensionType.getKey(triggerDim);
            portalCompound.putString(FROM_DIM, String.valueOf(dimensionRegistryName));

            portalCompound.putLong(TO, returnPos.toLong());
            portalList.add(portalCompound);
        } else {
            BlockPos portalReturnPos = BlockPos.fromLong(found.getLong(TO));
            if (portalReturnPos.distanceSq(returnPos) > MAX_PORTAL_DISTANCE_SQ) {
                found.putLong(TO, returnPos.toLong());
            }
        }
    }

    private void removeReturnPortal(ListNBT portalList, CompoundNBT portal) {
        for (int i = 0; i < portalList.size(); i++) {
            if (portalList.get(i) == portal) {
                portalList.remove(i);
                break;
            }
        }
    }
}
