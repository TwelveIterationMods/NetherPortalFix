package net.blay09.mods.netherportalfix.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.fabricmc.api.ModInitializer;

public class FabricNetherPortalFix implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initialize(NetherPortalFix.MOD_ID, EmptyLoadContext.INSTANCE, NetherPortalFix::initialize);
    }
}
