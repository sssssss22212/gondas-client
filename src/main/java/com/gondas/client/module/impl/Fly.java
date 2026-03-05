package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.network.play.client.CPlayerPacket.PositionPacket;
import net.minecraft.util.math.vector.Vector3d;

public class Fly extends Module {
   private Setting.Mode mode = new Setting.Mode("Mode", new String[]{"Vanilla", "Glide", "Packet", "Creative"});
   private Setting.Double speed = new Setting.Double("Speed", 2.0D, 0.1D, 10.0D);
   private Setting.Boolean antiKick = new Setting.Boolean("AntiKick", true);
   private int tickCounter = 0;
   private double startY = 0.0D;

   public Fly() {
      super("Fly", "Fly like a bird", Module.Category.MOVEMENT);
      this.addSettings(new Setting[]{this.mode, this.speed, this.antiKick});
   }

   public void onEnable() {
      if (mc.field_71439_g != null) {
         this.startY = mc.field_71439_g.func_226278_cu_();
      }

   }

   public void onTick() {
      if (mc.field_71439_g != null) {
         String m = this.mode.getValue();
         if (m.equals("Vanilla")) {
            mc.field_71439_g.field_71075_bZ.field_75100_b = true;
            mc.field_71439_g.field_71075_bZ.func_195931_a((float)(this.speed.getValue() / 5.0D));
         } else if (m.equals("Creative")) {
            mc.field_71439_g.field_71075_bZ.field_75100_b = true;
            mc.field_71439_g.field_71075_bZ.func_195931_a((float)(this.speed.getValue() / 5.0D));
            if (!mc.field_71439_g.field_71075_bZ.field_75098_d) {
               mc.field_71439_g.field_71075_bZ.field_75102_a = true;
            }
         } else if (m.equals("Glide")) {
            Vector3d motion = mc.field_71439_g.func_213322_ci();
            double speedVal = this.speed.getValue();
            double yaw = Math.toRadians((double)mc.field_71439_g.field_70177_z);
            double moveX = 0.0D;
            double moveZ = 0.0D;
            if (mc.field_71474_y.field_74351_w.func_151470_d()) {
               moveX -= Math.sin(yaw) * speedVal;
               moveZ += Math.cos(yaw) * speedVal;
            }

            if (mc.field_71474_y.field_74368_y.func_151470_d()) {
               moveX += Math.sin(yaw) * speedVal;
               moveZ -= Math.cos(yaw) * speedVal;
            }

            if (mc.field_71474_y.field_74370_x.func_151470_d()) {
               moveX += Math.cos(yaw) * speedVal;
               moveZ += Math.sin(yaw) * speedVal;
            }

            if (mc.field_71474_y.field_74366_z.func_151470_d()) {
               moveX -= Math.cos(yaw) * speedVal;
               moveZ -= Math.sin(yaw) * speedVal;
            }

            mc.field_71439_g.func_213293_j(moveX, motion.field_72448_b, moveZ);
            if (mc.field_71474_y.field_74314_A.func_151470_d()) {
               mc.field_71439_g.func_213293_j(moveX, speedVal, moveZ);
            } else if (mc.field_71474_y.field_228046_af_.func_151470_d()) {
               mc.field_71439_g.func_213293_j(moveX, -speedVal, moveZ);
            } else {
               mc.field_71439_g.func_213293_j(moveX, -0.01D, moveZ);
            }
         } else if (m.equals("Packet")) {
            double speedVal = this.speed.getValue();
            double yaw = Math.toRadians((double)mc.field_71439_g.field_70177_z);
            double moveX = 0.0D;
            double moveY = 0.0D;
            double moveZ = 0.0D;
            if (mc.field_71474_y.field_74351_w.func_151470_d()) {
               moveX -= Math.sin(yaw) * speedVal;
               moveZ += Math.cos(yaw) * speedVal;
            }

            if (mc.field_71474_y.field_74368_y.func_151470_d()) {
               moveX += Math.sin(yaw) * speedVal;
               moveZ -= Math.cos(yaw) * speedVal;
            }

            if (mc.field_71474_y.field_74314_A.func_151470_d()) {
               moveY = speedVal;
            }

            if (mc.field_71474_y.field_228046_af_.func_151470_d()) {
               moveY = -speedVal;
            }

            double newX = mc.field_71439_g.func_226277_ct_() + moveX;
            double newY = mc.field_71439_g.func_226278_cu_() + moveY;
            double newZ = mc.field_71439_g.func_226281_cx_() + moveZ;
            mc.field_71439_g.func_70107_b(newX, newY, newZ);
            mc.field_71439_g.field_71174_a.func_147297_a(new PositionPacket(newX, newY, newZ, mc.field_71439_g.func_233570_aj_()));
            ++this.tickCounter;
            if (this.antiKick.getValue() && this.tickCounter >= 40) {
               this.tickCounter = 0;
               mc.field_71439_g.func_70107_b(newX, newY - 0.1D, newZ);
               mc.field_71439_g.field_71174_a.func_147297_a(new PositionPacket(newX, newY - 0.1D, newZ, true));
            }
         }

      }
   }

   public void onDisable() {
      if (mc.field_71439_g != null) {
         mc.field_71439_g.field_71075_bZ.field_75100_b = false;
         mc.field_71439_g.field_71075_bZ.func_195931_a(0.05F);
         if (!mc.field_71439_g.field_71075_bZ.field_75098_d) {
            mc.field_71439_g.field_71075_bZ.field_75102_a = false;
         }
      }

   }
}
