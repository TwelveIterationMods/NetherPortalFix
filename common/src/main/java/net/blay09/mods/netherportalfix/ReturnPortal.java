package net.blay09.mods.netherportalfix;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public class ReturnPortal {
    private final UUID uid;
    private final BlockPos pos;

    public ReturnPortal(UUID uid, BlockPos pos) {
        this.uid = uid;
        this.pos = pos;
    }

    public UUID getUid() {
        return uid;
    }

    public BlockPos getPos() {
        return pos;
    }
}
