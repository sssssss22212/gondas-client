package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.module.ModuleManager;
import com.gondas.client.setting.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MobileButtons - виртуальные кнопки на экране для телефонов
 * Позволяет управлять модулями через касания экрана
 */
public class MobileButtons extends Module {
   // Основные настройки
   private Setting.Boolean enabled = new Setting.Boolean("Enabled", true);
   private Setting.Double buttonSize = new Setting.Double("ButtonSize", 40.0D, 20.0D, 80.0D);
   private Setting.Double spacing = new Setting.Double("Spacing", 5.0D, 0.0D, 20.0D);
   private Setting.Int columns = new Setting.Int("Columns", 3, 1, 6);
   private Setting.Mode position = new Setting.Mode("Position", new String[]{"BottomLeft", "BottomRight", "TopLeft", "TopRight", "Center", "Custom"});
   private Setting.Double customX = new Setting.Double("CustomX", 10.0D, 0.0D, 1920.0D);
   private Setting.Double customY = new Setting.Double("CustomY", 200.0D, 0.0D, 1080.0D);
   
   // Внешний вид
   private Setting.Double opacity = new Setting.Double("Opacity", 0.7D, 0.1D, 1.0D);
   private Setting.Boolean showNames = new Setting.Boolean("ShowNames", true);
   private Setting.Boolean showKeybinds = new Setting.Boolean("ShowKeybinds", false);
   private Setting.Boolean roundedButtons = new Setting.Boolean("RoundedButtons", true);
   private Setting.Boolean pulseAnimation = new Setting.Boolean("PulseAnimation", true);
   private Setting.Boolean showCategory = new Setting.Boolean("ShowCategory", false);
   
   // Кнопки быстрого доступа
   private Setting.Boolean quickToggle = new Setting.Boolean("QuickToggle", true);
   private Setting.Boolean holdToActivate = new Setting.Boolean("HoldToActivate", false);
   private Setting.Int holdDelay = new Setting.Int("HoldDelay", 500, 100, 2000);
   
   // Категории кнопок
   private Setting.Boolean combatButtons = new Setting.Boolean("CombatButtons", true);
   private Setting.Boolean movementButtons = new Setting.Boolean("MovementButtons", true);
   private Setting.Boolean renderButtons = new Setting.Boolean("RenderButtons", true);
   private Setting.Boolean playerButtons = new Setting.Boolean("PlayerButtons", true);
   private Setting.Boolean worldButtons = new Setting.Boolean("WorldButtons", true);
   private Setting.Boolean miscButtons = new Setting.Boolean("MiscButtons", true);
   
   // Специальные кнопки
   private Setting.Boolean showPanicButton = new Setting.Boolean("PanicButton", true);
   private Setting.Boolean showGUIButton = new Setting.Boolean("GUIButton", true);
   private Setting.Boolean showProfileButton = new Setting.Boolean("ProfileButton", false);
   
   // Внутренние классы
   private static class MobileButton {
      String name;
      String displayName;
      Module module;
      float x, y, width, height;
      boolean isHovered;
      boolean isPressed;
      long pressTime;
      int color;
      String category;
      
      public MobileButton(String name, Module module, String category) {
         this.name = name;
         this.module = module;
         this.category = category;
         this.displayName = name;
         this.width = 40;
         this.height = 40;
      }
   }
   
   // Состояние
   private List<MobileButton> buttons = new ArrayList<>();
   private Map<String, MobileButton> buttonMap = new HashMap<>();
   private int tickCounter = 0;
   private float animationTime = 0;
   private boolean initialized = false;
   
   // Категории по цветам
   private static final int COLOR_COMBAT = 0xFFFF4444;      // Красный
   private static final int COLOR_MOVEMENT = 0xFF44FF44;    // Зелёный
   private static final int COLOR_RENDER = 0xFF4444FF;      // Синий
   private static final int COLOR_PLAYER = 0xFFFFFF44;      // Жёлтый
   private static final int COLOR_WORLD = 0xFFFF44FF;       // Розовый
   private static final int COLOR_MISC = 0xFF44FFFF;        // Голубой
   private static final int COLOR_PANIC = 0xFFFF0000;       // Ярко-красный
   private static final int COLOR_GUI = 0xFF888888;         // Серый
   
