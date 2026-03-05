package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NoRender extends Module {
   // Визуальные эффекты
   private Setting.Boolean fire = new Setting.Boolean("Fire", true);
   private Setting.Boolean blindness = new Setting.Boolean("Blindness", true);
   private Setting.Boolean nausea = new Setting.Boolean("Nausea", true);
   private Setting.Boolean pumpkin = new Setting.Boolean("Pumpkin", true);
   private Setting.Boolean water = new Setting.Boolean("Water", true);
   private Setting.Boolean lava = new Setting.Boolean("Lava", true);
   private Setting.Boolean inWall = new Setting.Boolean("InWall", true);
   
   // Погода
   private Setting.Boolean rain = new Setting.Boolean("Rain", true);
   private Setting.Boolean snow = new Setting.Boolean("Snow", true);
   private Setting.Boolean thunder = new Setting.Boolean("Thunder", true);
   
   // Сущности
   private Setting.Boolean items = new Setting.Boolean("Items", false);
   private Setting.Boolean xpOrbs = new Setting.Boolean("XPOrbs", false);
   private Setting.Boolean arrows = new Setting.Boolean("Arrows", false);
   private Setting.Boolean armorStands = new Setting.Boolean("ArmorStands", false);
   
   // HUD элементы
   private Setting.Boolean bossBar = new Setting.Boolean("BossBar", false);
   private Setting.Boolean scoreboard = new Setting.Boolean("Scoreboard", false);
   private Setting.Boolean crosshair = new Setting.Boolean("Crosshair", false);
   private Setting.Boolean hotbar = new Setting.Boolean("Hotbar", false);
   
   // Дополнительные
   private Setting.Boolean totemAnimation = new Setting.Boolean("TotemAnimation", false);
   private Setting.Boolean explosion = new Setting.Boolean("Explosion", true);
   private Setting.Boolean beaconBeams = new Setting.Boolean("BeaconBeams", false);
   private Setting.Boolean enchantTable = new Setting.Boolean("EnchantTable", false);
   private Setting.Boolean particles = new Setting.Boolean("Particles", false);
   
   // Туман
   private Setting.Boolean fog = new Setting.Boolean("Fog", false);
   private Setting.Double fogDensity = new Setting.Double("FogDensity", 0.0D, 0.0D, 1.0D);

   public NoRender() {
      super("NoRender", "Disable visual effects", Module.Category.RENDER);
      this.addSettings(new Setting[]{
         // Визуальные эффекты
         this.fire, this.blindness, this.nausea, this.pumpkin, 
         this.water, this.lava, this.inWall,
         // Погода
         this.rain, this.snow, this.thunder,
         // Сущности
         this.items, this.xpOrbs, this.arrows, this.armorStands,
         // HUD
         this.bossBar, this.scoreboard, this.crosshair, this.hotbar,
         // Дополнительные
         this.totemAnimation, this.explosion, this.beaconBeams, 
         this.enchantTable, this.particles,
         // Туман
         this.fog, this.fogDensity
      });
   }
   
   // Отмена оверлеев блоков (огонь, вода, лава, тыква, стена)
   @SubscribeEvent
   public void onRenderBlockOverlay(RenderBlockOverlayEvent event) {
      if (mc.field_71439_g == null) return;
      
      switch (event.getOverlayType()) {
         case FIRE:
            if (this.fire.getValue()) event.setCanceled(true);
            break;
         case WATER:
            if (this.water.getValue()) event.setCanceled(true);
            break;
         case LAVA:
            if (this.lava.getValue()) event.setCanceled(true);
            break;
         case IN_WALL:
            if (this.inWall.getValue()) event.setCanceled(true);
            break;
      }
   }
   
   // Отмена рендера оверлея тыквы
   @SubscribeEvent
   public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
      // Можно добавить логику для скрытия сущностей
   }
   
   // Удаление эффектов слепоты и тошноты
   public boolean shouldRemoveBlindness() {
      return this.isToggled() && this.blindness.getValue() && 
             mc.field_71439_g != null && 
             mc.field_71439_g.func_70644_a(Effects.field_76440_q);
   }
   
   public boolean shouldRemoveNausea() {
      return this.isToggled() && this.nausea.getValue() && 
             mc.field_71439_g != null && 
             mc.field_71439_g.func_70644_a(Effects.field_76431_u);
   }
   
   // Проверка погоды
   public boolean shouldRemoveRain() {
      return this.isToggled() && this.rain.getValue();
   }
   
   public boolean shouldRemoveSnow() {
      return this.isToggled() && this.snow.getValue();
   }
   
   public boolean shouldRemoveThunder() {
      return this.isToggled() && this.thunder.getValue();
   }
   
   // Проверка сущностей для рендера
   public boolean shouldRenderEntity(Entity entity) {
      if (!this.isToggled()) return true;
      
      if (entity instanceof ItemEntity && this.items.getValue()) return false;
      if (entity instanceof ExperienceOrbEntity && this.xpOrbs.getValue()) return false;
      if (entity instanceof ArrowEntity && this.arrows.getValue()) return false;
      
      return true;
   }
   
   // Проверка HUD элементов
   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
      if (!this.isToggled()) return;
      
      switch (event.getType()) {
         case BOSSHEALTH:
         case BOSSINFO:
            if (this.bossBar.getValue()) event.setCanceled(true);
            break;
         case PLAYER_LIST:
            // Не отменяем, это таб
            break;
         case CROSSHAIRS:
            if (this.crosshair.getValue()) event.setCanceled(true);
            break;
         case HOTBAR:
            if (this.hotbar.getValue()) event.setCanceled(true);
            break;
         case EXPERIENCE:
         case JUMPBAR:
            // Можно добавить настройки
            break;
         default:
            break;
      }
   }
   
   // Настройка тумана
   @SubscribeEvent
   public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
      if (this.isToggled() && this.fog.getValue()) {
         event.setDensity((float)this.fogDensity.getValue());
         event.setCanceled(true);
      }
   }
   
   @SubscribeEvent
   public void onFogColor(EntityViewRenderEvent.FogColors event) {
      // Можно изменить цвет тумана при необходимости
   }
   
   // Проверка на тыкву
   public boolean shouldRemovePumpkin() {
      if (!this.isToggled() || !this.pumpkin.getValue()) return false;
      if (mc.field_71439_g == null) return false;
      
      // Проверяем, надета ли тыква
      return mc.field_71439_g.func_184582_a(net.minecraft.inventory.EquipmentSlotType.HEAD).func_77973_b() == Blocks.field_150423_aK.asItem();
   }
   
   // Проверка тотемной анимации
   public boolean shouldCancelTotemAnimation() {
      return this.isToggled() && this.totemAnimation.getValue();
   }
   
   // Проверка частиц
   public boolean shouldRemoveParticles() {
      return this.isToggled() && this.particles.getValue();
   }
   
   // Проверка луча маяка
   public boolean shouldRemoveBeaconBeam() {
      return this.isToggled() && this.beaconBeams.getValue();
   }
   
   // Проверка стола зачарования
   public boolean shouldRemoveEnchantTableBook() {
      return this.isToggled() && this.enchantTable.getValue();
   }
   
   // Получить модификатор прозрачности тумана
   public float getFogDensity() {
      if (this.isToggled() && this.fog.getValue()) {
         return (float)this.fogDensity.getValue();
      }
      return -1.0F; // По умолчанию
   }
}
