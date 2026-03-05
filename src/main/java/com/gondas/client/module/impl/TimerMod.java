package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

import java.lang.reflect.Field;

public class TimerMod extends Module {
   private Setting.Double speed = new Setting.Double("Speed", 1.5D, 0.1D, 10.0D);
   private Setting.Boolean smooth = new Setting.Boolean("Smooth", true);
   private Setting.Double smoothSpeed = new Setting.Double("SmoothSpeed", 0.1D, 0.01D, 0.5D);
   private Setting.Mode mode = new Setting.Mode("Mode", new String[]{"Normal", "Pulse", "Oscillate", "Random"});
   private Setting.Double pulseMin = new Setting.Double("PulseMin", 1.0D, 0.1D, 5.0D);
   private Setting.Double pulseMax = new Setting.Double("PulseMax", 3.0D, 0.5D, 10.0D);
   private Setting.Int pulseInterval = new Setting.Int("PulseInterval", 20, 5, 100);
   private Setting.Boolean onGroundOnly = new Setting.Boolean("OnGroundOnly", false);
   private Setting.Boolean onMoveOnly = new Setting.Boolean("OnMoveOnly", false);
   
   private float currentSpeed = 1.0F;
   private int pulseTimer = 0;
   private boolean pulseState = false;
   private int oscillateDirection = 1;
   
   private static Field timerField;
   private static Field tickLengthField;
   
   static {
      try {
         // Получаем доступ к таймеру Minecraft через рефлексию
         timerField = Minecraft.class.getDeclaredField("field_71428_T");
         timerField.setAccessible(true);
         
         // Получаем доступ к tickLength в Timer
         tickLengthField = Timer.class.getDeclaredField("field_74278_d");
         tickLengthField.setAccessible(true);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public TimerMod() {
      super("Timer", "Change game speed", Module.Category.MISC);
      this.addSettings(new Setting[]{
         this.speed, this.smooth, this.smoothSpeed, this.mode,
         this.pulseMin, this.pulseMax, this.pulseInterval,
         this.onGroundOnly, this.onMoveOnly
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      currentSpeed = 1.0F;
      pulseTimer = 0;
      pulseState = false;
      oscillateDirection = 1;
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      setTimerSpeed(1.0F);
   }
   
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) return;
      if (mc.field_71439_g == null) return;
      
      // Проверка условий
      if (onGroundOnly.getValue() && !mc.field_71439_g.func_233570_aj_()) {
         setTimerSpeed(1.0F);
         return;
      }
      
      if (onMoveOnly.getValue()) {
         double dx = mc.field_71439_g.func_226277_ct_() - mc.field_71439_g.field_70169_q;
         double dz = mc.field_71439_g.func_226281_cx_() - mc.field_71439_g.field_70166_s;
         if (dx * dx + dz * dz < 0.01D) {
            setTimerSpeed(1.0F);
            return;
         }
      }
      
      // Вычисляем целевую скорость
      float targetSpeed = calculateTargetSpeed();
      
      // Плавное изменение скорости
      if (smooth.getValue()) {
         float diff = targetSpeed - currentSpeed;
         if (Math.abs(diff) > 0.01F) {
            currentSpeed += diff * (float)smoothSpeed.getValue();
         } else {
            currentSpeed = targetSpeed;
         }
      } else {
         currentSpeed = targetSpeed;
      }
      
      // Применяем скорость
      setTimerSpeed(currentSpeed);
   }
   
   private float calculateTargetSpeed() {
      String modeStr = this.mode.getValue();
      float baseSpeed = (float)this.speed.getValue();
      
      switch (modeStr) {
         case "Pulse":
            pulseTimer++;
            if (pulseTimer >= this.pulseInterval.getValue()) {
               pulseTimer = 0;
               pulseState = !pulseState;
            }
            return pulseState ? (float)this.pulseMax.getValue() : (float)this.pulseMin.getValue();
            
         case "Oscillate":
            float min = (float)this.pulseMin.getValue();
            float max = (float)this.pulseMax.getValue();
            currentSpeed += oscillateDirection * 0.05F;
            if (currentSpeed >= max) {
               currentSpeed = max;
               oscillateDirection = -1;
            } else if (currentSpeed <= min) {
               currentSpeed = min;
               oscillateDirection = 1;
            }
            return currentSpeed;
            
         case "Random":
            float randomSpeed = baseSpeed + (float)(Math.random() - 0.5) * 0.5F;
            return Math.max(0.1F, randomSpeed);
            
         default:
            return baseSpeed;
      }
   }
   
   private void setTimerSpeed(float speed) {
      try {
         if (timerField != null && tickLengthField != null) {
            Timer timer = (Timer) timerField.get(mc);
            if (timer != null) {
               // tickLength = 1000.0F / speed
               // Базовое значение = 50.0F (1000 / 20 тиков в секунду)
               float tickLength = 50.0F / speed;
               tickLengthField.setFloat(timer, tickLength);
            }
         }
      } catch (Exception e) {
         // Альтернативный способ через MinecraftForge если доступен
         try {
            // Попробуем найти другой способ
            Timer timer = mc.field_71428_T;
            if (timer != null) {
               Field field = Timer.class.getDeclaredField("field_74278_d");
               field.setAccessible(true);
               float tickLength = 50.0F / speed;
               field.setFloat(timer, tickLength);
            }
         } catch (Exception ex) {
            // Игнорируем ошибки
         }
      }
   }
   
   public float getCurrentSpeed() {
      return currentSpeed;
   }
}
