package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.NoteBlockEvent;

import java.util.Random;

public class AutoFish extends Module {
   private Setting.Int delay = new Setting.Int("Delay", 5, 0, 20);
   private Setting.Int recastDelay = new Setting.Int("RecastDelay", 8, 1, 40);
   private Setting.Boolean autoRod = new Setting.Boolean("AutoRod", true);
   private Setting.Boolean autoSwitch = new Setting.Boolean("AutoSwitch", false);
   private Setting.Int rodSlot = new Setting.Int("RodSlot", 1, 1, 9);
   private Setting.Boolean detectionMode = new Setting.Boolean("SoundDetection", true);
   private Setting.Boolean motionDetection = new Setting.Boolean("MotionDetection", true);
   private Setting.Double motionThreshold = new Setting.Double("MotionThreshold", 0.1D, 0.01D, 0.5D);
   private Setting.Boolean antiBreak = new Setting.Boolean("AntiBreak", true);
   private Setting.Int minDurability = new Setting.Int("MinDurability", 5, 1, 50);
   private Setting.Boolean autoEat = new Setting.Boolean("AutoEat", false);
   private Setting.Int eatThreshold = new Setting.Int("EatThreshold", 10, 5, 20);
   private Setting.Boolean protectFromMobs = new Setting.Boolean("ProtectFromMobs", false);
   private Setting.Double mobRange = new Setting.Double("MobRange", 5.0D, 3.0D, 10.0D);
   private Setting.Boolean castOnEnable = new Setting.Boolean("CastOnEnable", true);
   private Setting.Boolean logCatches = new Setting.Boolean("LogCatches", true);
   
   private Random random = new Random();
   private boolean waiting = false;
   private int timer = 0;
   private int recastTimer = 0;
   private int catchCount = 0;
   private double lastMotionY = 0;
   private int originalSlot = -1;
   private long lastCatchTime = 0;
   
   public AutoFish() {
      super("AutoFish", "Smart auto fisher", Module.Category.WORLD);
      this.addSettings(new Setting[]{
         this.delay, this.recastDelay, this.autoRod, this.autoSwitch,
         this.rodSlot, this.detectionMode, this.motionDetection,
         this.motionThreshold, this.antiBreak, this.minDurability,
         this.autoEat, this.eatThreshold, this.protectFromMobs,
         this.mobRange, this.castOnEnable, this.logCatches
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      waiting = false;
      timer = 0;
      recastTimer = 0;
      originalSlot = -1;
      
      // Автоматически закидываем удочку при включении
      if (this.castOnEnable.getValue() && mc.field_71439_g != null) {
         if (this.autoSwitch.getValue()) {
            switchToRod();
         }
         
         if (isHoldingRod()) {
            mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, Hand.MAIN_HAND);
         }
      }
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      
      // Возвращаем слот
      if (this.autoSwitch.getValue() && originalSlot != -1 && mc.field_71439_g != null) {
         mc.field_71439_g.field_71071_by.field_70461_c = originalSlot;
         originalSlot = -1;
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) return;
      if (mc.field_71439_g == null || mc.field_71441_e == null) return;
      
      // Защита от мобов
      if (this.protectFromMobs.getValue() && isMobNearby()) {
         return;
      }
      
      // Авто-еда
      if (this.autoEat.getValue() && mc.field_71439_g.func_71024_bL() <= this.eatThreshold.getValue()) {
         return;
      }
      
      // Проверка на удочку
      if (!isHoldingRod()) {
         if (this.autoSwitch.getValue()) {
            switchToRod();
         } else if (this.autoRod.getValue()) {
            findAndSwitchToRod();
         } else {
            return;
         }
      }
      
      // Проверка прочности
      if (this.antiBreak.getValue() && isRodLowDurability()) {
         if (this.autoRod.getValue()) {
            findAndSwitchToRod();
         }
         return;
      }
      
      // Обработка таймеров
      if (recastTimer > 0) {
         recastTimer--;
         return;
      }
      
      if (timer > 0) {
         timer--;
         return;
      }
      
      // Проверяем поплавок
      FishingBobberEntity bobber = mc.field_71439_g.field_71104_cf;
      
      if (bobber != null && !waiting) {
         // Проверка клёва по движению
         if (this.motionDetection.getValue()) {
            Vector3d motion = bobber.func_213322_ci();
            double motionY = Math.abs(motion.field_72448_b);
            
            // Обнаруживаем резкое движение вниз (клёв)
            if (motionY > this.motionThreshold.getValue() && lastMotionY < motionY * 0.5D) {
               catchFish();
               return;
            }
            
            lastMotionY = motionY;
         }
         
         // Дополнительная проверка по погружению
         if (bobber.func_213322_ci().field_72448_b < -0.1D) {
            catchFish();
         }
      }
      
      // Если нет поплавка и не ждём - перезакидываем
      if (bobber == null && !waiting && isHoldingRod()) {
         recastRod();
      }
   }
   
   private void catchFish() {
      // Ловим рыбу
      mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, Hand.MAIN_HAND);
      
      waiting = true;
      timer = this.delay.getValue() + random.nextInt(3);
      catchCount++;
      lastCatchTime = System.currentTimeMillis();
      
      if (this.logCatches.getValue()) {
         // Логируем улов (можно добавить сообщение в чат)
      }
   }
   
