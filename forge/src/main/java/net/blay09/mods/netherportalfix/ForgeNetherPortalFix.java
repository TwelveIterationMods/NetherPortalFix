package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(NetherPortalFix.MOD_ID)
public class ForgeNetherPortalFix {

    public ForgeNetherPortalFix() {
        Balm.initialize(NetherPortalFix.MOD_ID, NetherPortalFix::initialize);

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
    }

}
