package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.fabricmc.api.ModInitializer;

public class FabricNetherPortalFix implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initialize(NetherPortalFix.MOD_ID, NetherPortalFix::initialize);
    }
}
