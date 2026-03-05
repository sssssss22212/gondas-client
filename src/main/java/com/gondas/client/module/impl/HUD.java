package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Iterator;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HUD extends Module {
   public HUD() {
      super("HUD", "Display module list", Module.Category.RENDER);
   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if (event.getType() == ElementType.TEXT) {
         MatrixStack matrixStack = new MatrixStack();
         int y = 2;
         Iterator var4 = ModuleManager.getModules().iterator();

         while(var4.hasNext()) {
            Module m = (Module)var4.next();
            if (m.isToggled()) {
               mc.field_71466_p.func_238405_a_(matrixStack, m.getName(), 2.0F, (float)y, -16711936);
               y += 10;
            }
         }

      }
   }
}
