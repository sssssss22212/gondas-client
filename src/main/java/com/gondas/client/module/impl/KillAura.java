package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.network.play.client.CPlayerPacket.RotationPacket;
import net.minecraft.network.play.client.CPlayerPacket.PositionPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

public class KillAura extends Module {
   private Setting.Double range = new Setting.Double("Range", 3.5D, 1.0D, 6.0D);
   private Setting.Double wallsRange = new Setting.Double("WallsRange", 3.0D, 1.0D, 6.0D);
   private Setting.Boolean players = new Setting.Boolean("Players", true);
   private Setting.Boolean mobs = new Setting.Boolean("Mobs", false);
   private Setting.Boolean animals = new Setting.Boolean("Animals", false);
   private Setting.Boolean invisible = new Setting.Boolean("Invisible", true);
   private Setting.Boolean walls = new Setting.Boolean("Walls", false);
   private Setting.Double minCps = new Setting.Double("MinCPS", 8.0D, 1.0D, 20.0D);
   private Setting.Double maxCps = new Setting.Double("MaxCPS", 12.0D, 1.0D, 20.0D);
   private Setting.Boolean swing = new Setting.Boolean("Swing", true);
   private Setting.Boolean keepSprint = new Setting.Boolean("KeepSprint", true);
   private Setting.Boolean onlyWeapon = new Setting.Boolean("OnlyWeapon", true);
   private Setting.Mode rotation = new Setting.Mode("Rotation", new String[]{"Silent", "None", "Normal"});
   private Setting.Mode targetMode = new Setting.Mode("Target", new String[]{"Closest", "Health", "Angle", "Distance"});
   private Setting.Double rotSpeed = new Setting.Double("RotSpeed", 120.0D, 1.0D, 180.0D);
   private Setting.Boolean smoothRot = new Setting.Boolean("SmoothRot", true);
   private Setting.Double jitter = new Setting.Double("Jitter", 2.0D, 0.0D, 5.0D);
   private Setting.Double aimNoise = new Setting.Double("AimNoise", 1.0D, 0.0D, 3.0D);
   private Setting.Mode acMode = new Setting.Mode("ACMode", new String[]{"NCP", "Vanilla", "AAC", "Matrix", "Grim", "Sparky"});
   private Setting.Boolean fovCheck = new Setting.Boolean("FOVCheck", false);
   private Setting.Double maxFov = new Setting.Double("MaxFOV", 180.0D, 30.0D, 360.0D);
   private Setting.Boolean autoBlock = new Setting.Boolean("AutoBlock", false);
   private Setting.Boolean smartDelay = new Setting.Boolean("SmartDelay", true);
   private Setting.Boolean attackInAir = new Setting.Boolean("AttackInAir", true);
   private Setting.Double attackChance = new Setting.Double("AttackChance", 100.0D, 0.0D, 100.0D);
   private Setting.Boolean randomPause = new Setting.Boolean("RandomPause", true);
   private Setting.Boolean hypixelMode = new Setting.Boolean("HypixelMode", false);
   
   private Random random = new Random();
   private int attackCooldown = 0;
   private float targetYaw;
   private float targetPitch;
   private float currentYaw;
   private float currentPitch;
   private LivingEntity target = null;
   private int pauseTimer = 0;
   private int blockTimer = 0;
   private boolean isBlocking = false;
   private long lastAttackTime = 0;
   private double lastTargetHealth = 0;
   private int switchTargetCooldown = 0;
   
   public KillAura() {
      super("KillAura", "Smart auto attack with anticheat bypass", Module.Category.COMBAT);
      this.addSettings(new Setting[]{
         this.range, this.wallsRange, this.players, this.mobs, this.animals, 
         this.invisible, this.walls, this.minCps, this.maxCps, this.swing, 
         this.keepSprint, this.onlyWeapon, this.rotation, this.targetMode, 
         this.rotSpeed, this.smoothRot, this.jitter, this.aimNoise, this.acMode, 
         this.fovCheck, this.maxFov, this.autoBlock, this.smartDelay, 
         this.attackInAir, this.attackChance, this.randomPause, this.hypixelMode
      });
   }

   public void onEnable() {
      super.onEnable();
      if (mc.field_71439_g != null) {
         this.currentYaw = mc.field_71439_g.field_70177_z;
         this.currentPitch = mc.field_71439_g.field_70125_A;
         this.targetYaw = this.currentYaw;
         this.targetPitch = this.currentPitch;
      }
      this.target = null;
      this.pauseTimer = 0;
      this.blockTimer = 0;
      this.isBlocking = false;
   }

