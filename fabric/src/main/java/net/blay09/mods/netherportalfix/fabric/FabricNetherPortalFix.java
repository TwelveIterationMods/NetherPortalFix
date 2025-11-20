package net.blay09.mods.netherportalfix.fabric;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.fabricmc.api.ModInitializer;

public class FabricNetherPortalFix implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initializeMod(NetherPortalFix.MOD_ID, FabricLoadContext.INSTANCE, NetherPortalFix::initialize);
    }
}
