package com.gondas.client.module;

import com.gondas.client.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public abstract class Module {
   protected static final Minecraft mc = Minecraft.func_71410_x();
   private final String name;
   private final String description;
   private final Module.Category category;
   private final int key;
   private boolean toggled;
   private final List<Setting> settings;

   public Module(String name, String description, Module.Category category, int key) {
      this.settings = new ArrayList();
      this.name = name;
      this.description = description;
      this.category = category;
      this.key = key;
   }

   public Module(String name, String description, Module.Category category) {
      this(name, description, category, 0);
   }

   public Module(String name, Module.Category category) {
      this(name, "", category, 0);
   }

   protected void addSettings(Setting... settingArray) {
      Setting[] var2 = settingArray;
      int var3 = settingArray.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Setting s = var2[var4];
         this.settings.add(s);
      }

   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public void toggle() {
      this.toggled = !this.toggled;
      if (this.toggled) {
         this.onEnable();
      } else {
         this.onDisable();
      }

   }

   public void onEnable() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onDisable() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   public void onTick() {
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public int getKey() {
      return this.key;
   }

   public boolean isToggled() {
      return this.toggled;
   }

   public void setToggled(boolean toggled) {
      this.toggled = toggled;
   }

   public static enum Category {
      COMBAT("Combat"),
      MOVEMENT("Movement"),
      RENDER("Render"),
      PLAYER("Player"),
      WORLD("World"),
      MISC("Misc");

      public final String name;

      private Category(String name) {
         this.name = name;
      }
   }
}