   public void onDisable() {
      super.onDisable();
      this.target = null;
      this.stopBlocking();
   }

   public void onTick() {
      if (mc.field_71439_g == null || mc.field_71441_e == null) return;
      
      // Случайные паузы для обхода античитов
      if (this.randomPause.getValue() && this.pauseTimer > 0) {
         this.pauseTimer--;
         updateRotationsSmooth();
         return;
      }
      
      // Расчет CPS с вариациями
      double baseCps = this.minCps.getValue() + this.random.nextDouble() * (this.maxCps.getValue() - this.minCps.getValue());
      
      // Добавляем случайные вариации CPS
      if (this.random.nextInt(100) < 20) {
         baseCps += this.random.nextDouble() * 2.0D - 1.0D;
      }
      
      int cooldownMax = Math.max(1, (int)(20.0D / baseCps));
      
      if (this.attackCooldown > 0) {
         this.attackCooldown--;
         if (this.target != null && !this.rotation.getValue().equals("None")) {
            this.updateRotationsSmooth();
         }
         return;
      }
      
      if (this.onlyWeapon.getValue() && !this.isHoldingWeapon()) {
         this.target = null;
         this.stopBlocking();
         return;
      }
      
      // Уменьшаем кулдаун смены цели
      if (this.switchTargetCooldown > 0) {
         this.switchTargetCooldown--;
      }
      
      // Поиск цели
      LivingEntity newTarget = this.findTarget();
      
      // Умная смена цели - не переключаемся слишком часто
      if (newTarget != null && this.target != null && newTarget != this.target) {
         if (this.switchTargetCooldown > 0) {
            newTarget = this.target; // Остаемся на текущей цели
         } else {
            this.switchTargetCooldown = 10 + this.random.nextInt(10);
         }
      }
      
      this.target = newTarget;
      
      if (this.target == null) {
         this.stopBlocking();
         if (this.rotation.getValue().equals("Silent") && mc.field_71439_g != null) {
            this.targetYaw = mc.field_71439_g.field_70177_z;
            this.targetPitch = mc.field_71439_g.field_70125_A;
            this.updateRotationsSmooth();
         }
         return;
      }
      
      // AutoBlock
      if (this.autoBlock.getValue()) {
         this.startBlocking();
      }
      
      // Получаем ротации с предсказанием движения
      float[] targetRotations = this.getSmartRotations(this.target);
      
      // Добавляем шум
      if (this.aimNoise.getValue() > 0.0D) {
         targetRotations[0] += (this.random.nextFloat() - 0.5F) * (float)this.aimNoise.getValue();
         targetRotations[1] += (this.random.nextFloat() - 0.5F) * (float)this.aimNoise.getValue() * 0.5F;
         targetRotations[1] = MathHelper.func_76131_a(targetRotations[1], -90.0F, 90.0F);
      }

      this.targetYaw = targetRotations[0];
      this.targetPitch = targetRotations[1];
      
      if (!this.rotation.getValue().equals("None")) {
         this.updateRotationsSmooth();
      }

      double dist = (double)mc.field_71439_g.func_70032_d(this.target);
      double maxRange = this.canSeeTarget(this.target) ? this.range.getValue() : this.wallsRange.getValue();
      
      if (!this.walls.getValue() && !this.canSeeTarget(this.target)) {
         return;
      }
      
      if (dist > maxRange) {
         return;
      }
      
      // Шанс атаки
      if (this.attackChance.getValue() < 100.0D && this.random.nextDouble() * 100.0D > this.attackChance.getValue()) {
         return;
      }
      
      // Проверка атаки в воздухе
      if (!this.attackInAir.getValue() && !mc.field_71439_g.func_233570_aj_()) {
         return;
      }
      
      // Умная задержка - учитываем кулдаун оружия
      if (this.smartDelay.getValue()) {
         float attackSpeed = 4.0F; // Базовая скорость атаки
         if (mc.field_71439_g.func_184614_ca().func_77973_b() instanceof ToolItem) {
            // Можно добавить проверку на зачарования
         }
         
         // Проверяем кулдаун атаки
         float cooldownProgress = mc.field_71439_g.func_184811_cU().func_185143_a(mc.field_71439_g.func_184614_ca().func_77973_b());
         if (cooldownProgress < 0.9F) {
            return;
         }
      }
      
      // Hypixel mode - специальные паузы
      if (this.hypixelMode.getValue() && this.random.nextInt(100) < 10) {
         this.pauseTimer = this.random.nextInt(5) + 3;
         return;
      }
      
      // Атака
      this.attack(this.target);
      this.attackCooldown = Math.max(1, cooldownMax + this.random.nextInt(3) - 1);
      
      // Запоминаем здоровье цели
      this.lastTargetHealth = this.target.func_110143_aJ();
      this.lastAttackTime = System.currentTimeMillis();
      
      // Случайная пауза
      if (this.randomPause.getValue() && this.random.nextInt(100) < 15) {
         this.pauseTimer = this.random.nextInt(3) + 1;
      }
   }

