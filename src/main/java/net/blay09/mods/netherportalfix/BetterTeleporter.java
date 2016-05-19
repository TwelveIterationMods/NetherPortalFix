package net.blay09.mods.netherportalfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;

public class BetterTeleporter extends Teleporter {

    private static final int PORTAL_RANGE_SQR = 9;
    private static final String NBT_RETURN_PORTALS = "ReturnPortals";
    private static final String NBT_FROM = "From";
    private static final String NBT_FROM_DIM = "FromDim";
    private static final String NBT_TO = "To";
    private static final String NBT_TO_DIM = "ToDim";

    private final WorldServer world;

    public BetterTeleporter(WorldServer world) {
        super(world);
        this.world = world;
    }

    @Override
    public void placeInPortal(@Nonnull Entity entity, float rotationYaw) {
        if(entity instanceof EntityPlayer) {
            PortalPositionAndDimension from = new PortalPositionAndDimension(entity.lastPortalPos);
            super.placeInPortal(entity, rotationYaw);
            PortalPositionAndDimension to = new PortalPositionAndDimension(entity.getPosition());
            NBTTagCompound tagCompound = entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagList tagList = tagCompound.getTagList(NBT_RETURN_PORTALS, Constants.NBT.TAG_COMPOUND);
            for(int i = tagList.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound portalCompound = tagList.getCompoundTagAt(i);
                PortalPositionAndDimension testTo = new PortalPositionAndDimension(BlockPos.fromLong(portalCompound.getLong(NBT_TO)), portalCompound.getInteger(NBT_TO_DIM));
                if(testTo.dimensionId == entity.worldObj.provider.getDimension() && testTo.distanceSq(to) <= PORTAL_RANGE_SQR) {
                    tagList.removeTag(i);
                }
            }
            NBTTagCompound portalCompound = new NBTTagCompound();
            portalCompound.setLong(NBT_FROM, from.toLong());
            portalCompound.setInteger(NBT_FROM_DIM, from.dimensionId);
            portalCompound.setLong(NBT_TO, to.toLong());
            portalCompound.setInteger(NBT_TO_DIM, to.dimensionId);
            tagList.appendTag(portalCompound);
            tagCompound.setTag(NBT_RETURN_PORTALS, tagList);
            entity.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tagCompound);
        } else {
            super.placeInPortal(entity, rotationYaw);
        }
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
        if (entity instanceof EntityPlayer) {
            NBTTagCompound tagCompound = entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagList tagList = tagCompound.getTagList(NBT_RETURN_PORTALS, Constants.NBT.TAG_COMPOUND);
            for(int i = tagList.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound portalCompound = tagList.getCompoundTagAt(i);
                PortalPositionAndDimension to = new PortalPositionAndDimension(BlockPos.fromLong(portalCompound.getLong(NBT_TO)), portalCompound.getInteger(NBT_TO_DIM));
                if (to.dimensionId == entity.getEntityWorld().provider.getDimension() && to.distanceSq(entity.lastPortalPos) <= PORTAL_RANGE_SQR) {
                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posZ);
                    long key = ChunkPos.chunkXZ2Int(x, y);
                    PortalPosition oldValue = destinationCoordinateCache.get(key);
                    PortalPositionAndDimension from = new PortalPositionAndDimension(BlockPos.fromLong(portalCompound.getLong(NBT_FROM)), portalCompound.getInteger(NBT_FROM_DIM));
                    destinationCoordinateCache.put(key, from);
                    boolean result = super.placeInExistingPortal(entity, rotationYaw);
                    if(oldValue != null) {
                        destinationCoordinateCache.put(key, oldValue);
                    }
                    tagList.removeTag(i);
                    tagCompound.setTag(NBT_RETURN_PORTALS, tagList);
                    entity.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tagCompound);
                    return result;
                }
            }
        }
        return super.placeInExistingPortal(entity, rotationYaw);
    }

    public class PortalPositionAndDimension extends PortalPosition {
        public final int dimensionId;

        public PortalPositionAndDimension(BlockPos pos) {
            this(pos, world.provider.getDimension());
        }

        public PortalPositionAndDimension(BlockPos pos, int dimensionId) {
            super(pos, world.getWorldTime());
            this.dimensionId = dimensionId;
        }
    }

}
