package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerPacket.PositionPacket;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Criticals extends Module {
   private Setting.Mode mode = new Setting.Mode("Mode", new String[]{"Packet", "Jump", "MiniJump", "NCP", "AAC", "Matrix", "Grim", "Sparky", " Vulcan", "Spartan"});
   private Setting.Boolean onlyOnGround = new Setting.Boolean("OnlyGround", true);
   private Setting.Boolean movingOnly = new Setting.Boolean("MovingOnly", false);
   private Setting.Double delay = new Setting.Double("Delay", 0.0D, 0.0D, 500.0D);
   private Setting.Boolean onlyWeapon = new Setting.Boolean("OnlyWeapon", true);
   private Setting.Boolean smartCrit = new Setting.Boolean("SmartCrit", true);
   private Setting.Double minDamage = new Setting.Double("MinDamage", 0.5D, 0.0D, 2.0D);
   private Setting.Boolean ignoreWater = new Setting.Boolean("IgnoreWater", false);
   private Setting.Boolean ignoreWeb = new Setting.Boolean("IgnoreWeb", false);
   
   private Random random = new Random();
   private long lastCrit = 0L;
   private int critCounter = 0;

   public Criticals() {
      super("Criticals", "Auto critical hits", Module.Category.COMBAT);
      this.addSettings(new Setting[]{
         this.mode, this.onlyOnGround, this.movingOnly, this.delay,
         this.onlyWeapon, this.smartCrit, this.minDamage,
         this.ignoreWater, this.ignoreWeb
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      critCounter = 0;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onAttack(AttackEntityEvent event) {
      if (mc.field_71439_g == null || event.getTarget() == null) return;
      
      // Проверка задержки
      if ((double)(System.currentTimeMillis() - this.lastCrit) < this.delay.getValue()) return;
      
      // Проверка на землю
      if (this.onlyOnGround.getValue() && !mc.field_71439_g.func_233570_aj_()) return;
      
      // Проверка движения
      if (this.movingOnly.getValue()) {
         double dx = mc.field_71439_g.func_226277_ct_() - mc.field_71439_g.field_70169_q;
         double dz = mc.field_71439_g.func_226281_cx_() - mc.field_71439_g.field_70166_s;
         if (dx * dx + dz * dz < 0.01D) return;
      }
      
      // Проверка на оружие
      if (this.onlyWeapon.getValue()) {
         if (!(mc.field_71439_g.func_184614_ca().func_77973_b() instanceof net.minecraft.item.SwordItem) &&
             !(mc.field_71439_g.func_184614_ca().func_77973_b() instanceof net.minecraft.item.AxeItem)) {
            return;
         }
      }
      
      // Проверка на воду
      if (!this.ignoreWater.getValue() && mc.field_71439_g.func_208600_a_()) {
         return;
      }
      
      // Проверка на паутину
      if (!this.ignoreWeb.getValue() && mc.field_71439_g.func_225508_ch_()) {
         return;
      }
      
      // Умные криты - проверяем, нужен ли крит
      if (this.smartCrit.getValue()) {
         // Не делаем крит если цель уже мертва или имеет низкое здоровье
         if (event.getTarget() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getTarget();
            if (target.func_110143_aJ() <= 0) return;
         }
      }

      String m = this.mode.getValue();
      
      switch (m) {
         case "Packet":
            doPacketCrit();
            break;
         case "Jump":
            doJumpCrit();
            break;
         case "MiniJump":
            doMiniJumpCrit();
            break;
         case "NCP":
            doNCPCrit();
            break;
         case "AAC":
            doAACCrit();
            break;
         case "Matrix":
            doMatrixCrit();
            break;
         case "Grim":
            doGrimCrit();
            break;
         case "Sparky":
            doSparkyCrit();
            break;
         case "Vulcan":
            doVulcanCrit();
            break;
         case "Spartan":
            doSpartanCrit();
            break;
      }

      this.lastCrit = System.currentTimeMillis();
      this.critCounter++;
   }
   
   private void sendPosition(double x, double y, double z, boolean onGround) {
      mc.field_71439_g.field_71174_a.func_147297_a(
         new PositionPacket(x, y, z, onGround)
      );
   }

   private void doPacketCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Классический пакетный крит
      sendPosition(x, y + 0.0625D, z, false);
      sendPosition(x, y, z, false);
   }

   private void doJumpCrit() {
      // Просто прыгаем
      mc.field_71439_g.func_70664_aZ();
   }

   private void doMiniJumpCrit() {
      // Маленький прыжок
      mc.field_71439_g.func_70024_g(0.0D, 0.12D, 0.0D);
   }

   private void doNCPCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // NCP обход
      sendPosition(x, y + 0.11D, z, false);
      sendPosition(x, y + 0.1100013579D, z, false);
      sendPosition(x, y + 1.3579E-6D, z, false);
   }

   private void doAACCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // AAC обход
      sendPosition(x, y + 0.42D, z, false);
      sendPosition(x, y + 0.75D, z, false);
   }

   private void doMatrixCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Matrix обход
      sendPosition(x, y + 0.2D, z, false);
      sendPosition(x, y + 0.12D, z, false);
   }

   private void doGrimCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Grim обход
      sendPosition(x, y + 0.06D, z, false);
      sendPosition(x, y, z, false);
   }
   
   private void doSparkyCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Sparky обход
      sendPosition(x, y + 0.0611D, z, false);
      sendPosition(x, y + 0.0612D, z, false);
   }
   
   private void doVulcanCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Vulcan обход
      sendPosition(x, y + 0.125D, z, false);
      sendPosition(x, y, z, false);
   }
   
   private void doSpartanCrit() {
      double x = mc.field_71439_g.func_226277_ct_();
      double y = mc.field_71439_g.func_226278_cu_();
      double z = mc.field_71439_g.func_226281_cx_();
      
      // Spartan обход
      sendPosition(x, y + 0.0525D, z, false);
      sendPosition(x, y + 0.001D, z, false);
   }
   
   public int getCritCount() {
      return critCounter;
   }
}
