package net.blay09.mods.netherportalfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;

public class BetterTeleporter extends Teleporter {

    private static final int PORTAL_RANGE_SQR = 9;
    private static final String NBT_RETURN_PORTALS = "ReturnPortals";
    private static final String NBT_FROM_X = "FromX";
    private static final String NBT_FROM_Y = "FromY";
    private static final String NBT_FROM_Z = "FromZ";
    private static final String NBT_FROM_DIM = "FromDim";
    private static final String NBT_TO_X = "ToX";
    private static final String NBT_TO_Y = "ToY";
    private static final String NBT_TO_Z = "ToZ";
    private static final String NBT_TO_DIM = "ToDim";

    private final WorldServer world;

    public BetterTeleporter(WorldServer world) {
        super(world);
        this.world = world;
    }

    @Override
    public void placeInPortal(Entity entity, double oldX, double oldY, double oldZ, float rotationYaw) {
        if (entity instanceof EntityPlayer) {
            PortalPositionAndDimension from = new PortalPositionAndDimension((int) oldX, (int) oldY, (int) oldZ); // lastPortalPos
            super.placeInPortal(entity, oldX, oldY, oldZ, rotationYaw);
            PortalPositionAndDimension to = new PortalPositionAndDimension(((EntityPlayer) entity).getPlayerCoordinates());
            NBTTagCompound tagCompound = entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagList tagList = tagCompound.getTagList(NBT_RETURN_PORTALS, Constants.NBT.TAG_COMPOUND);
            for (int i = tagList.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound portalCompound = tagList.getCompoundTagAt(i);
                int toX = portalCompound.getInteger(NBT_TO_X);
                int toY = portalCompound.getInteger(NBT_TO_Y);
                int toZ = portalCompound.getInteger(NBT_TO_Z);
                PortalPositionAndDimension testTo = new PortalPositionAndDimension(toX, toY, toZ, portalCompound.getInteger(NBT_TO_DIM));
                System.out.println(testTo.getDistanceSquaredToChunkCoordinates(to));
                if (testTo.dimensionId == entity.worldObj.provider.dimensionId && testTo.getDistanceSquaredToChunkCoordinates(to) <= PORTAL_RANGE_SQR) {
                    tagList.removeTag(i);
                }
            }
            NBTTagCompound portalCompound = new NBTTagCompound();
            portalCompound.setInteger(NBT_FROM_X, from.posX);
            portalCompound.setInteger(NBT_FROM_Y, from.posY);
            portalCompound.setInteger(NBT_FROM_Z, from.posZ);
            portalCompound.setInteger(NBT_FROM_DIM, from.dimensionId);
            portalCompound.setInteger(NBT_TO_X, to.posX);
            portalCompound.setInteger(NBT_TO_Y, to.posY);
            portalCompound.setInteger(NBT_TO_Z, to.posZ);
            portalCompound.setInteger(NBT_TO_DIM, to.dimensionId);
            tagList.appendTag(portalCompound);
            tagCompound.setTag(NBT_RETURN_PORTALS, tagList);
            entity.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tagCompound);
        } else {
            super.placeInPortal(entity, oldX, oldY, oldZ, rotationYaw);
        }
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double oldX, double oldY, double oldZ, float rotationYaw) {
        if (entity instanceof EntityPlayer) {
            NBTTagCompound tagCompound = entity.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            NBTTagList tagList = tagCompound.getTagList(NBT_RETURN_PORTALS, Constants.NBT.TAG_COMPOUND);
            for(int i = tagList.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound portalCompound = tagList.getCompoundTagAt(i);
                int toX = portalCompound.getInteger(NBT_TO_X);
                int toY = portalCompound.getInteger(NBT_TO_Y);
                int toZ = portalCompound.getInteger(NBT_TO_Z);
                PortalPositionAndDimension to = new PortalPositionAndDimension(toX, toY, toZ, portalCompound.getInteger(NBT_TO_DIM));
                PortalPosition lastPortalPosition = new PortalPosition((int) oldX, (int) oldY, (int) oldZ, world.getTotalWorldTime());
                if (to.dimensionId == entity.worldObj.provider.dimensionId && to.getDistanceSquaredToChunkCoordinates(lastPortalPosition) <= PORTAL_RANGE_SQR) {
                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posZ);
                    long key = ChunkCoordIntPair.chunkXZ2Int(x, y);
                    PortalPosition oldValue = (PortalPosition) destinationCoordinateCache.getValueByKey(key);
                    int fromX = portalCompound.getInteger(NBT_FROM_X);
                    int fromY = portalCompound.getInteger(NBT_FROM_Y);
                    int fromZ = portalCompound.getInteger(NBT_FROM_Z);
                    PortalPositionAndDimension from = new PortalPositionAndDimension(fromX, fromY, fromZ, portalCompound.getInteger(NBT_FROM_DIM));
                    destinationCoordinateCache.add(key, from);
                    if (!destinationCoordinateKeys.contains(key)) {
                        destinationCoordinateKeys.add(key);
                    }
                    boolean result = super.placeInExistingPortal(entity, oldX, oldY, oldZ, rotationYaw);
                    if (oldValue != null) {
                        destinationCoordinateCache.add(key, oldValue);
                    }
                    tagList.removeTag(i);
                    tagCompound.setTag(NBT_RETURN_PORTALS, tagList);
                    entity.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tagCompound);
                    return result;
                }
            }
        }
        return super.placeInExistingPortal(entity, oldX, oldY, oldZ, rotationYaw);
    }

    public class PortalPositionAndDimension extends PortalPosition {
        public final int dimensionId;

        public PortalPositionAndDimension(ChunkCoordinates pos) {
            this(pos.posX, pos.posY, pos.posZ, world.provider.dimensionId);
        }

        public PortalPositionAndDimension(int x, int y, int z) {
            this(x, y, z, world.provider.dimensionId);
        }

        public PortalPositionAndDimension(int x, int y, int z, int dimensionId) {
            super(x, y, z, world.getWorldTime());
            this.dimensionId = dimensionId;
        }
    }

}
