package net.blay09.mods.netherportalfix;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "netherportalfix", name = "Nether Portal Fix", acceptableRemoteVersions = "*")
public class NetherPortalFix {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if(event.world instanceof WorldServer) {
            if(event.world.provider.getDimension() == 0 || event.world.provider.getDimension() == -1) {
                ((WorldServer) event.world).worldTeleporter = new BetterTeleporter((WorldServer) event.world);
            }
        }
    }

}
