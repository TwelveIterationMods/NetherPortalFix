package net.blay09.mods.netherportalfix;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@Mod("netherportalfix")
public class NetherPortalFix {

    private static final Logger logger = LogManager.getLogger();

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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            if (player.getPersistentData().contains(SCHEDULED_TELEPORT)) {
                logger.debug("Skipping EntityTravelToDimensionEvent as we triggered it");
                return;
            }

            BlockPos fromPos = player.prevBlockpos;
            final RegistryKey<World> fromDim = event.getEntity().world.func_234923_W_(); // getDimension()
            final RegistryKey<World> toDim = event.getDimension();
            final RegistryKey<World> OVERWORLD = World.field_234918_g_;
            final RegistryKey<World> THE_NETHER = World.field_234919_h_;
            boolean isPlayerCurrentlyInPortal = player.inPortal;
            boolean isTeleportBetweenNetherAndOverworld = (fromDim == OVERWORLD && toDim == THE_NETHER)
                    || (fromDim == THE_NETHER && toDim == OVERWORLD);
            if (isPlayerCurrentlyInPortal && isTeleportBetweenNetherAndOverworld) {
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

                        boolean foundNetherPortalAtTargetLocation = toWorld.getBlockState(toPos).getBlock() == Blocks.NETHER_PORTAL;
                        if (foundNetherPortalAtTargetLocation) {
                            CompoundNBT tagCompound = new CompoundNBT();

                            ResourceLocation dimensionRegistryName = toDim.func_240901_a_();
                            tagCompound.putString(TO_DIM, String.valueOf(dimensionRegistryName));

                            tagCompound.putLong(TO, toPos.toLong());
                            player.getPersistentData().put(SCHEDULED_TELEPORT, tagCompound);
                            logger.debug("Found return portal, scheduling teleport");
                            event.setCanceled(true);
                        } else {
                            player.sendStatusMessage(new TranslationTextComponent("netherportalfix:portal_destroyed"), true);
                            removeReturnPortal(portalList, returnPortal);
                        }
                    }
                } else {
                    logger.debug("No return portal found");
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
                RegistryKey<World> toDim = RegistryKey.func_240903_a_(Registry.field_239699_ae_, new ResourceLocation(data.getString(TO_DIM)));

                MinecraftServer server = event.player.getEntityWorld().getServer();
                if (server != null) {
                    BlockPos pos = BlockPos.fromLong(data.getLong(TO));
                    ServerWorld toWorld = server.getWorld(toDim);
                    if (toWorld != null) {
                        teleport(((ServerPlayerEntity) event.player), pos, toWorld);
                    }
                    event.player.inPortal = false;
                }

                entityData.remove(SCHEDULED_TELEPORT);
            }
        }
    }

    private void teleport(ServerPlayerEntity player, BlockPos pos, ServerWorld toWorld) {
        player.teleport(toWorld, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, player.rotationYaw, player.rotationPitch);

        // Resync some things that Vanilla is missing:
        for (EffectInstance effectinstance : player.getActivePotionEffects()) {
            player.connection.sendPacket(new SPlayEntityEffectPacket(player.getEntityId(), effectinstance));
        }
        player.setExperienceLevel(player.experienceLevel);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        final RegistryKey<World> OVERWORLD = World.field_234918_g_;
        final RegistryKey<World> THE_NETHER = World.field_234919_h_;
        if ((event.getFrom() == OVERWORLD && event.getTo() == THE_NETHER) || (event.getFrom() == THE_NETHER && event.getTo() == OVERWORLD)) {
            PlayerEntity player = event.getPlayer();

            // If prevBlockpos is not set, the player has spawned in the Nether, so we shouldn't look for a return
            // portal to store
            if (player.prevBlockpos == null) {
                return;
            }

            BlockPos fromPos = findPortalAt(player, event.getFrom(), player.prevBlockpos);
            BlockPos toPos = player.func_233580_cy_(); // getPosition()
            if (fromPos != null) {
                ListNBT portalList = getPlayerPortalList(player);
                storeReturnPortal(portalList, toPos, event.getTo(), fromPos);
                logger.debug("Storing return portal from " + event.getTo() + " to " + fromPos + " in " + event.getFrom());
            } else {
                logger.debug("Not storing return portal because I'm not in a portal.");
            }
        } else {
            logger.debug("Not storing return portal because it's from " + event.getFrom() + " to " + event.getTo());
        }
    }

    private BlockPos findPortalAt(PlayerEntity player, RegistryKey<World> dimensionType, BlockPos pos) {
        MinecraftServer server = player.world.getServer();
        if (server != null) {
            ServerWorld fromWorld = server.getWorld(dimensionType);
            if (fromWorld.getBlockState(pos).getBlock() == Blocks.NETHER_PORTAL) {
                return pos;
            }

            for (Direction value : Direction.values()) {
                BlockPos offsetPos = pos.offset(value);
                if (fromWorld.getBlockState(offsetPos).getBlock() == Blocks.NETHER_PORTAL) {
                    return offsetPos;
                }
            }
        }
        return null;
    }

    private ListNBT getPlayerPortalList(PlayerEntity player) {
        CompoundNBT data = player.getPersistentData().getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        ListNBT list = data.getList(NETHER_PORTAL_FIX, Constants.NBT.TAG_COMPOUND);
        data.put(NETHER_PORTAL_FIX, list);
        player.getPersistentData().put(PlayerEntity.PERSISTED_NBT_TAG, data);
        return list;
    }

    @Nullable
    private CompoundNBT findReturnPortal(ListNBT portalList, BlockPos triggerPos, RegistryKey<World> triggerDim) {
        for (INBT entry : portalList) {
            CompoundNBT portal = (CompoundNBT) entry;
            RegistryKey<World> fromDim = RegistryKey.func_240903_a_(Registry.field_239699_ae_, new ResourceLocation(portal.getString(FROM_DIM)));
            if (fromDim == triggerDim) {
                BlockPos portalTrigger = BlockPos.fromLong(portal.getLong(FROM));
                if (portalTrigger.distanceSq(triggerPos) <= MAX_PORTAL_DISTANCE_SQ) {
                    return portal;
                }
            }
        }

        return null;
    }

    private void storeReturnPortal(ListNBT portalList, BlockPos triggerPos, RegistryKey<World> triggerDim, BlockPos returnPos) {
        CompoundNBT found = findReturnPortal(portalList, triggerPos, triggerDim);
        if (found == null) {
            CompoundNBT portalCompound = new CompoundNBT();
            portalCompound.putLong(FROM, triggerPos.toLong());

            portalCompound.putString(FROM_DIM, String.valueOf(triggerDim.func_240901_a_())); // getResourceLocation()

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
