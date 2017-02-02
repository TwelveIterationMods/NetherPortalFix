package net.blay09.mods.netherportalfix;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "netherportalfix", name = "NetherPortalFix", acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.11]")
public class NetherPortalFix {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if(event.getWorld() instanceof WorldServer) {
            if(event.getWorld().provider.getDimension() == 0 || event.getWorld().provider.getDimension() == -1) {
                ((WorldServer) event.getWorld()).worldTeleporter = new BetterTeleporter((WorldServer) event.getWorld());
            }
        }
    }

}
