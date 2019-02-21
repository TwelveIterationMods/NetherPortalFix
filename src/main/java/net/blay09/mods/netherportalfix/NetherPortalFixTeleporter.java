package net.blay09.mods.netherportalfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.util.ITeleporter;

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
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        entity.fallDistance = 0f;

        if (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection != null)
        {
            ((EntityPlayerMP) entity).connection.setPlayerLocation(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f, yaw, entity.rotationPitch);
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
