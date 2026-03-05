package com.gondas.client.module.impl;

import com.gondas.client.gui.ClickGUI;
import com.gondas.client.module.Module;

public class ClickGUIModule extends Module {
   public ClickGUIModule() {
      super("ClickGUI", "Open GUI menu", Module.Category.MISC, 0);
   }

   public void onEnable() {
      super.onEnable();
      mc.func_147108_a(new ClickGUI());
      this.toggle();
   }
}
