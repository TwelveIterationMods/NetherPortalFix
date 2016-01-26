package net.blay09.mods.netherportalfix;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = "netherportalfix", name = "Nether Portal Fix", acceptableRemoteVersions = "*")
public class NetherPortalFix {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if(event.world instanceof WorldServer) {
            if(event.world.provider.dimensionId == 0 || event.world.provider.dimensionId == -1) {
                ((WorldServer) event.world).worldTeleporter = new BetterTeleporter((WorldServer) event.world);
            }
        }
    }

}
