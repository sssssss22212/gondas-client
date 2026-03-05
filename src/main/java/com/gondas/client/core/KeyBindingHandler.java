package com.gondas.client.core;

import com.gondas.client.gui.ClickGUI;
import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import com.gondas.client.module.impl.ClickGUIModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class KeyBindingHandler {
   public static KeyBinding clickGUI = new KeyBinding("key.gondas.clickgui", 344, "key.categories.gondas");

   public static void register() {
      ClientRegistry.registerKeyBinding(clickGUI);
      MinecraftForge.EVENT_BUS.register(new KeyBindingHandler());
   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if (clickGUI.func_151468_f()) {
         Minecraft.func_71410_x().func_147108_a(new ClickGUI());
      }

      if (event.getAction() == 1) {
         int key = event.getKey();
         Module m = ModuleManager.getByKey(key);
         if (m != null && !(m instanceof ClickGUIModule)) {
            m.toggle();
         }
      }

   }
}
