package net.blay09.mods.netherportalfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;

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
    public void placeInPortal(Entity entity, float rotationYaw) {
        if(entity instanceof EntityPlayer) {
            PortalPositionAndDimension from = new PortalPositionAndDimension(entity.field_181016_an);
            super.placeInPortal(entity, rotationYaw);
            PortalPositionAndDimension to = new PortalPositionAndDimension(entity.getPosition());
            NBTTagCompound tagCompound = entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagList tagList = tagCompound.getTagList(NBT_RETURN_PORTALS, Constants.NBT.TAG_COMPOUND);
            for(int i = tagList.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound portalCompound = tagList.getCompoundTagAt(i);
                PortalPositionAndDimension testTo = new PortalPositionAndDimension(BlockPos.fromLong(portalCompound.getLong(NBT_TO)), portalCompound.getInteger(NBT_TO_DIM));
                if(testTo.dimensionId == entity.worldObj.provider.getDimensionId() && testTo.distanceSq(to) <= PORTAL_RANGE_SQR) {
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
                if (to.dimensionId == entity.getEntityWorld().provider.getDimensionId() && to.distanceSq(entity.field_181016_an) <= PORTAL_RANGE_SQR) { // lastPortalPos
                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posZ);
                    long key = ChunkCoordIntPair.chunkXZ2Int(x, y);
                    PortalPosition oldValue = destinationCoordinateCache.getValueByKey(key);
                    PortalPositionAndDimension from = new PortalPositionAndDimension(BlockPos.fromLong(portalCompound.getLong(NBT_FROM)), portalCompound.getInteger(NBT_FROM_DIM));
                    destinationCoordinateCache.add(key, from);
                    if(!destinationCoordinateKeys.contains(key)) {
                        destinationCoordinateKeys.add(key);
                    }
                    boolean result = super.placeInExistingPortal(entity, rotationYaw);
                    if(oldValue != null) {
                        destinationCoordinateCache.add(key, oldValue);
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
            this(pos, world.provider.getDimensionId());
        }

        public PortalPositionAndDimension(BlockPos pos, int dimensionId) {
            super(pos, world.getWorldTime());
            this.dimensionId = dimensionId;
        }
    }

}
