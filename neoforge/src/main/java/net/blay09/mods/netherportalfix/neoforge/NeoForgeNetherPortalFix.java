package net.blay09.mods.netherportalfix.neoforge;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(NetherPortalFix.MOD_ID)
public class NeoForgeNetherPortalFix {

    public NeoForgeNetherPortalFix(ModContainer modContainer, IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modContainer, modEventBus);
        Balm.initializeMod(NetherPortalFix.MOD_ID, context, NetherPortalFix::initialize);
    }

}
