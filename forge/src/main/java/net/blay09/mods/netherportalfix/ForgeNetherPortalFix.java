package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.minecraftforge.fml.common.Mod;

@Mod(NetherPortalFix.MOD_ID)
public class ForgeNetherPortalFix {

    public ForgeNetherPortalFix() {
        Balm.initialize(NetherPortalFix.MOD_ID, NetherPortalFix::initialize);
    }

}
