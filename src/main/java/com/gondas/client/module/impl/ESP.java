package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ESP extends Module {
   private Setting.Boolean players = new Setting.Boolean("Players", true);
   private Setting.Boolean hostile = new Setting.Boolean("Hostile", true);
   private Setting.Boolean passive = new Setting.Boolean("Passive", false);
   private Setting.Boolean names = new Setting.Boolean("Names", true);
   private Setting.Boolean health = new Setting.Boolean("Health", true);
   private Setting.Double range = new Setting.Double("Range", 100.0D, 10.0D, 200.0D);
   private Setting.Mode mode = new Setting.Mode("Mode", new String[]{"Box", "Outline", "Full"});
   private Setting.Double lineWidth = new Setting.Double("LineWidth", 2.0D, 1.0D, 5.0D);

   public ESP() {
      super("ESP", "See entities through walls", Module.Category.RENDER);
      this.addSettings(new Setting[]{this.players, this.hostile, this.passive, this.names, this.health, this.range, this.mode, this.lineWidth});
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if (mc.field_71441_e != null && mc.field_71439_g != null) {
         MatrixStack matrix = event.getMatrixStack();
         Vector3d camPos = mc.field_71460_t.func_215316_n().func_216785_c();
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.disableLighting();
         RenderSystem.disableTexture();
         RenderSystem.lineWidth((float)this.lineWidth.getValue());
         Iterator var4 = mc.field_71441_e.func_217416_b().iterator();

         while(true) {
            Entity entity;
            double dist;
            do {
               do {
                  do {
                     if (!var4.hasNext()) {
                        RenderSystem.enableDepthTest();
                        RenderSystem.enableTexture();
                        RenderSystem.disableBlend();
                        return;
                     }

                     entity = (Entity)var4.next();
                  } while(!(entity instanceof LivingEntity));
               } while(entity == mc.field_71439_g);

               dist = (double)mc.field_71439_g.func_70032_d(entity);
            } while(dist > this.range.getValue());

            LivingEntity living = (LivingEntity)entity;
            float r = 1.0F;
            float g = 0.0F;
            float b = 0.0F;
            boolean shouldRender = false;
            if (entity instanceof PlayerEntity && this.players.getValue()) {
               r = 1.0F;
               g = 0.2F;
               b = 0.2F;
               shouldRender = true;
            } else if (entity instanceof MonsterEntity && this.hostile.getValue()) {
               r = 1.0F;
               g = 0.0F;
               b = 1.0F;
               shouldRender = true;
            } else if (entity instanceof AnimalEntity && this.passive.getValue()) {
               r = 0.0F;
               g = 1.0F;
               b = 0.0F;
               shouldRender = true;
            }

            if (shouldRender) {
               matrix.func_227860_a_();
               double x = entity.func_226277_ct_() - camPos.field_72450_a;
               double y = entity.func_226278_cu_() - camPos.field_72448_b;
               double z = entity.func_226281_cx_() - camPos.field_72449_c;
               float width = entity.func_213311_cf();
               float height = entity.func_213302_cg();
               double minX = x - (double)width / 2.0D;
               double maxX = x + (double)width / 2.0D;
               double maxY = y + (double)height;
               double minZ = z - (double)width / 2.0D;
               double maxZ = z + (double)width / 2.0D;
               Matrix4f matrix4f = matrix.func_227866_c_().func_227870_a_();
               Tessellator tessellator = Tessellator.func_178181_a();
               BufferBuilder buffer = tessellator.func_178180_c();
               String m = this.mode.getValue();
               if (m.equals("Full")) {
                  buffer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                  buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)minZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)minZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 0.15F).func_181675_d();
                  tessellator.func_78381_a();
               }

               buffer.func_181668_a(1, DefaultVertexFormats.field_181706_f);
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)minZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)maxX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)y, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               buffer.func_227888_a_(matrix4f, (float)minX, (float)maxY, (float)maxZ).func_227885_a_(r, g, b, 1.0F).func_181675_d();
               tessellator.func_78381_a();
               if (this.health.getValue()) {
                  float healthPercent = living.func_110143_aJ() / living.func_110138_aP();
                  float barWidth = width * healthPercent;
                  float barHeight = 0.15F;
                  float barY = (float)maxY + 0.3F;
                  buffer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                  buffer.func_227888_a_(matrix4f, (float)minX, barY, (float)minZ).func_227885_a_(0.0F, 0.0F, 0.0F, 0.6F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, barY, (float)minZ).func_227885_a_(0.0F, 0.0F, 0.0F, 0.6F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)maxX, barY + barHeight, (float)minZ).func_227885_a_(0.0F, 0.0F, 0.0F, 0.6F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX, barY + barHeight, (float)minZ).func_227885_a_(0.0F, 0.0F, 0.0F, 0.6F).func_181675_d();
                  tessellator.func_78381_a();
                  float hr = healthPercent > 0.5F ? (1.0F - healthPercent) * 2.0F : 1.0F;
                  float hg = healthPercent > 0.5F ? 1.0F : healthPercent * 2.0F;
                  buffer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                  buffer.func_227888_a_(matrix4f, (float)minX, barY, (float)minZ).func_227885_a_(hr, hg, 0.0F, 1.0F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX + barWidth, barY, (float)minZ).func_227885_a_(hr, hg, 0.0F, 1.0F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX + barWidth, barY + barHeight, (float)minZ).func_227885_a_(hr, hg, 0.0F, 1.0F).func_181675_d();
                  buffer.func_227888_a_(matrix4f, (float)minX, barY + barHeight, (float)minZ).func_227885_a_(hr, hg, 0.0F, 1.0F).func_181675_d();
                  tessellator.func_78381_a();
               }

               matrix.func_227865_b_();
            }
         }
      }
   }
}