   public MobileButtons() {
      super("MobileButtons", "Virtual buttons for mobile devices", Module.Category.RENDER);
      this.addSettings(new Setting[]{
         this.enabled, this.buttonSize, this.spacing, this.columns, 
         this.position, this.customX, this.customY,
         this.opacity, this.showNames, this.showKeybinds, 
         this.roundedButtons, this.pulseAnimation, this.showCategory,
         this.quickToggle, this.holdToActivate, this.holdDelay,
         this.combatButtons, this.movementButtons, this.renderButtons,
         this.playerButtons, this.worldButtons, this.miscButtons,
         this.showPanicButton, this.showGUIButton, this.showProfileButton
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      if (!initialized) {
         initButtons();
         initialized = true;
      }
   }
   
   private void initButtons() {
      buttons.clear();
      buttonMap.clear();
      
      // Добавляем кнопки для всех модулей
      for (Module module : ModuleManager.getModules()) {
         Module.Category cat = module.getCategory();
         
         boolean shouldAdd = false;
         int color = COLOR_MISC;
         
         switch (cat.name) {
            case "Combat":
               if (combatButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_COMBAT;
               }
               break;
            case "Movement":
               if (movementButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_MOVEMENT;
               }
               break;
            case "Render":
               if (renderButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_RENDER;
               }
               break;
            case "Player":
               if (playerButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_PLAYER;
               }
               break;
            case "World":
               if (worldButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_WORLD;
               }
               break;
            case "Misc":
               if (miscButtons.getValue()) {
                  shouldAdd = true;
                  color = COLOR_MISC;
               }
               break;
         }
         
         if (shouldAdd) {
            MobileButton btn = new MobileButton(module.getName(), module, cat.name);
            btn.color = color;
            buttons.add(btn);
            buttonMap.put(module.getName(), btn);
         }
      }
      
      // Добавляем специальные кнопки
      if (showPanicButton.getValue()) {
         MobileButton panic = new MobileButton("PANIC", null, "Special");
         panic.color = COLOR_PANIC;
         panic.displayName = "PANIC";
         buttons.add(0, panic);
      }
      
      if (showGUIButton.getValue()) {
         MobileButton gui = new MobileButton("GUI", null, "Special");
         gui.color = COLOR_GUI;
         gui.displayName = "GUI";
         buttons.add(0, gui);
      }
      
      updateButtonPositions();
   }
   
   private void updateButtonPositions() {
      if (mc.func_228018_at_() == null) return;
      
      int screenWidth = mc.func_228018_at_().func_228155_b_();
      int screenHeight = mc.func_228018_at_().func_228156_c_();
      
      float size = (float)buttonSize.getValue();
      float space = (float)spacing.getValue();
      int cols = columns.getValue();
      
      // Рассчитываем начальную позицию
      float startX, startY;
      String pos = position.getValue();
      
      switch (pos) {
         case "BottomLeft":
            startX = 10;
            startY = screenHeight - (size + space) * ((buttons.size() + cols - 1) / cols) - 10;
            break;
         case "BottomRight":
            startX = screenWidth - (size + space) * cols;
            startY = screenHeight - (size + space) * ((buttons.size() + cols - 1) / cols) - 10;
            break;
         case "TopLeft":
            startX = 10;
            startY = 10;
            break;
         case "TopRight":
            startX = screenWidth - (size + space) * cols;
            startY = 10;
            break;
         case "Center":
            int rows = (buttons.size() + cols - 1) / cols;
            startX = (screenWidth - (size + space) * cols) / 2;
            startY = (screenHeight - (size + space) * rows) / 2;
            break;
         case "Custom":
         default:
            startX = (float)customX.getValue();
            startY = (float)customY.getValue();
            break;
      }
      
      // Размещаем кнопки
      int col = 0;
      int row = 0;
      
      for (MobileButton btn : buttons) {
         btn.x = startX + col * (size + space);
         btn.y = startY + row * (size + space);
         btn.width = size;
         btn.height = size;
         
         col++;
         if (col >= cols) {
            col = 0;
            row++;
         }
      }
   }
   
   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
      if (!isToggled() || !enabled.getValue()) return;
      if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
      if (mc.field_71462_r != null) return; // Не показывать при открытом GUI
      
      MatrixStack matrix = event.getMatrixStack();
      FontRenderer font = mc.field_71466_p;
      
      animationTime += 0.05F;
      
      updateButtonPositions();
      
      for (MobileButton btn : buttons) {
         renderButton(matrix, btn, font);
      }
   }
   
   private void renderButton(MatrixStack matrix, MobileButton btn, FontRenderer font) {
      float alpha = (float)opacity.getValue();
      
      // Анимация пульсации для активных модулей
      if (pulseAnimation.getValue() && btn.module != null && btn.module.isToggled()) {
         float pulse = (float)(Math.sin(animationTime * 2) * 0.2 + 0.8);
         alpha *= pulse;
      }
      
      // Цвет кнопки
      int color = btn.color;
      
      // Если модуль активен - делаем ярче
      if (btn.module != null && btn.module.isToggled()) {
         color = 0xFFFFFFFF;
      }
      
      // Если нажата - темнее
      if (btn.isPressed) {
         alpha *= 0.7F;
      }
      
      // Рисуем кнопку
      RenderSystem.enableBlend();
      RenderSystem.disableTexture();
      RenderSystem.blendFunc(770, 771);
      
      matrix.func_227860_a_();
      
      // Фон кнопки
      float r = ((color >> 16) & 0xFF) / 255.0F;
      float g = ((color >> 8) & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      
      if (roundedButtons.getValue()) {
         drawRoundedRect(matrix, btn.x, btn.y, btn.width, btn.height, 8, r, g, b, alpha);
      } else {
         drawRect(matrix, btn.x, btn.y, btn.width, btn.height, r, g, b, alpha);
      }
      
      // Рамка для активных модулей
      if (btn.module != null && btn.module.isToggled()) {
         if (roundedButtons.getValue()) {
            drawRoundedOutline(matrix, btn.x, btn.y, btn.width, btn.height, 8, 1.5F, 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            drawOutline(matrix, btn.x, btn.y, btn.width, btn.height, 1.5F, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
      
      RenderSystem.enableTexture();
      
      // Текст на кнопке
      if (showNames.getValue()) {
         String text = btn.displayName;
         if (text.length() > 5) {
            text = text.substring(0, 5);
         }
         
         int textWidth = font.func_78256_a(text);
         float textX = btn.x + (btn.width - textWidth) / 2;
         float textY = btn.y + (btn.height - 8) / 2;
         
         font.func_238421_b_(matrix, text, textX, textY, 0xFFFFFFFF);
         
         // Показываем категорию
         if (showCategory.getValue() && btn.category != null) {
            String cat = btn.category.substring(0, Math.min(3, btn.category.length()));
            font.func_238407_a_(matrix, cat, btn.x + 2, btn.y + btn.height - 8, 0xFFAAAAAA);
         }
      }
      
      matrix.func_227865_b_();
   }
   
   private void drawRoundedRect(MatrixStack matrix, float x, float y, float w, float h, 
                                 float radius, float r, float g, float b, float a) {
      // Упрощенная реализация - можно улучшить с помощью шейдеров
      drawRect(matrix, x + radius, y, w - radius * 2, h, r, g, b, a);
      drawRect(matrix, x, y + radius, w, h - radius * 2, r, g, b, a);
   }
   
   private void drawRect(MatrixStack matrix, float x, float y, float w, float h,
                         float r, float g, float b, float a) {
      net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.func_178181_a();
      net.minecraft.client.renderer.BufferBuilder buffer = tessellator.func_178180_c();
      Matrix4f matrix4f = matrix.func_227866_c_().func_227870_a_();
      
      buffer.func_181668_a(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.field_181706_f);
      buffer.func_227888_a_(matrix4f, x, y + h, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x + w, y + h, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x + w, y, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x, y, 0).func_227885_a_(r, g, b, a).func_181675_d();
      tessellator.func_78381_a();
   }
   
   private void drawRoundedOutline(MatrixStack matrix, float x, float y, float w, float h,
                                    float radius, float lineWidth, float r, float g, float b, float a) {
      RenderSystem.lineWidth(lineWidth);
      drawOutline(matrix, x, y, w, h, lineWidth, r, g, b, a);
   }
   
   private void drawOutline(MatrixStack matrix, float x, float y, float w, float h,
                            float lineWidth, float r, float g, float b, float a) {
      net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.func_178181_a();
      net.minecraft.client.renderer.BufferBuilder buffer = tessellator.func_178180_c();
      Matrix4f matrix4f = matrix.func_227866_c_().func_227870_a_();
      
      RenderSystem.lineWidth(lineWidth);
      buffer.func_181668_a(2, net.minecraft.client.renderer.vertex.DefaultVertexFormats.field_181706_f);
      buffer.func_227888_a_(matrix4f, x, y, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x + w, y, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x + w, y + h, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x, y + h, 0).func_227885_a_(r, g, b, a).func_181675_d();
      buffer.func_227888_a_(matrix4f, x, y, 0).func_227885_a_(r, g, b, a).func_181675_d();
      tessellator.func_78381_a();
   }
   
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) return;
      if (!isToggled() || !enabled.getValue()) return;
      
      tickCounter++;
      
      // Проверяем клики (для мобильных устройств через мышь/касание)
      if (mc.field_71474_y != null) {
         // Проверяем левый клик
         if (mc.field_71474_y.field_74313_A.func_151470_d()) {
            handleClick();
         }
         
         // Проверяем удержание
         if (holdToActivate.getValue()) {
            checkHold();
         }
      }
   }
   
   private void handleClick() {
      if (mc.func_228018_at_() == null) return;
      
      // Получаем позицию курсора/касания
      double mouseX = mc.field_71474_y.field_74317_l;
      double mouseY = mc.field_71474_y.field_74318_m;
      
      // Масштабирование под размер экрана
      int screenWidth = mc.func_228018_at_().func_228155_b_();
      int screenHeight = mc.func_228018_at_().func_228156_c_();
      int scaledWidth = mc.func_228018_at_().func_228155_b_();
      int scaledHeight = mc.func_228018_at_().func_228156_c_();
      
      double scaleX = (double)scaledWidth / mc.func_228018_at_().func_228155_b_();
      double scaleY = (double)scaledHeight / mc.func_228018_at_().func_228156_c_();
      
      // Находим нажатую кнопку
      for (MobileButton btn : buttons) {
         if (mouseX >= btn.x && mouseX <= btn.x + btn.width &&
             mouseY >= btn.y && mouseY <= btn.y + btn.height) {
            
            if (quickToggle.getValue()) {
               handleButtonAction(btn);
            }
            
            btn.isPressed = true;
            btn.pressTime = System.currentTimeMillis();
            break;
         }
      }
   }
   
   private void checkHold() {
      long currentTime = System.currentTimeMillis();
      
      for (MobileButton btn : buttons) {
         if (btn.isPressed) {
            if (currentTime - btn.pressTime >= holdDelay.getValue()) {
               // Активируем пока удерживаем
               if (btn.module != null && !btn.module.isToggled()) {
                  btn.module.toggle();
               }
            }
         }
      }
   }
   
   private void handleButtonAction(MobileButton btn) {
      if (btn.module != null) {
         // Переключаем модуль
         btn.module.toggle();
      } else {
         // Специальные кнопки
         switch (btn.name) {
            case "PANIC":
               panicMode();
               break;
            case "GUI":
               openGUI();
               break;
            case "Profile":
               openProfile();
               break;
         }
      }
   }
   
   private void panicMode() {
      // Отключаем все модули
      for (Module module : ModuleManager.getModules()) {
         if (module.isToggled() && module != this) {
            module.toggle();
         }
      }
      
      if (mc.field_71439_g != null) {
         mc.field_71439_g.func_145747_a(
            new net.minecraft.util.text.StringTextComponent("§c[MobileButtons] §fPANIC! All modules disabled."),
            mc.field_71439_g.func_145748_c_()
         );
      }
   }
   
   private void openGUI() {
      // Открываем ClickGUI
      Module guiModule = ModuleManager.getModuleByName("ClickGUI");
      if (guiModule != null && !guiModule.isToggled()) {
         guiModule.toggle();
      }
   }
   
   private void openProfile() {
      // Открываем профиль настроек
      // Можно добавить позже
   }
   
   // API для внешнего использования
   public void addButton(String name, Module module, String category) {
      MobileButton btn = new MobileButton(name, module, category);
      buttons.add(btn);
      buttonMap.put(name, btn);
      updateButtonPositions();
   }
   
   public void removeButton(String name) {
      MobileButton btn = buttonMap.get(name);
      if (btn != null) {
         buttons.remove(btn);
         buttonMap.remove(name);
         updateButtonPositions();
      }
   }
   
   public List<String> getButtonNames() {
      List<String> names = new ArrayList<>();
      for (MobileButton btn : buttons) {
         names.add(btn.name);
      }
      return names;
   }
}
