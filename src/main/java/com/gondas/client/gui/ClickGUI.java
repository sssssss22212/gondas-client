package com.gondas.client.gui;

import com.gondas.client.config.ConfigManager;
import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import com.gondas.client.setting.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Screen {
   private static int selectedCategory = 0;
   private int moduleScrollOffset = 0;
   private Module selectedModule = null;
   private int settingScrollOffset = 0;
   private long openTime = System.currentTimeMillis();
   private Setting draggingSetting = null;
   private float animation = 0.0F;

   public ClickGUI() {
      super(new StringTextComponent("Gondas Client"));
   }

   public void func_230430_a_(MatrixStack ms, int mx, int my, float pt) {
      long elapsed = System.currentTimeMillis() - this.openTime;
      float target = Math.min(1.0F, (float)elapsed / 150.0F);
      this.animation += (target - this.animation) * 0.2F;
      int bgAlpha = (int)(180.0F * this.animation);
      func_238467_a_(ms, 0, 0, this.field_230708_k_, this.field_230709_l_, bgAlpha << 24 | 657930);
      int panelW = 600;
      int panelH = 350;
      float easedX = (float)this.field_230708_k_ / 2.0F - (float)panelW / 2.0F * this.animation;
      float easedY = (float)this.field_230709_l_ / 2.0F - (float)panelH / 2.0F * this.animation;
      int panelX = (int)easedX;
      int panelY = (int)easedY;
      func_238467_a_(ms, panelX + 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 4, 805306368);
      func_238467_a_(ms, panelX, panelY, panelX + panelW, panelY + panelH, -267053803);
      float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
      int borderColor = Color.HSBtoRGB(hue, 0.6F, 0.8F);
      this.drawBorder(ms, panelX, panelY, panelW, panelH, borderColor, 2);
      String title = "GONDAS CLIENT";
      int titleColor = Color.HSBtoRGB((float)(System.currentTimeMillis() % 2500L) / 2500.0F, 0.9F, 1.0F);
      int titleX = panelX + panelW / 2 - this.field_230712_o_.func_78256_a(title) / 2;
      this.field_230712_o_.func_238405_a_(ms, title, (float)titleX, (float)(panelY + 8), titleColor);
      this.field_230712_o_.func_238405_a_(ms, "v2.0", (float)(panelX + panelW - 35), (float)(panelY + 8), -16733696);
      int catY = panelY + 28;
      int catIndex = 0;
      String[] catNames = new String[]{"COMBAT", "MOVE", "PLAYER", "RENDER", "MISC", "WORLD"};
      Module.Category[] var23 = Module.Category.values();
      int modX = var23.length;

      int modY;
      int catBtnX;
      for(modY = 0; modY < modX; ++modY) {
         Module.Category var10000 = var23[modY];
         catBtnX = panelX + 8 + catIndex * 70;
         boolean selected = catIndex == selectedCategory;
         boolean hover = mx >= catBtnX && mx <= catBtnX + 65 && my >= catY && my <= catY + 22;
         int btnColor = selected ? -16729344 : (hover ? -13290187 : -14671840);
         func_238467_a_(ms, catBtnX, catY, catBtnX + 65, catY + 22, btnColor);
         if (selected) {
            this.drawBorder(ms, catBtnX, catY, 65, 22, -16711936, 1);
         }

         this.field_230712_o_.func_238405_a_(ms, catNames[catIndex], (float)(catBtnX + 5), (float)(catY + 6), selected ? -16777216 : -3355444);
         ++catIndex;
      }

      int cfgY = panelY + panelH - 25;
      this.renderConfigButtons(ms, mx, my, panelX + 8, cfgY);
      modX = panelX + 8;
      modY = panelY + 58;
      int modW = 200;
      catBtnX = panelH - 95;
      this.renderModulesPanel(ms, mx, my, modX, modY, modW, catBtnX);
      int setX = panelX + 215;
      int setW = panelW - 225;
      this.renderSettingsPanel(ms, mx, my, setX, modY, setW, catBtnX);
      super.func_230430_a_(ms, mx, my, pt);
   }

   private void renderConfigButtons(MatrixStack ms, int mx, int my, int x, int y) {
      String[] buttons = new String[]{"Save", "Load", "Reset"};
      int[] colors = new int[]{-16733696, -16750900, -5636096};

      for(int i = 0; i < buttons.length; ++i) {
         int btnX = x + i * 60;
         boolean hover = mx >= btnX && mx <= btnX + 55 && my >= y && my <= y + 18;
         func_238467_a_(ms, btnX, y, btnX + 55, y + 18, hover ? -12566464 : colors[i]);
         this.field_230712_o_.func_238405_a_(ms, buttons[i], (float)(btnX + 10), (float)(y + 5), -1);
      }

      String cfgName = "Config: " + ConfigManager.getCurrentConfig();
      this.field_230712_o_.func_238405_a_(ms, cfgName, (float)(x + 190), (float)(y + 5), -7829368);
   }

   private void renderModulesPanel(MatrixStack ms, int mx, int my, int x, int y, int w, int h) {
      func_238467_a_(ms, x, y, x + w, y + h, Integer.MIN_VALUE);
      this.drawBorder(ms, x, y, w, h, -14342875, 1);
      this.field_230712_o_.func_238405_a_(ms, "MODULES", (float)(x + 6), (float)(y + 4), -16720640);
      List<Module> modules = ModuleManager.getByCategory(Module.Category.values()[selectedCategory]);
      int moduleY = y + 18 - this.moduleScrollOffset;
      Iterator var10 = modules.iterator();

      while(true) {
         while(var10.hasNext()) {
            Module m = (Module)var10.next();
            if (moduleY >= y - 14 && moduleY <= y + h - 14) {
               boolean toggled = m.isToggled();
               boolean hover = mx >= x + 3 && mx <= x + w - 3 && my >= moduleY && my <= moduleY + 16;
               int modColor = toggled ? -16738048 : (hover ? -13290187 : -15066598);
               func_238467_a_(ms, x + 3, moduleY, x + w - 3, moduleY + 16, modColor);
               if (toggled) {
                  this.drawBorder(ms, x + 3, moduleY, w - 6, 16, -16711936, 1);
               }

               this.field_230712_o_.func_238405_a_(ms, m.getName(), (float)(x + 8), (float)(moduleY + 4), toggled ? -1 : -6710887);
               if (m.getKey() != 0) {
                  String key = GLFW.glfwGetKeyName(m.getKey(), 0);
                  if (key != null) {
                     String keyText = "[" + key.toUpperCase() + "]";
                     this.field_230712_o_.func_238405_a_(ms, keyText, (float)(x + w - 10 - this.field_230712_o_.func_78256_a(keyText)), (float)(moduleY + 4), -10066330);
                  }
               }

               if (!m.getSettings().isEmpty()) {
                  this.field_230712_o_.func_238405_a_(ms, ">", (float)(x + w - 8), (float)(moduleY + 4), this.selectedModule == m ? -16711936 : -11184811);
               }

               moduleY += 18;
            } else {
               moduleY += 18;
            }
         }

         return;
      }
   }

   private void renderSettingsPanel(MatrixStack ms, int mx, int my, int x, int y, int w, int h) {
      func_238467_a_(ms, x, y, x + w, y + h, Integer.MIN_VALUE);
      this.drawBorder(ms, x, y, w, h, -14342875, 1);
      this.field_230712_o_.func_238405_a_(ms, "SETTINGS", (float)(x + 6), (float)(y + 4), -16720640);
      if (this.selectedModule != null && !this.selectedModule.getSettings().isEmpty()) {
         this.field_230712_o_.func_238405_a_(ms, "for " + this.selectedModule.getName(), (float)(x + 65), (float)(y + 4), -10066330);
         int sY = y + 20 - this.settingScrollOffset;
         Iterator var9 = this.selectedModule.getSettings().iterator();

         while(true) {
            while(var9.hasNext()) {
               Setting s = (Setting)var9.next();
               if (sY >= y - 20 && sY <= y + h - 20) {
                  this.field_230712_o_.func_238405_a_(ms, s.getName(), (float)(x + 6), (float)sY, -2236963);
                  boolean hover;
                  if (s instanceof Setting.Boolean) {
                     Setting.Boolean bool = (Setting.Boolean)s;
                     hover = mx >= x + w - 45 && mx <= x + w - 6 && my >= sY - 1 && my <= sY + 12;
                     String text = bool.getValue() ? "ON" : "OFF";
                     int textColor = bool.getValue() ? -16711936 : -48060;
                     func_238467_a_(ms, x + w - 48, sY - 1, x + w - 6, sY + 12, hover ? -13290187 : -14671840);
                     this.field_230712_o_.func_238405_a_(ms, text, (float)(x + w - 42), (float)sY, textColor);
                  } else if (s instanceof Setting.Int) {
                     this.renderSlider(ms, x, w, sY, (Setting.Int)s, mx, my);
                  } else if (s instanceof Setting.Double) {
                     this.renderSlider(ms, x, w, sY, (Setting.Double)s, mx, my);
                  } else if (s instanceof Setting.Mode) {
                     Setting.Mode mode = (Setting.Mode)s;
                     hover = mx >= x + w - this.field_230712_o_.func_78256_a(mode.getValue()) - 18 && mx <= x + w - 6 && my >= sY - 1 && my <= sY + 12;
                     func_238467_a_(ms, x + w - this.field_230712_o_.func_78256_a(mode.getValue()) - 15, sY - 1, x + w - 6, sY + 12, hover ? -13290187 : -14671840);
                     this.field_230712_o_.func_238405_a_(ms, mode.getValue(), (float)(x + w - this.field_230712_o_.func_78256_a(mode.getValue()) - 10), (float)sY, -16720640);
                     this.field_230712_o_.func_238405_a_(ms, "<", (float)(x + w - this.field_230712_o_.func_78256_a(mode.getValue()) - 25), (float)sY, -10066330);
                     this.field_230712_o_.func_238405_a_(ms, ">", (float)(x + w - 8), (float)sY, -10066330);
                  }

                  sY += 24;
               } else {
                  sY += 24;
               }
            }

            return;
         }
      } else {
         this.field_230712_o_.func_238405_a_(ms, "Select a module", (float)(x + 15), (float)(y + 40), -11184811);
      }
   }

   private void renderSlider(MatrixStack ms, int panelX, int panelW, int y, Setting setting, int mx, int my) {
      double value;
      double min;
      double max;
      String valueStr;
      if (setting instanceof Setting.Int) {
         Setting.Int intSet = (Setting.Int)setting;
         value = (double)intSet.getValue();
         min = (double)intSet.getMin();
         max = (double)intSet.getMax();
         valueStr = String.valueOf((int)value);
      } else {
         Setting.Double doubleSet = (Setting.Double)setting;
         value = doubleSet.getValue();
         min = doubleSet.getMin();
         max = doubleSet.getMax();
         valueStr = String.format("%.1f", value);
      }

      int sliderX = panelX + 65;
      int sliderW = panelW - 130;
      int sliderH = 6;
      int sliderY = y + 4;
      func_238467_a_(ms, sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, -13290187);
      double percent = (value - min) / (max - min);
      int filledW = (int)((double)sliderW * percent);
      func_238467_a_(ms, sliderX, sliderY, sliderX + filledW, sliderY + sliderH, -16729344);
      int handleX = sliderX + filledW - 2;
      func_238467_a_(ms, handleX, sliderY - 1, handleX + 4, sliderY + sliderH + 1, -1);
      this.field_230712_o_.func_238405_a_(ms, valueStr, (float)(panelX + panelW - 38), (float)y, -16720640);
      if (mx >= sliderX && mx <= sliderX + sliderW && my >= sliderY && my <= sliderY + sliderH) {
         func_238467_a_(ms, sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, 822083583);
      }

   }

   public boolean func_231044_a_(double mx, double my, int btn) {
      int panelX = this.field_230708_k_ / 2 - 300;
      int panelY = this.field_230709_l_ / 2 - 175;
      int cfgY = panelY + 350 - 25;
      String[] actions = new String[]{"Save", "Load", "Reset"};

      int i;
      int modX;
      for(i = 0; i < actions.length; ++i) {
         modX = panelX + 8 + i * 60;
         if (mx >= (double)modX && mx <= (double)(modX + 55) && my >= (double)cfgY && my <= (double)(cfgY + 18)) {
            if (i == 0) {
               ConfigManager.saveConfig();
            } else if (i == 1) {
               ConfigManager.loadConfig();
            } else {
               ConfigManager.resetConfig();
            }

            return true;
         }
      }

      i = panelY + 28;

      int modY;
      for(modX = 0; modX < 6; ++modX) {
         modY = panelX + 8 + modX * 70;
         if (mx >= (double)modY && mx <= (double)(modY + 65) && my >= (double)i && my <= (double)(i + 22)) {
            selectedCategory = modX;
            this.moduleScrollOffset = 0;
            this.selectedModule = null;
            return true;
         }
      }

      modX = panelX + 8;
      modY = panelY + 58;
      int modW = 200;
      int modH = 255;
      List<Module> modules = ModuleManager.getByCategory(Module.Category.values()[selectedCategory]);
      int moduleY = modY + 18 - this.moduleScrollOffset;

      for(Iterator var17 = modules.iterator(); var17.hasNext(); moduleY += 18) {
         Module m = (Module)var17.next();
         if (moduleY >= modY && moduleY <= modY + modH - 14 && mx >= (double)(modX + 3) && mx <= (double)(modX + modW - 3) && my >= (double)moduleY && my <= (double)(moduleY + 16)) {
            if (btn == 0) {
               m.toggle();
            } else if (btn == 1) {
               this.selectedModule = m;
               this.settingScrollOffset = 0;
            }

            return true;
         }
      }

      int setX = panelX + 215;
      int setW = 385;
      if (this.selectedModule != null) {
         int sY = modY + 20 - this.settingScrollOffset;

         for(Iterator var20 = this.selectedModule.getSettings().iterator(); var20.hasNext(); sY += 24) {
            Setting s = (Setting)var20.next();
            if (mx >= (double)(setX + 3) && mx <= (double)(setX + setW - 3) && my >= (double)(sY - 1) && my <= (double)(sY + 18)) {
               if (s instanceof Setting.Boolean) {
                  ((Setting.Boolean)s).setValue(!((Setting.Boolean)s).getValue());
               } else if (s instanceof Setting.Mode) {
                  Setting.Mode mode = (Setting.Mode)s;
                  List<String> modes = mode.getModes();
                  int idx = modes.indexOf(mode.getValue());
                  mode.setValue((String)modes.get((idx + 1) % modes.size()));
               } else if (s instanceof Setting.Int || s instanceof Setting.Double) {
                  this.draggingSetting = s;
                  this.updateSliderValue(s, mx, setX + 65, setW - 130);
               }

               return true;
            }
         }
      }

      return super.func_231044_a_(mx, my, btn);
   }

   public boolean func_231045_a_(double mx, double my, int btn, double dx, double dy) {
      if (this.draggingSetting != null) {
         int panelX = this.field_230708_k_ / 2 - 300;
         this.updateSliderValue(this.draggingSetting, mx, panelX + 280, 255);
         return true;
      } else {
         return super.func_231045_a_(mx, my, btn, dx, dy);
      }
   }

   public boolean func_231048_c_(double mx, double my, int btn) {
      this.draggingSetting = null;
      return super.func_231048_c_(mx, my, btn);
   }

   private void updateSliderValue(Setting setting, double mx, int sliderX, int sliderW) {
      double percent = (mx - (double)sliderX) / (double)sliderW;
      percent = Math.max(0.0D, Math.min(1.0D, percent));
      double min;
      double max;
      if (setting instanceof Setting.Int) {
         Setting.Int intSet = (Setting.Int)setting;
         min = (double)intSet.getMin();
         max = (double)intSet.getMax();
         int value = (int)Math.round(min + percent * (max - min));
         intSet.setValue(value);
      } else if (setting instanceof Setting.Double) {
         Setting.Double doubleSet = (Setting.Double)setting;
         min = doubleSet.getMin();
         max = doubleSet.getMax();
         double value = min + percent * (max - min);
         doubleSet.setValue(value);
      }

   }

   public boolean func_231043_a_(double mx, double my, double delta) {
      int panelX = this.field_230708_k_ / 2 - 300;
      int panelY = this.field_230709_l_ / 2 - 175;
      int modX = panelX + 8;
      int modY = panelY + 58;
      int modW = 200;
      int modH = 255;
      if (mx >= (double)modX && mx <= (double)(modX + modW) && my >= (double)modY && my <= (double)(modY + modH)) {
         this.moduleScrollOffset = Math.max(0, this.moduleScrollOffset - (int)(delta * 12.0D));
      }

      int setX = panelX + 215;
      int setW = 385;
      if (mx >= (double)setX && mx <= (double)(setX + setW) && my >= (double)modY && my <= (double)(modY + modH)) {
         this.settingScrollOffset = Math.max(0, this.settingScrollOffset - (int)(delta * 12.0D));
      }

      return super.func_231043_a_(mx, my, delta);
   }

   public boolean func_231046_a_(int key, int scan, int mods) {
      if (key != 344 && key != 256) {
         return super.func_231046_a_(key, scan, mods);
      } else {
         this.field_230706_i_.func_147108_a((Screen)null);
         return true;
      }
   }

   private void drawBorder(MatrixStack ms, int x, int y, int w, int h, int color, int lineWidth) {
      func_238467_a_(ms, x, y, x + w, y + lineWidth, color);
      func_238467_a_(ms, x, y + h - lineWidth, x + w, y + h, color);
      func_238467_a_(ms, x, y, x + lineWidth, y + h, color);
      func_238467_a_(ms, x + w - lineWidth, y, x + w, y + h, color);
   }

   public boolean func_231177_au__() {
      return false;
   }
}
