package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class NetherPortalFix {

    public static final String MOD_ID = "netherportalfix";

    private static final Logger logger = LogManager.getLogger();

    private static final int MAX_PORTAL_DISTANCE_SQ = 16;
    private static final String NETHER_PORTAL_FIX = "NetherPortalFix";
    private static final String SCHEDULED_TELEPORT = "NPFScheduledTeleport";
    private static final String FROM = "From";
    private static final String FROM_DIM = "FromDim";
    private static final String TO = "To";
    private static final String TO_DIM = "ToDim";

    public static void initialize() {
        Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.End, NetherPortalFix::onPlayerTick);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().contains(SCHEDULED_TELEPORT)) {
                logger.debug("Skipping EntityTravelToDimensionEvent as we triggered it");
                return;
            }

            BlockPos fromPos = player.lastPos;
            final ResourceKey<Level> fromDim = player.level.dimension();
            final ResourceKey<Level> toDim = event.getDimension();
            final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
            final ResourceKey<Level> THE_NETHER = Level.NETHER;
            boolean isPlayerCurrentlyInPortal = player.isInsidePortal;
            boolean isTeleportBetweenNetherAndOverworld = (fromDim == OVERWORLD && toDim == THE_NETHER)
                    || (fromDim == THE_NETHER && toDim == OVERWORLD);
            if (isPlayerCurrentlyInPortal && isTeleportBetweenNetherAndOverworld) {
                ListTag portalList = getPlayerPortalList(player);
                CompoundTag returnPortal = findReturnPortal(portalList, fromPos, fromDim);
                if (returnPortal != null) {
                    MinecraftServer server = player.getServer();
                    if (server != null) {
                        Level toLevel = server.getLevel(toDim);
                        BlockPos toPos = BlockPos.of(returnPortal.getLong(TO));

                        // Find the lowest possible portal block to prevent any (literal) headaches
                        BlockPos tryPos;
                        while (true) {
                            tryPos = toPos.relative(Direction.DOWN);
                            if (toLevel.getBlockState(tryPos).getBlock() == Blocks.NETHER_PORTAL) {
                                toPos = tryPos;
                            } else {
                                break;
                            }
                        }

                        boolean foundNetherPortalAtTargetLocation = toLevel.getBlockState(toPos).getBlock() == Blocks.NETHER_PORTAL;
                        if (foundNetherPortalAtTargetLocation) {
                            CompoundTag tagCompound = new CompoundTag();

                            ResourceLocation dimensionRegistryName = toDim.location();
                            tagCompound.putString(TO_DIM, String.valueOf(dimensionRegistryName));

                            tagCompound.putLong(TO, toPos.asLong());
                            player.getPersistentData().put(SCHEDULED_TELEPORT, tagCompound);
                            logger.debug("Found return portal, scheduling teleport");
                            event.setCanceled(true);
                        } else {
                            player.displayClientMessage(new TranslatableComponent("netherportalfix:portal_destroyed"), true);
                            removeReturnPortal(portalList, returnPortal);
                        }
                    }
                } else {
                    logger.debug("No return portal found");
                }
            }
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        CompoundTag entityData = Balm.getHooks().getPersistentData(player);
        if (entityData.contains(SCHEDULED_TELEPORT)) {
            CompoundTag data = entityData.getCompound(SCHEDULED_TELEPORT);
            ResourceKey<Level> toDim = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(data.getString(TO_DIM)));

            MinecraftServer server = player.getServer();
            if (server != null) {
                BlockPos pos = BlockPos.of(data.getLong(TO));
                ServerLevel toLevel = server.getLevel(toDim);
                if (toLevel != null) {
                    teleport(player, pos, toLevel);
                }
                player.isInsidePortal = false;
            }

            entityData.remove(SCHEDULED_TELEPORT);
        }
    }

    private static void teleport(ServerPlayer player, BlockPos pos, ServerLevel toWorld) {
        player.teleportTo(toWorld, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, player.getYRot(), player.getXRot());

        // Resync some things that Vanilla is missing:
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effectInstance));
        }
        player.setExperienceLevels(player.experienceLevel);
    }

    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
        final ResourceKey<Level> THE_NETHER = Level.NETHER;
        if ((event.getFrom() == OVERWORLD && event.getTo() == THE_NETHER) || (event.getFrom() == THE_NETHER && event.getTo() == OVERWORLD)) {
            Player player = event.getPlayer();

            // If prevBlockpos is not set, the player has spawned in the Nether, so we shouldn't look for a return
            // portal to store
            if (player.lastPos == null) {
                return;
            }

            BlockPos fromPos = findPortalAt(player, event.getFrom(), player.lastPos);
            BlockPos toPos = player.blockPosition();
            if (fromPos != null) {
                ListTag portalList = getPlayerPortalList(player);
                storeReturnPortal(portalList, toPos, event.getTo(), fromPos);
                logger.debug("Storing return portal from " + event.getTo() + " to " + fromPos + " in " + event.getFrom());
            } else {
                logger.debug("Not storing return portal because I'm not in a portal.");
            }
        } else {
            logger.debug("Not storing return portal because it's from " + event.getFrom() + " to " + event.getTo());
        }
    }

    private BlockPos findPortalAt(Player player, ResourceKey<Level> dimensionType, BlockPos pos) {
        MinecraftServer server = player.level.getServer();
        if (server != null) {
            ServerLevel fromWorld = server.getLevel(dimensionType);
            if (fromWorld.getBlockState(pos).getBlock() == Blocks.NETHER_PORTAL) {
                return pos;
            }

            for (Direction value : Direction.values()) {
                BlockPos offsetPos = pos.relative(value);
                if (fromWorld.getBlockState(offsetPos).getBlock() == Blocks.NETHER_PORTAL) {
                    return offsetPos;
                }
            }
        }
        return null;
    }

    private ListTag getPlayerPortalList(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = data.getList(NETHER_PORTAL_FIX, Tag.TAG_COMPOUND);
        data.put(NETHER_PORTAL_FIX, list);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        return list;
    }

    @Nullable
    private CompoundTag findReturnPortal(ListTag portalList, BlockPos triggerPos, ResourceKey<Level> triggerDim) {
        for (Tag entry : portalList) {
            CompoundTag portal = (CompoundTag) entry;
            ResourceKey<Level> fromDim = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(portal.getString(FROM_DIM)));
            if (fromDim == triggerDim) {
                BlockPos portalTrigger = BlockPos.of(portal.getLong(FROM));
                if (portalTrigger.distSqr(triggerPos) <= MAX_PORTAL_DISTANCE_SQ) {
                    return portal;
                }
            }
        }

        return null;
    }

    private void storeReturnPortal(ListTag portalList, BlockPos triggerPos, ResourceKey<Level> triggerDim, BlockPos returnPos) {
        CompoundTag found = findReturnPortal(portalList, triggerPos, triggerDim);
        if (found == null) {
            CompoundTag portalCompound = new CompoundTag();
            portalCompound.putLong(FROM, triggerPos.asLong());

            portalCompound.putString(FROM_DIM, String.valueOf(triggerDim.location()));

            portalCompound.putLong(TO, returnPos.asLong());
            portalList.add(portalCompound);
        } else {
            BlockPos portalReturnPos = BlockPos.of(found.getLong(TO));
            if (portalReturnPos.distSqr(returnPos) > MAX_PORTAL_DISTANCE_SQ) {
                found.putLong(TO, returnPos.asLong());
            }
        }
    }

    private void removeReturnPortal(ListTag portalList, CompoundTag portal) {
        for (int i = 0; i < portalList.size(); i++) {
            if (portalList.get(i) == portal) {
                portalList.remove(i);
                break;
            }
        }
    }
}
