package net.blay09.mods.netherportalfix.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.blay09.mods.netherportalfix.ReturnPortalManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @ModifyExpressionValue(method = "getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/DimensionTransition;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/portal/PortalForcer;findClosestPortalPosition(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Ljava/util/Optional;"))
    public Optional<BlockPos> getExitPortal(Optional<BlockPos> original, ServerLevel level, Entity entity) {
        final var fromPos = entity.blockPosition();
        final ResourceKey<Level> fromDim = entity.level().dimension();
        final var returnPortal = ReturnPortalManager.findReturnPortal(entity, fromDim, fromPos);
        if (returnPortal == null) {
            NetherPortalFix.logger.info("No return portal found");
            return original;
        }

        if (!level.getBlockState(returnPortal.getPos()).is(Blocks.NETHER_PORTAL)) {
            NetherPortalFix.logger.info("Return portal is no longer valid");
            return original;
        }

        NetherPortalFix.logger.debug("Return portal found, redirecting! :)");
        return Optional.of(returnPortal.getPos());
    }

}
