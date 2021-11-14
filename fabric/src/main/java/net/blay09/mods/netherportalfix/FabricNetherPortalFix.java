package net.blay09.mods.netherportalfix;

import net.fabricmc.api.ModInitializer;

public class FabricNetherPortalFix implements ModInitializer {
    @Override
    public void onInitialize() {
        NetherPortalFix.initialize();
    }
}