   private void updateRotationsSmooth() {
      if (mc.field_71439_g == null) return;
      
      float speed = (float)this.rotSpeed.getValue();
      float gcd = (float)(mc.field_71474_y.field_74341_c * 0.6000000238418579D + 0.20000000298023224D);
      gcd = gcd * gcd * gcd * 8.0F;
      
      float yawDiff = MathHelper.func_76142_g(this.targetYaw - this.currentYaw);
      float pitchDiff = MathHelper.func_76142_g(this.targetPitch - this.currentPitch);
      
      // Определяем скорость в зависимости от дистанции до цели
      float dist = Math.abs(yawDiff) + Math.abs(pitchDiff);
      
      float yawChange = MathHelper.func_76131_a(yawDiff, -speed, speed);
      float pitchChange = MathHelper.func_76131_a(pitchDiff, -speed, speed);
      
      // Плавные повороты
      if (this.smoothRot.getValue()) {
         if (dist < 30.0F) {
            float factor = dist / 30.0F;
            yawChange *= 0.4F + factor * 0.6F;
            pitchChange *= 0.4F + factor * 0.6F;
         }
         
         // Еще более плавно при близком прицеливании
         if (dist < 10.0F) {
            yawChange *= 0.5F;
            pitchChange *= 0.5F;
         }
      }

      // Jitter для обхода античитов
      if (this.jitter.getValue() > 0.0D && this.random.nextInt(10) < 4) {
         yawChange += (this.random.nextFloat() - 0.5F) * (float)this.jitter.getValue();
         pitchChange += (this.random.nextFloat() - 0.5F) * (float)this.jitter.getValue() * 0.3F;
      }

      // GCD fix для обхода античитов
      yawChange = (float)Math.round(yawChange / gcd) * gcd;
      pitchChange = (float)Math.round(pitchChange / gcd) * gcd;
      
      this.currentYaw += yawChange;
      this.currentPitch += pitchChange;
      this.currentPitch = MathHelper.func_76131_a(this.currentPitch, -90.0F, 90.0F);
      
      if (this.rotation.getValue().equals("Silent")) {
         mc.field_71439_g.field_71174_a.func_147297_a(new RotationPacket(this.currentYaw, this.currentPitch, mc.field_71439_g.func_233570_aj_()));
      } else {
         mc.field_71439_g.field_70177_z = this.currentYaw;
         mc.field_71439_g.field_70125_A = this.currentPitch;
      }
   }

   private LivingEntity findTarget() {
      List<LivingEntity> possibleTargets = new CopyOnWriteArrayList<>();
      
      for (Entity e : mc.field_71441_e.func_217416_b()) {
         if (!(e instanceof LivingEntity)) continue;
         if (e == mc.field_71439_g) continue;
         
         LivingEntity living = (LivingEntity)e;
         
         // Проверка типа сущности
         if (living instanceof PlayerEntity && !this.players.getValue()) continue;
         if (living instanceof MonsterEntity && !this.mobs.getValue()) continue;
         if (living instanceof AnimalEntity && !this.animals.getValue()) continue;
         
         // Проверка невидимости
         if (living.func_82150_aj() && !this.invisible.getValue()) continue;
         
         // Проверка здоровья
         if (living.func_110143_aJ() <= 0.0F) continue;
         
         // Проверка дистанции
         double dist = mc.field_71439_g.func_70032_d(living);
         double maxRange = this.canSeeTarget(living) ? this.range.getValue() : this.wallsRange.getValue();
         
         if (!this.walls.getValue() && !this.canSeeTarget(living)) continue;
         if (dist > maxRange || dist <= 0.0D) continue;
         
         // Проверка FOV
         if (this.fovCheck.getValue()) {
            float[] rot = this.getRotations(living);
            float yawDiff = Math.abs(MathHelper.func_76142_g(rot[0] - this.currentYaw));
            if (yawDiff > this.maxFov.getValue() / 2.0D) continue;
         }
         
         possibleTargets.add(living);
      }

      if (possibleTargets.isEmpty()) return null;

      // Сортировка по выбранному режиму
      String mode = this.targetMode.getValue();
      Comparator<LivingEntity> comparator;
      
      switch (mode) {
         case "Health":
            comparator = Comparator.comparingDouble(LivingEntity::func_110143_aJ);
            break;
         case "Angle":
            comparator = Comparator.comparingDouble(this::getAngleDiff);
            break;
         case "Distance":
            comparator = Comparator.comparingDouble(e -> mc.field_71439_g.func_70032_d(e));
            break;
         default: // Closest
            comparator = Comparator.comparingDouble(e -> mc.field_71439_g.func_70032_d(e));
      }
      
      possibleTargets.sort(comparator);
      return possibleTargets.get(0);
   }

