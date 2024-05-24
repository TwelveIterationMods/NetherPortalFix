package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NetherPortalFix.MOD_ID)
public class NeoForgeNetherPortalFix {

    public NeoForgeNetherPortalFix(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        Balm.initialize(NetherPortalFix.MOD_ID, context, NetherPortalFix::initialize);
    }

}
