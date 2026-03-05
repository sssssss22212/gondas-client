package com.gondas.client.event;

import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import java.util.Iterator;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventHandler {
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Iterator var2 = ModuleManager.getModules().iterator();

         while(var2.hasNext()) {
            Module m = (Module)var2.next();
            if (m.isToggled()) {
               m.onTick();
            }
         }

      }
   }
}