   private boolean canSeeTarget(LivingEntity entity) {
      return mc.field_71439_g.func_70685_l(entity);
   }

   private double getAngleDiff(LivingEntity entity) {
      float[] rot = this.getRotations(entity);
      return Math.abs(MathHelper.func_76142_g(rot[0] - this.currentYaw)) + Math.abs(MathHelper.func_76142_g(rot[1] - this.currentPitch));
   }

   private float[] getRotations(LivingEntity targetEntity) {
      double x = targetEntity.func_226277_ct_() - mc.field_71439_g.func_226277_ct_();
      double y = targetEntity.func_226280_cw_() - mc.field_71439_g.func_226280_cw_();
      double z = targetEntity.func_226281_cx_() - mc.field_71439_g.func_226281_cx_();
      double dist = Math.sqrt(x * x + z * z);
      float yaw = (float)(Math.toDegrees(Math.atan2(z, x)) - 90.0D);
      float pitch = (float)(-Math.toDegrees(Math.atan2(y, dist)));
      return new float[]{yaw, MathHelper.func_76131_a(pitch, -90.0F, 90.0F)};
   }
   
   private float[] getSmartRotations(LivingEntity targetEntity) {
      // Предсказываем позицию цели
      Vector3d targetMotion = targetEntity.func_213322_ci();
      double predictX = targetEntity.func_226277_ct_() + targetMotion.field_72450_a * 0.5D;
      double predictY = targetEntity.func_226280_cw_() + targetMotion.field_72448_b * 0.5D;
      double predictZ = targetEntity.func_226281_cx_() + targetMotion.field_72449_c * 0.5D;
      
      // Целимся в центр хитбокса с небольшим разбросом
      double targetHeight = targetEntity.func_213302_cg() * (0.3D + random.nextDouble() * 0.4D);
      
      double x = predictX - mc.field_71439_g.func_226277_ct_();
      double y = (predictY - targetHeight) - mc.field_71439_g.func_226280_cw_();
      double z = predictZ - mc.field_71439_g.func_226281_cx_();
      
      double dist = Math.sqrt(x * x + z * z);
      float yaw = (float)(Math.toDegrees(Math.atan2(z, x)) - 90.0D);
      float pitch = (float)(-Math.toDegrees(Math.atan2(y, dist)));
      
      return new float[]{yaw, MathHelper.func_76131_a(pitch, -90.0F, 90.0F)};
   }

   private void attack(LivingEntity targetEntity) {
      if (this.swing.getValue()) {
         mc.field_71439_g.func_184609_a(Hand.MAIN_HAND);
      }

      boolean wasSprinting = mc.field_71439_g.func_70051_ag();
      
      // Снимаем спринт для критического удара
      if (wasSprinting && !this.keepSprint.getValue()) {
         mc.field_71439_g.func_70031_b(false);
      }

      // Наносим удар
      mc.field_71442_b.func_78764_a(mc.field_71439_g, targetEntity);
      
      // Восстанавливаем спринт
      if (this.keepSprint.getValue() && wasSprinting) {
         mc.field_71439_g.func_70031_b(true);
      }
   }
   
   private void startBlocking() {
      if (!this.isBlocking && mc.field_71439_g.func_184614_ca().func_77973_b() instanceof SwordItem) {
         mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, Hand.MAIN_HAND);
         this.isBlocking = true;
      }
   }
   
   private void stopBlocking() {
      if (this.isBlocking) {
         mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, Hand.MAIN_HAND);
         this.isBlocking = false;
      }
   }

   private boolean isHoldingWeapon() {
      return mc.field_71439_g.func_184614_ca().func_77973_b() instanceof SwordItem || 
             mc.field_71439_g.func_184614_ca().func_77973_b() instanceof AxeItem;
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public float getCurrentYaw() {
      return this.currentYaw;
   }

   public float getCurrentPitch() {
      return this.currentPitch;
   }
}
