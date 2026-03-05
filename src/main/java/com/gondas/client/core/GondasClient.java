package com.gondas.client.core;

import com.gondas.client.event.ClientEventHandler;
import com.gondas.client.module.ModuleManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("gondasclient")
public class GondasClient {
   public static final Logger LOGGER = LogManager.getLogger();
   public static final String MOD_ID = "gondasclient";
   public static GondasClient INSTANCE;

   public GondasClient() {
      INSTANCE = this;
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
      MinecraftForge.EVENT_BUS.register(this);
      MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
   }

   private void clientSetup(FMLClientSetupEvent event) {
      ModuleManager.init();
      KeyBindingHandler.register();
      LOGGER.info("Gondas Client loaded successfully!");
      LOGGER.info("Press RIGHT SHIFT to open ClickGUI");
   }
}
