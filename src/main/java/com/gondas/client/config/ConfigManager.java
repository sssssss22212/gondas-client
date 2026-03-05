package com.gondas.client.config;

import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import com.gondas.client.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;

public class ConfigManager {
   private static final String CONFIG_DIR = "gondas";
   private static String currentConfig = "default";

   public static String getCurrentConfig() {
      return currentConfig;
   }

   public static void saveConfig() {
      saveConfig(currentConfig);
   }

   public static void saveConfig(String name) {
      try {
         Path configPath = Minecraft.func_71410_x().field_71412_D.toPath().resolve("gondas");
         if (!Files.exists(configPath, new LinkOption[0])) {
            Files.createDirectories(configPath);
         }

         File configFile = configPath.resolve(name + ".json").toFile();
         JsonObject root = new JsonObject();
         JsonObject modulesObj = new JsonObject();
         Iterator var5 = ModuleManager.getModules().iterator();

         while(var5.hasNext()) {
            Module m = (Module)var5.next();
            JsonObject moduleObj = new JsonObject();
            moduleObj.addProperty("toggled", m.isToggled());
            moduleObj.addProperty("key", m.getKey());
            JsonObject settingsObj = new JsonObject();
            Iterator var9 = m.getSettings().iterator();

            while(var9.hasNext()) {
               Setting s = (Setting)var9.next();
               if (s instanceof Setting.Boolean) {
                  settingsObj.addProperty(s.getName(), ((Setting.Boolean)s).getValue());
               } else if (s instanceof Setting.Int) {
                  settingsObj.addProperty(s.getName(), ((Setting.Int)s).getValue());
               } else if (s instanceof Setting.Double) {
                  settingsObj.addProperty(s.getName(), ((Setting.Double)s).getValue());
               } else if (s instanceof Setting.Mode) {
                  settingsObj.addProperty(s.getName(), ((Setting.Mode)s).getValue());
               }
            }

            moduleObj.add("settings", settingsObj);
            modulesObj.add(m.getName(), moduleObj);
         }

         root.add("modules", modulesObj);
         Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
         Files.write(configFile.toPath(), gson.toJson(root).getBytes(), new OpenOption[0]);
         currentConfig = name;
         System.out.println("[Gondas] Config saved: " + name);
      } catch (Exception var11) {
         System.err.println("[Gondas] Failed to save config: " + var11.getMessage());
      }

   }

   public static void loadConfig() {
      loadConfig(currentConfig);
   }

   public static void loadConfig(String name) {
      try {
         Path configPath = Minecraft.func_71410_x().field_71412_D.toPath().resolve("gondas");
         File configFile = configPath.resolve(name + ".json").toFile();
         if (!configFile.exists()) {
            System.out.println("[Gondas] Config not found: " + name);
         } else {
            String json = new String(Files.readAllBytes(configFile.toPath()));
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();
            JsonObject modulesObj = root.getAsJsonObject("modules");
            Iterator var7 = modulesObj.entrySet().iterator();

            while(true) {
               Module m;
               JsonObject moduleObj;
               do {
                  Entry entry;
                  do {
                     if (!var7.hasNext()) {
                        currentConfig = name;
                        System.out.println("[Gondas] Config loaded: " + name);
                        return;
                     }

                     entry = (Entry)var7.next();
                     m = ModuleManager.getModuleByName((String)entry.getKey());
                  } while(m == null);

                  moduleObj = ((JsonElement)entry.getValue()).getAsJsonObject();
                  if (moduleObj.has("toggled")) {
                     boolean toggled = moduleObj.get("toggled").getAsBoolean();
                     if (toggled != m.isToggled()) {
                        m.toggle();
                     }
                  }
               } while(!moduleObj.has("settings"));

               JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
               Iterator var12 = settingsObj.entrySet().iterator();

               while(var12.hasNext()) {
                  Entry<String, JsonElement> settingEntry = (Entry)var12.next();
                  Setting s = getSettingByName(m, (String)settingEntry.getKey());
                  if (s != null) {
                     JsonElement val = (JsonElement)settingEntry.getValue();
                     if (s instanceof Setting.Boolean) {
                        ((Setting.Boolean)s).setValue(val.getAsBoolean());
                     } else if (s instanceof Setting.Int) {
                        ((Setting.Int)s).setValue(val.getAsInt());
                     } else if (s instanceof Setting.Double) {
                        ((Setting.Double)s).setValue(val.getAsDouble());
                     } else if (s instanceof Setting.Mode) {
                        ((Setting.Mode)s).setValue(val.getAsString());
                     }
                  }
               }
            }
         }
      } catch (Exception var16) {
         System.err.println("[Gondas] Failed to load config: " + var16.getMessage());
      }
   }

   public static void resetConfig() {
      Iterator var0 = ModuleManager.getModules().iterator();

      while(var0.hasNext()) {
         Module m = (Module)var0.next();
         if (m.isToggled()) {
            m.toggle();
         }
      }

      currentConfig = "default";
      System.out.println("[Gondas] Config reset");
   }

   private static Setting getSettingByName(Module module, String name) {
      Iterator var2 = module.getSettings().iterator();

      Setting s;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         s = (Setting)var2.next();
      } while(!s.getName().equals(name));

      return s;
   }

   public static String[] listConfigs() {
      try {
         Path configPath = Minecraft.func_71410_x().field_71412_D.toPath().resolve("gondas");
         if (!Files.exists(configPath, new LinkOption[0])) {
            return new String[]{"default"};
         } else {
            File[] files = configPath.toFile().listFiles((dir, n) -> {
               return n.endsWith(".json");
            });
            if (files != null && files.length != 0) {
               String[] names = new String[files.length];

               for(int i = 0; i < files.length; ++i) {
                  names[i] = files[i].getName().replace(".json", "");
               }

               return names;
            } else {
               return new String[]{"default"};
            }
         }
      } catch (Exception var4) {
         return new String[]{"default"};
      }
   }
}