   private void recastRod() {
      mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, Hand.MAIN_HAND);
      recastTimer = this.recastDelay.getValue() + random.nextInt(5);
      waiting = false;
   }
   
   private boolean isHoldingRod() {
      if (mc.field_71439_g == null) return false;
      return mc.field_71439_g.func_184614_ca().func_77973_b() instanceof FishingRodItem;
   }
   
   private void switchToRod() {
      if (mc.field_71439_g == null) return;
      
      // Запоминаем текущий слот
      if (originalSlot == -1) {
         originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
      }
      
      // Переключаемся на указанный слот
      mc.field_71439_g.field_71071_by.field_70461_c = this.rodSlot.getValue() - 1;
   }
   
   private void findAndSwitchToRod() {
      if (mc.field_71439_g == null) return;
      
      // Ищем удочку в инвентаре
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
         if (stack.func_77973_b() instanceof FishingRodItem) {
            if (!this.antiBreak.getValue() || !isLowDurability(stack)) {
               mc.field_71439_g.field_71071_by.field_70461_c = i;
               return;
            }
         }
      }
   }
   
   private boolean isRodLowDurability() {
      if (mc.field_71439_g == null) return false;
      ItemStack stack = mc.field_71439_g.func_184614_ca();
      return isLowDurability(stack);
   }
   
   private boolean isLowDurability(ItemStack stack) {
      if (!stack.func_77984_f()) return false;
      int max = stack.func_77958_k();
      int current = max - stack.func_77952_i();
      return current <= this.minDurability.getValue();
   }
   
   private boolean isMobNearby() {
      if (mc.field_71439_g == null || mc.field_71441_e == null) return false;
      
      return mc.field_71441_e.func_217416_b().stream()
         .filter(e -> e instanceof net.minecraft.entity.monster.MonsterEntity)
         .anyMatch(e -> e.func_70032_d(mc.field_71439_g) <= this.mobRange.getValue());
   }
   
   // Проверка воды под поплавком
   private boolean isWaterAtBobber() {
      FishingBobberEntity bobber = mc.field_71439_g.field_71104_cf;
      if (bobber == null || mc.field_71441_e == null) return false;
      
      BlockPos pos = bobber.func_233580_cy_();
      return mc.field_71441_e.func_180495_p(pos).func_177230_c() == Blocks.field_150358_i;
   }
   
   public int getCatchCount() {
      return catchCount;
   }
   
   public long getLastCatchTime() {
      return lastCatchTime;
   }
}
