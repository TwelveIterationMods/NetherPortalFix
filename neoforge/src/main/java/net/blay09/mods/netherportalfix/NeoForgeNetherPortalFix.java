package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(NetherPortalFix.MOD_ID)
public class NeoForgeNetherPortalFix {

    public NeoForgeNetherPortalFix() {
        Balm.initialize(NetherPortalFix.MOD_ID, NetherPortalFix::initialize);

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
    }

}
