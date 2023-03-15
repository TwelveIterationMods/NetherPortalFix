package net.blay09.mods.netherportalfix.mixin;

import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.blay09.mods.netherportalfix.ReturnPortalManager;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ForgeServerPlayerMixin {

    private static final ThreadLocal<ResourceKey<Level>> fromDimHolder = new ThreadLocal<>();

    @Inject(remap = false, method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"))
    public void changeDimensionHead(ServerLevel level, ITeleporter teleporter, CallbackInfoReturnable<Entity> callbackInfo) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        fromDimHolder.set(player.level.dimension());
    }

    @Inject(remap = false, method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    public void changeDimensionTail(ServerLevel level, ITeleporter teleporter, CallbackInfoReturnable<Entity> callbackInfo) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        final ResourceKey<Level> fromDim = fromDimHolder.get();
        final ResourceKey<Level> toDim = level.dimension();
        final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
        final ResourceKey<Level> THE_NETHER = Level.NETHER;
        if ((fromDim != OVERWORLD || toDim != THE_NETHER) && (fromDim != THE_NETHER || toDim != OVERWORLD)) {
            NetherPortalFix.logger.debug("Not storing return portal because it's from {} to {}", fromDim, toDim);
            return;
        }

        BlockPos lastPos = ((LivingEntityAccessor) player).getLastPos();
        if (lastPos == null) {
            NetherPortalFix.logger.debug("Not storing return portal because I just spawned.");
            return;
        }

        BlockUtil.FoundRectangle fromPortal = ReturnPortalManager.findPortalAt(player, fromDim, lastPos);
        BlockPos toPos = player.blockPosition();
        if (fromPortal == null) {
            NetherPortalFix.logger.debug("Not storing return portal because I'm not in a portal.");
        } else {
            ReturnPortalManager.storeReturnPortal(player, toDim, toPos, fromPortal);
            NetherPortalFix.logger.debug("Storing return portal from {} to {} in {}", toDim, fromPortal.minCorner, fromDim);
        }
    }

}
