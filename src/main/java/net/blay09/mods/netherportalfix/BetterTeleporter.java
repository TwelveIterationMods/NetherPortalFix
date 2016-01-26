package net.blay09.mods.netherportalfix;

import com.google.common.collect.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Iterator;

public class BetterTeleporter extends Teleporter {

    public static final int PORTAL_RANGE_SQR = 9;

    private final WorldServer world;
    private static Multimap<Entity, Pair<PortalPositionAndDimension, PortalPositionAndDimension>> returnPortalMap = ArrayListMultimap.create();

    public BetterTeleporter(WorldServer world) {
        super(world);
        this.world = world;
    }

    @Override
    public void placeInPortal(Entity entity, double oldX, double oldY, double oldZ, float rotationYaw) {
        if(entity instanceof EntityPlayer) {
            PortalPositionAndDimension from = new PortalPositionAndDimension((int) oldX, (int) oldY, (int) oldZ); // lastPortalPos
            super.placeInPortal(entity, oldX, oldY, oldZ, rotationYaw);
            PortalPositionAndDimension to = new PortalPositionAndDimension(((EntityPlayer) entity).getPlayerCoordinates());
            Collection<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> returnPortals = returnPortalMap.get(entity);
            Iterator<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> it = returnPortals.iterator();
            while(it.hasNext()) {
                Pair<PortalPositionAndDimension, PortalPositionAndDimension> returnPortal = it.next();
                if (returnPortal.getRight().dimensionId == entity.worldObj.provider.dimensionId && returnPortal.getRight().getDistanceSquaredToChunkCoordinates(to) <= PORTAL_RANGE_SQR) { // lastPortalPos
                    it.remove();
                }
            }
            returnPortalMap.put(entity, Pair.of(from, to));
            System.out.println("ReturnMap: " + returnPortals.size());
        } else {
            super.placeInPortal(entity, oldX, oldY, oldZ, rotationYaw);
        }
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double oldX, double oldY, double oldZ, float rotationYaw) {
        if (entity instanceof EntityPlayer) {
            Collection<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> returnPortals = returnPortalMap.get(entity);
            Iterator<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> it = returnPortals.iterator();
            PortalPosition lastPortalPosition = new PortalPosition((int) oldX, (int) oldY, (int) oldZ, world.getTotalWorldTime());
            while(it.hasNext()) {
                Pair<PortalPositionAndDimension, PortalPositionAndDimension> returnPortal = it.next();
                if (returnPortal.getRight().dimensionId == entity.worldObj.provider.dimensionId && returnPortal.getRight().getDistanceSquaredToChunkCoordinates(lastPortalPosition) <= PORTAL_RANGE_SQR) { // lastPortalPos
                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posZ);
                    long key = ChunkCoordIntPair.chunkXZ2Int(x, y);
                    PortalPosition oldValue = (PortalPosition) destinationCoordinateCache.getValueByKey(key);
                    destinationCoordinateCache.add(key, returnPortal.getLeft());
                    if(!destinationCoordinateKeys.contains(key)) {
                        destinationCoordinateKeys.add(key);
                    }
                    boolean result = super.placeInExistingPortal(entity, oldX, oldY, oldZ, rotationYaw);
                    if(oldValue != null) {
                        destinationCoordinateCache.add(key, oldValue);
                    }
                    it.remove();
                    return result;
                }
            }
        }
        return super.placeInExistingPortal(entity, oldX, oldY, oldZ, rotationYaw);
    }

    @Override
    public void removeStalePortalLocations(long worldTime) {
        super.removeStalePortalLocations(worldTime);

        Iterator<Entity> it = returnPortalMap.keySet().iterator();
        while(it.hasNext()) {
            if(it.next().isDead) {
                it.remove();
            }
        }
    }

    public class PortalPositionAndDimension extends PortalPosition {
        public final int dimensionId;

        public PortalPositionAndDimension(ChunkCoordinates pos) {
            super(pos.posX, pos.posY, pos.posZ, world.getWorldTime());
            dimensionId = world.provider.dimensionId;
        }

        public PortalPositionAndDimension(int x, int y, int z) {
            super(x, y, z, world.getWorldTime());
            this.dimensionId = world.provider.dimensionId;
        }
    }

}
