package net.blay09.mods.netherportalfix;

import com.google.common.collect.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
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
    public void placeInPortal(Entity entity, float rotationYaw) {
        if(entity instanceof EntityPlayer) {
            PortalPositionAndDimension from = new PortalPositionAndDimension(entity.field_181016_an);
            super.placeInPortal(entity, rotationYaw);
            PortalPositionAndDimension to = new PortalPositionAndDimension(entity.getPosition());
            Collection<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> returnPortals = returnPortalMap.get(entity);
            Iterator<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> it = returnPortals.iterator();
            while(it.hasNext()) {
                Pair<PortalPositionAndDimension, PortalPositionAndDimension> returnPortal = it.next();
                if (returnPortal.getRight().dimensionId == entity.getEntityWorld().provider.getDimensionId() && returnPortal.getRight().distanceSq(to) <= PORTAL_RANGE_SQR) { // lastPortalPos
                    it.remove();
                }
            }
            returnPortalMap.put(entity, Pair.of(from, to));
        } else {
            super.placeInPortal(entity, rotationYaw);
        }
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
        if (entity instanceof EntityPlayer) {
            Collection<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> returnPortals = returnPortalMap.get(entity);
            Iterator<Pair<PortalPositionAndDimension, PortalPositionAndDimension>> it = returnPortals.iterator();
            while(it.hasNext()) {
                Pair<PortalPositionAndDimension, PortalPositionAndDimension> returnPortal = it.next();
                if (returnPortal.getRight().dimensionId == entity.getEntityWorld().provider.getDimensionId() && returnPortal.getRight().distanceSq(entity.field_181016_an) <= PORTAL_RANGE_SQR) { // lastPortalPos
                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posZ);
                    long key = ChunkCoordIntPair.chunkXZ2Int(x, y);
                    PortalPosition oldValue = destinationCoordinateCache.getValueByKey(key);
                    destinationCoordinateCache.add(key, returnPortal.getLeft());
                    if(!destinationCoordinateKeys.contains(key)) {
                        destinationCoordinateKeys.add(key);
                    }
                    boolean result = super.placeInExistingPortal(entity, rotationYaw);
                    if(oldValue != null) {
                        destinationCoordinateCache.add(key, oldValue);
                    }
                    it.remove();
                    return result;
                }
            }
        }
        return super.placeInExistingPortal(entity, rotationYaw);
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

        public PortalPositionAndDimension(BlockPos pos) {
            super(pos, world.getWorldTime());
            dimensionId = world.provider.getDimensionId();
        }
    }

}
