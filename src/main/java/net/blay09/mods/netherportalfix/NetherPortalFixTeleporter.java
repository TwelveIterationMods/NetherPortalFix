package net.blay09.mods.netherportalfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

/**
 * Code mostly taken from FTB-Library by LatvianModder.
 */
public class NetherPortalFixTeleporter implements ITeleporter {

    private final BlockPos pos;
    private final DimensionType dimension;

    public NetherPortalFixTeleporter(BlockPos pos, DimensionType dimension) {
        this.pos = pos;
        this.dimension = dimension;
    }

    @Override
    public void placeEntity(World world, Entity entity, float yaw) {
        entity.setMotion(0f, 0f, 0f);
        entity.fallDistance = 0f;

        if (entity instanceof ServerPlayerEntity && ((ServerPlayerEntity) entity).connection != null)
        {
            ((ServerPlayerEntity) entity).connection.setPlayerLocation(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f, yaw, entity.rotationPitch);
        }
        else
        {
            entity.setLocationAndAngles(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f, yaw, entity.rotationPitch);
        }
    }

    public void teleport(Entity entity) {
        if (dimension != entity.dimension) {
            entity.changeDimension(dimension, this);
            return;
        }

        placeEntity(entity.world, entity, entity.rotationYaw);
    }
}
