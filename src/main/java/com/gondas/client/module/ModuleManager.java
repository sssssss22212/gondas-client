package com.gondas.client.module;

import com.gondas.client.module.impl.AimAssist;
import com.gondas.client.module.impl.AirJump;
import com.gondas.client.module.impl.AntiAFK;
import com.gondas.client.module.impl.AntiCobweb;
import com.gondas.client.module.impl.AutoArmor;
import com.gondas.client.module.impl.AutoEat;
import com.gondas.client.module.impl.AutoFarm;
import com.gondas.client.module.impl.AutoFish;
import com.gondas.client.module.impl.AutoLeave;
import com.gondas.client.module.impl.AutoMine;
import com.gondas.client.module.impl.AutoMiner;
import com.gondas.client.module.impl.AutoSeller;
import com.gondas.client.module.impl.AutoSneak;
import com.gondas.client.module.impl.AutoSprint;
import com.gondas.client.module.impl.AutoTool;
import com.gondas.client.module.impl.AutoTotem;
import com.gondas.client.module.impl.AutoWalk;
import com.gondas.client.module.impl.BoatFly;
import com.gondas.client.module.impl.BowAim;
import com.gondas.client.module.impl.Breaker;
import com.gondas.client.module.impl.CameraClip;
import com.gondas.client.module.impl.Chams;
import com.gondas.client.module.impl.ChestESP;
import com.gondas.client.module.impl.ChestStealer;
import com.gondas.client.module.impl.ClickGUIModule;
import com.gondas.client.module.impl.Criticals;
import com.gondas.client.module.impl.Dolphin;
import com.gondas.client.module.impl.ESP;
import com.gondas.client.module.impl.Fly;
import com.gondas.client.module.impl.FreeCam;
import com.gondas.client.module.impl.FullBright;
import com.gondas.client.module.impl.HUD;
import com.gondas.client.module.impl.HighJump;
import com.gondas.client.module.impl.HoleESP;
import com.gondas.client.module.impl.IceSpeed;
import com.gondas.client.module.impl.InventoryManager;
import com.gondas.client.module.impl.ItemESP;
import com.gondas.client.module.impl.Jesus;
import com.gondas.client.module.impl.KillAura;
import com.gondas.client.module.impl.LongJump;
import com.gondas.client.module.impl.MobESP;
import com.gondas.client.module.impl.NoFall;
import com.gondas.client.module.impl.NoRender;
import com.gondas.client.module.impl.NoWeather;
import com.gondas.client.module.impl.Nuker;
import com.gondas.client.module.impl.Parkour;
import com.gondas.client.module.impl.Reach;
import com.gondas.client.module.impl.SafeWalk;
import com.gondas.client.module.impl.Scaffold;
import com.gondas.client.module.impl.Spammer;
import com.gondas.client.module.impl.Speed;
import com.gondas.client.module.impl.Spider;
import com.gondas.client.module.impl.Sprint;
import com.gondas.client.module.impl.Step;
import com.gondas.client.module.impl.StorageESP;
import com.gondas.client.module.impl.TargetHUD;
import com.gondas.client.module.impl.TimeChanger;
import com.gondas.client.module.impl.TimerMod;
import com.gondas.client.module.impl.Tracers;
import com.gondas.client.module.impl.TriggerBot;
import com.gondas.client.module.impl.Velocity;
import com.gondas.client.module.impl.Xray;
import com.gondas.client.module.impl.Zoom;
import com.gondas.client.module.impl.MobileButtons;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
   private static final List<Module> modules = new ArrayList();

   public static void init() {
      modules.add(new KillAura());
      modules.add(new BowAim());
      modules.add(new AutoTotem());
      modules.add(new Velocity());
      modules.add(new Criticals());
      modules.add(new Reach());
      modules.add(new AimAssist());
      modules.add(new TriggerBot());
      modules.add(new AutoArmor());
      modules.add(new Fly());
      modules.add(new Speed());
      modules.add(new NoFall());
      modules.add(new AutoSprint());
      modules.add(new Sprint());
      modules.add(new Step());
      modules.add(new HighJump());
      modules.add(new LongJump());
      modules.add(new Spider());
      modules.add(new Jesus());
      modules.add(new Dolphin());
      modules.add(new BoatFly());
      modules.add(new IceSpeed());
      modules.add(new Parkour());
      modules.add(new AirJump());
      modules.add(new AntiCobweb());
      modules.add(new SafeWalk());
      modules.add(new FullBright());
      modules.add(new ESP());
      modules.add(new Tracers());
      modules.add(new Xray());
      modules.add(new ChestESP());
      modules.add(new ItemESP());
      modules.add(new MobESP());
      modules.add(new StorageESP());
      modules.add(new HoleESP());
      modules.add(new TargetHUD());
      modules.add(new Chams());
      modules.add(new NoRender());
      modules.add(new CameraClip());
      modules.add(new HUD());
      modules.add(new AutoFish());
      modules.add(new AutoFarm());
      modules.add(new Scaffold());
      modules.add(new Nuker());
      modules.add(new AutoMine());
      modules.add(new AutoMiner());
      modules.add(new AutoSeller());
      modules.add(new Breaker());
      modules.add(new AutoEat());
      modules.add(new AutoTool());
      modules.add(new ChestStealer());
      modules.add(new InventoryManager());
      modules.add(new ClickGUIModule());
      modules.add(new Zoom());
      modules.add(new TimerMod());
      modules.add(new Spammer());
      modules.add(new AntiAFK());
      modules.add(new AutoLeave());
      modules.add(new FreeCam());
      modules.add(new NoWeather());
      modules.add(new TimeChanger());
      modules.add(new AutoSneak());
      modules.add(new AutoWalk());
      modules.add(new MobileButtons());
   }

   public static List<Module> getModules() {
      return modules;
   }

   public static List<Module> getByCategory(Module.Category category) {
      return (List)modules.stream().filter((m) -> {
         return m.getCategory() == category;
      }).collect(Collectors.toList());
   }

   public static Module getByKey(int key) {
      return (Module)modules.stream().filter((m) -> {
         return m.getKey() == key;
      }).findFirst().orElse((Object)null);
   }

   public static Module getModuleByName(String name) {
      return (Module)modules.stream().filter((m) -> {
         return m.getName().equalsIgnoreCase(name);
      }).findFirst().orElse((Object)null);
   }
}
