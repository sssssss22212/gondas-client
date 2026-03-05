package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolItem;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AutoMiner extends Module {
   // Настройки добычи
   private Setting.Mode target = new Setting.Mode("Target", new String[]{
      "Diamond", "Gold", "Iron", "Coal", "Redstone", "Lapis", "Emerald", 
      "Netherite", "Copper", "Quartz", "All Ores", "Custom"
   });
   private Setting.Double range = new Setting.Double("Range", 50.0D, 10.0D, 100.0D);
   private Setting.Int maxDistance = new Setting.Int("MaxDistance", 100, 20, 500);
   
   // Настройки pathfinding
   private Setting.Boolean autoWalk = new Setting.Boolean("AutoWalk", true);
   private Setting.Boolean autoJump = new Setting.Boolean("AutoJump", true);
   private Setting.Double walkSpeed = new Setting.Double("WalkSpeed", 0.3D, 0.1D, 0.5D);
   private Setting.Int pathfindingRange = new Setting.Int("PathfindingRange", 30, 10, 50);
   private Setting.Boolean avoidLava = new Setting.Boolean("AvoidLava", true);
   private Setting.Boolean avoidWater = new Setting.Boolean("AvoidWater", false);
   private Setting.Boolean avoidCaves = new Setting.Boolean("AvoidCaves", false);
   
   // Настройки добычи
   private Setting.Boolean autoTool = new Setting.Boolean("AutoTool", true);
   private Setting.Double mineSpeed = new Setting.Double("MineSpeed", 1.0D, 0.5D, 2.0D);
   private Setting.Int mineDelay = new Setting.Int("MineDelay", 0, 0, 10);
   private Setting.Boolean rotateToBlock = new Setting.Boolean("RotateToBlock", true);
   private Setting.Double rotationSpeed = new Setting.Double("RotationSpeed", 180.0D, 30.0D, 360.0D);
   
   // Настройки безопасности
   private Setting.Boolean safetyCheck = new Setting.Boolean("SafetyCheck", true);
   private Setting.Boolean checkFall = new Setting.Boolean("CheckFall", true);
   private Setting.Int maxFallDistance = new Setting.Int("MaxFallDistance", 3, 1, 10);
   private Setting.Boolean stopOnDamage = new Setting.Boolean("StopOnDamage", true);
   private Setting.Int healthThreshold = new Setting.Int("HealthThreshold", 6, 1, 20);
   
   // Настройки инвентаря
   private Setting.Boolean autoDrop = new Setting.Boolean("AutoDrop", false);
   private Setting.Boolean keepTools = new Setting.Boolean("KeepTools", true);
   private Setting.Int dropThreshold = new Setting.Int("DropThreshold", 32, 1, 64);
   
   // Продвинутые настройки
   private Setting.Boolean veinMine = new Setting.Boolean("VeinMine", true);
   private Setting.Int veinRange = new Setting.Int("VeinRange", 2, 1, 5);
   private Setting.Boolean patrolMode = new Setting.Boolean("PatrolMode", false);
   private Setting.Int patrolY = new Setting.Int("PatrolY", 11, 1, 256);
   private Setting.Boolean returnHome = new Setting.Boolean("ReturnHome", false);
   
   // Состояние
   private BlockPos currentTarget = null;
   private BlockPos homePos = null;
   private List<BlockPos> path = new ArrayList<>();
   private int pathIndex = 0;
   private int mineTimer = 0;
   private int stuckTimer = 0;
   private BlockPos lastPos = null;
   private long lastMoveTime = 0;
   private Set<BlockPos> minedBlocks = ConcurrentHashMap.newKeySet();
   private int totalMined = 0;
   private float targetYaw = 0;
   private float targetPitch = 0;
   
   // Кэш найденных руд
   private Map<BlockPos, Block> oreCache = new ConcurrentHashMap<>();
   private long lastCacheUpdate = 0;
   
   public AutoMiner() {
      super("AutoMiner", "Baritone-like automatic mining", Module.Category.WORLD);
      this.addSettings(new Setting[]{
         this.target, this.range, this.maxDistance,
         this.autoWalk, this.autoJump, this.walkSpeed, this.pathfindingRange,
         this.avoidLava, this.avoidWater, this.avoidCaves,
         this.autoTool, this.mineSpeed, this.mineDelay, this.rotateToBlock, this.rotationSpeed,
         this.safetyCheck, this.checkFall, this.maxFallDistance, this.stopOnDamage, this.healthThreshold,
         this.autoDrop, this.keepTools, this.dropThreshold,
         this.veinMine, this.veinRange, this.patrolMode, this.patrolY, this.returnHome
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.field_71439_g != null) {
         homePos = mc.field_71439_g.func_233580_cy_();
         lastPos = homePos;
      }
      currentTarget = null;
      path.clear();
      pathIndex = 0;
      mineTimer = 0;
      stuckTimer = 0;
      oreCache.clear();
      minedBlocks.clear();
      totalMined = 0;
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      currentTarget = null;
      path.clear();
      stopMoving();
   }
   
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) return;
      if (mc.field_71439_g == null || mc.field_71441_e == null) return;
      
      // Проверка здоровья
      if (stopOnDamage.getValue() && mc.field_71439_g.func_110143_aJ() <= healthThreshold.getValue()) {
         stopMoving();
         return;
      }
      
      // Обновление кэша руд
      updateOreCache();
      
      // Проверка застревания
      checkStuck();
      
      // Обработка задержки добычи
      if (mineTimer > 0) {
         mineTimer--;
         return;
      }
      
      // Если нет цели - ищем
      if (currentTarget == null) {
         findNextTarget();
      }
      
      // Если всё ещё нет цели - патрулирование
      if (currentTarget == null && patrolMode.getValue()) {
         patrol();
         return;
      }
      
      // Если есть цель - идём к ней или добываем
      if (currentTarget != null) {
         double distToTarget = getDistanceToBlock(currentTarget);
         
         // Если достаточно близко - добываем
         if (distToTarget <= 4.5D) {
            mineBlock(currentTarget);
         } else if (autoWalk.getValue()) {
            // Иначе - идём к цели
            walkToTarget();
         }
      }
      
      // Авто-выбрасывание
      if (autoDrop.getValue()) {
         dropItems();
      }
      
      // Подбираем выпавшие предметы
      collectDrops();
   }
   
   private void updateOreCache() {
      long now = System.currentTimeMillis();
      if (now - lastCacheUpdate < 1000) return; // Обновляем раз в секунду
      lastCacheUpdate = now;
      
      oreCache.clear();
      
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      int r = (int)(range.getValue() / 2);
      
      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = playerPos.func_177982_a(x, y, z);
               Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
               
               if (isTargetOre(block) && !minedBlocks.contains(pos)) {
                  oreCache.put(pos, block);
               }
            }
         }
      }
   }
   
   private void findNextTarget() {
      if (oreCache.isEmpty()) return;
      
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      
      // Сортируем по расстоянию
      List<BlockPos> sortedOres = oreCache.keySet().stream()
         .sorted(Comparator.comparingDouble(pos -> getDistanceToBlock((BlockPos)pos)))
         .collect(Collectors.toList());
      
      for (BlockPos pos : sortedOres) {
         // Проверяем безопасность
         if (safetyCheck.getValue() && !isSafePosition(pos)) {
            continue;
         }
         
         // Проверяем расстояние до дома
         if (returnHome.getValue() && homePos != null) {
            if (getDistanceBetween(homePos, pos) > maxDistance.getValue()) {
               continue;
            }
         }
         
         currentTarget = pos;
         
         // Если vein mining - удаляем соседние блоки той же руды из кэша
         if (veinMine.getValue()) {
            Block oreType = oreCache.get(pos);
            for (BlockPos nearby : getNearbyOres(pos, oreType)) {
               oreCache.remove(nearby);
            }
         }
         
         oreCache.remove(pos);
         return;
      }
   }
   
   private List<BlockPos> getNearbyOres(BlockPos center, Block oreType) {
      List<BlockPos> nearby = new ArrayList<>();
      int vr = veinRange.getValue();
      
      for (int x = -vr; x <= vr; x++) {
         for (int y = -vr; y <= vr; y++) {
            for (int z = -vr; z <= vr; z++) {
               BlockPos pos = center.func_177982_a(x, y, z);
               if (!pos.equals(center)) {
                  Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                  if (block == oreType) {
                     nearby.add(pos);
                  }
               }
            }
         }
      }
      
      return nearby;
   }
   
   private void mineBlock(BlockPos pos) {
      // Авто-инструмент
      if (autoTool.getValue()) {
         selectBestTool(pos);
      }
      
      // Поворот к блоку
      if (rotateToBlock.getValue()) {
         rotateTo(pos);
      }
      
      // Проверяем, нужно ли смотреть на блок
      Direction face = getBestFace(pos);
      
      // Добываем
      mc.field_71442_b.func_180512_c(pos, face);
      
      // Проверяем, сломан ли блок
      if (mc.field_71441_e.func_175623_d(pos)) {
         minedBlocks.add(pos);
         totalMined++;
         
         // Если vein mining - добываем соседние
         if (veinMine.getValue()) {
            Block oreType = oreCache.get(pos);
            if (oreType != null) {
               for (BlockPos nearby : getNearbyOres(pos, oreType)) {
                  if (!minedBlocks.contains(nearby)) {
                     currentTarget = nearby;
                     return;
                  }
               }
            }
         }
         
         currentTarget = null;
         mineTimer = mineDelay.getValue();
      }
   }
   
   private void walkToTarget() {
      if (currentTarget == null) return;
      
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      
      // Простой путь - идём прямо к цели
      Vector3d targetVec = new Vector3d(
         currentTarget.func_177958_n() + 0.5,
         currentTarget.func_177956_o(),
         currentTarget.func_177952_p() + 0.5
      );
      
      Vector3d playerVec = mc.field_71439_g.func_213303_ch();
      Vector3d direction = targetVec.func_178788_d(playerVec).func_72432_a();
      
      // Угол поворота
      double yaw = Math.toDegrees(Math.atan2(-direction.field_72449_c, direction.field_72450_a));
      targetYaw = (float)yaw;
      
      // Поворачиваемся
      smoothRotate();
      
      // Двигаемся
      double speed = walkSpeed.getValue();
      
      // Вперёд
      mc.field_71474_y.field_74351_w.func_151470_d();
      
      // Прыжок если нужно
      if (autoJump.getValue() && needsJump()) {
         mc.field_71439_g.func_70664_aZ();
      }
   }
   
   private void smoothRotate() {
      float currentYaw = mc.field_71439_g.field_70177_z;
      float diff = MathHelper.func_76142_g(targetYaw - currentYaw);
      float speed = (float)(rotationSpeed.getValue() / 20.0D);
      
      if (Math.abs(diff) > speed) {
         mc.field_71439_g.field_70177_z += diff > 0 ? speed : -speed;
      } else {
         mc.field_71439_g.field_70177_z = targetYaw;
      }
   }
   
   private void rotateTo(BlockPos pos) {
      Vector3d playerPos = mc.field_71439_g.func_213303_ch();
      Vector3d blockPos = new Vector3d(
         pos.func_177958_n() + 0.5,
         pos.func_177956_o() + 0.5,
         pos.func_177952_p() + 0.5
      );
      
      Vector3d diff = blockPos.func_178788_d(playerPos);
      
      double yaw = Math.toDegrees(Math.atan2(-diff.field_72449_c, diff.field_72450_a));
      double dist = Math.sqrt(diff.field_72450_a * diff.field_72450_a + diff.field_72449_c * diff.field_72449_c);
      double pitch = Math.toDegrees(Math.atan2(-diff.field_72448_b, dist));
      
      targetYaw = (float)yaw;
      targetPitch = (float)pitch;
      
      smoothRotate();
   }
   
   private boolean needsJump() {
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      BlockPos forward = playerPos.func_177982_a(
         (int)Math.round(Math.sin(Math.toRadians(mc.field_71439_g.field_70177_z + 90))),
         0,
         (int)Math.round(Math.cos(Math.toRadians(mc.field_71439_g.field_70177_z + 90)))
      );
      
      // Проверяем блок впереди на уровне ног
      Block forwardBlock = mc.field_71441_e.func_180495_p(forward).func_177230_c();
      return forwardBlock != Blocks.field_150350_a && forwardBlock.func_235327_a_() < 1.0F;
   }
   
   private void selectBestTool(BlockPos pos) {
      BlockState state = mc.field_71441_e.func_180495_p(pos);
      float bestSpeed = 1.0F;
      int bestSlot = -1;
      
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
         float speed = stack.func_150997_a(state);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = i;
         }
      }
      
      if (bestSlot != -1 && bestSlot != mc.field_71439_g.field_71071_by.field_70461_c) {
         mc.field_71439_g.field_71071_by.field_70461_c = bestSlot;
      }
   }
   
   private Direction getBestFace(BlockPos pos) {
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      
      if (pos.func_177956_o() > playerPos.func_177956_o()) return Direction.UP;
      if (pos.func_177956_o() < playerPos.func_177956_o()) return Direction.DOWN;
      
      int dx = pos.func_177958_n() - playerPos.func_177958_n();
      int dz = pos.func_177952_p() - playerPos.func_177952_p();
      
      if (Math.abs(dx) > Math.abs(dz)) {
         return dx > 0 ? Direction.EAST : Direction.WEST;
      } else {
         return dz > 0 ? Direction.SOUTH : Direction.NORTH;
      }
   }
   
   private boolean isTargetOre(Block block) {
      String mode = target.getValue();
      
      switch (mode) {
         case "Diamond":
            return block == Blocks.field_150482_ag;
         case "Gold":
            return block == Blocks.field_150352_o;
         case "Iron":
            return block == Blocks.field_150366_p;
         case "Coal":
            return block == Blocks.field_150372_cE;
         case "Redstone":
            return block == Blocks.field_150419_aX;
         case "Lapis":
            return block == Blocks.field_150369_r;
         case "Emerald":
            return block == Blocks.field_150365_H;
         case "Copper":
            return block == Blocks.field_235367_bs_;
         case "Netherite":
         case "Quartz":
            return block == Blocks.field_150394_bc || block == Blocks.field_235322_gs_;
         case "All Ores":
            return isOre(block);
         default:
            return false;
      }
   }
   
   private boolean isOre(Block block) {
      return block == Blocks.field_150482_ag ||  // Diamond
             block == Blocks.field_150352_o ||   // Gold
             block == Blocks.field_150366_p ||   // Iron
             block == Blocks.field_150372_cE ||  // Coal
             block == Blocks.field_150419_aX ||  // Redstone
             block == Blocks.field_150369_r ||   // Lapis
             block == Blocks.field_150365_H ||   // Emerald
             block == Blocks.field_150394_bc ||  // Quartz
             block == Blocks.field_235367_bs_ || // Copper
             block == Blocks.field_235322_gs_;   // Ancient Debris
   }
   
   private boolean isSafePosition(BlockPos pos) {
      // Проверка лавы
      if (avoidLava.getValue()) {
         for (Direction dir : Direction.values()) {
            BlockPos check = pos.func_177972_a(dir);
            Block block = mc.field_71441_e.func_180495_p(check).func_177230_c();
            if (block == Blocks.field_150356_k) return false;
         }
      }
      
      // Проверка воды
      if (avoidWater.getValue()) {
         for (Direction dir : Direction.values()) {
            BlockPos check = pos.func_177972_a(dir);
            Block block = mc.field_71441_e.func_180495_p(check).func_177230_c();
            if (block == Blocks.field_150358_i) return false;
         }
      }
      
      // Проверка падения
      if (checkFall.getValue()) {
         int fallDist = 0;
         for (int y = pos.func_177956_o() - 1; y >= pos.func_177956_o() - 10; y--) {
            BlockPos check = new BlockPos(pos.func_177958_n(), y, pos.func_177952_p());
            if (!mc.field_71441_e.func_175623_d(check)) break;
            fallDist++;
         }
         if (fallDist > maxFallDistance.getValue()) return false;
      }
      
      return true;
   }
   
   private void patrol() {
      // Патрулирование на уровне Y
      int targetY = patrolY.getValue();
      BlockPos playerPos = mc.field_71439_g.func_233580_cy_();
      
      // Идём вперёд по уровню
      double yaw = mc.field_71439_g.field_70177_z;
      double rad = Math.toRadians(yaw + 90);
      
      double moveX = Math.sin(rad) * walkSpeed.getValue();
      double moveZ = Math.cos(rad) * walkSpeed.getValue();
      
      // Применяем движение
      mc.field_71439_g.func_213293_j(moveX, 0, moveZ);
      
      // Поворачиваем случайно
      if (mc.field_71439_g.func_70681_au().nextInt(100) < 2) {
         mc.field_71439_g.field_70177_z += mc.field_71439_g.func_70681_au().nextInt(90) - 45;
      }
   }
   
   private void checkStuck() {
      BlockPos currentPos = mc.field_71439_g.func_233580_cy_();
      
      if (lastPos != null && currentPos.equals(lastPos)) {
         stuckTimer++;
         if (stuckTimer > 60) { // 3 секунды
            // Застряли - меняем цель
            currentTarget = null;
            stuckTimer = 0;
            
            // Прыгаем
            if (autoJump.getValue()) {
               mc.field_71439_g.func_70664_aZ();
            }
         }
      } else {
         stuckTimer = 0;
      }
      
      lastPos = currentPos;
   }
   
   private void stopMoving() {
      // Останавливаем движение
      mc.field_71439_g.func_213293_j(0, mc.field_71439_g.func_213322_ci().field_72448_b, 0);
   }
   
   private void dropItems() {
      if (mc.field_71439_g == null) return;
      
      int itemCount = 0;
      for (int i = 9; i < 36; i++) { // Только основной инвентарь
         ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
         if (!stack.func_190926_b()) {
            itemCount += stack.func_190916_E();
         }
      }
      
      if (itemCount >= dropThreshold.getValue()) {
         for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (!stack.func_190926_b() && !shouldKeep(stack)) {
               mc.field_71439_g.func_71019_a(stack, true);
               mc.field_71439_g.field_71071_by.func_70299_a(i, ItemStack.field_190927_a);
            }
         }
      }
   }
   
   private boolean shouldKeep(ItemStack stack) {
      if (keepTools.getValue()) {
         return stack.func_77973_b() instanceof ToolItem ||
                stack.func_77973_b() instanceof PickaxeItem;
      }
      return false;
   }
   
   private void collectDrops() {
      if (mc.field_71441_e == null || mc.field_71439_g == null) return;
      
      for (Entity entity : mc.field_71441_e.func_217416_b()) {
         if (entity instanceof ItemEntity) {
            if (entity.func_70032_d(mc.field_71439_g) < 3.0D) {
               // Подбираем автоматически когда рядом
            }
         }
      }
   }
   
   private double getDistanceToBlock(BlockPos pos) {
      if (mc.field_71439_g == null) return Double.MAX_VALUE;
      return mc.field_71439_g.func_213303_ch().func_186082_d(
         new Vector3d(pos.func_177958_n() + 0.5, pos.func_177956_o(), pos.func_177952_p() + 0.5)
      );
   }
   
   private double getDistanceBetween(BlockPos a, BlockPos b) {
      return Math.sqrt(a.func_177951_i(b));
   }
   
   public int getTotalMined() {
      return totalMined;
   }
   
   public BlockPos getCurrentTarget() {
      return currentTarget;
   }
}
