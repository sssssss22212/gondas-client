package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoSeller - Автоматическая продажа предметов на аукционе и покупка дешевых товаров
 * Поддержка различных серверов аукционов
 */
public class AutoSeller extends Module {
   // Режим работы
   private Setting.Mode mode = new Setting.Mode("Mode", new String[]{"Sell", "Buy", "Both"});
   private Setting.Boolean autoOpen = new Setting.Boolean("AutoOpen", true);
   private Setting.String auctionCommand = new Setting.String("AuctionCommand", "/ah");
   
   // Настройки продажи
   private Setting.Double sellPrice = new Setting.Double("SellPrice", 1000.0D, 1.0D, 10000000.0D);
   private Setting.Double minSellPrice = new Setting.Double("MinSellPrice", 100.0D, 1.0D, 1000000.0D);
   private Setting.Boolean autoPrice = new Setting.Boolean("AutoPrice", true);
   private Setting.Double priceMultiplier = new Setting.Double("PriceMultiplier", 1.1D, 0.5D, 3.0D);
   private Setting.Int sellDelay = new Setting.Int("SellDelay", 20, 5, 100);
   private Setting.Int maxListings = new Setting.Int("MaxListings", 5, 1, 20);
   
   // Настройки покупки
   private Setting.Double maxBuyPrice = new Setting.Double("MaxBuyPrice", 5000.0D, 1.0D, 10000000.0D);
   private Setting.Int buyDelay = new Setting.Int("BuyDelay", 15, 5, 100);
   private Setting.Boolean snipeMode = new Setting.Boolean("SnipeMode", false);
   private Setting.Int snipeThreshold = new Setting.Int("SnipeThreshold", 50, 10, 100);
   
   // Предметы для торговли
   private Setting.String sellItems = new Setting.String("SellItems", "diamond,gold,iron,emerald,netherite");
   private Setting.String buyItems = new Setting.String("BuyItems", "diamond,emerald,netherite_star,totem");
   
   // Фильтры
   private Setting.Boolean ignoreEnchanted = new Setting.Boolean("IgnoreEnchanted", false);
   private Setting.Boolean onlyStackable = new Setting.Boolean("OnlyStackable", false);
   private Setting.Int minStackSize = new Setting.Int("MinStackSize", 1, 1, 64);
   private Setting.Boolean ignoreRenamed = new Setting.Boolean("IgnoreRenamed", true);
   
   // Продвинутые настройки
   private Setting.Boolean logTransactions = new Setting.Boolean("LogTransactions", true);
   private Setting.Boolean pauseOnFull = new Setting.Boolean("PauseOnFull", true);
   private Setting.Int refreshInterval = new Setting.Int("RefreshInterval", 100, 20, 500);
   
   // Состояние
   private enum State {
      IDLE, OPENING_AUCTION, CHECKING_ITEMS, SELLING, BUYING, WAITING
   }
   
   private State currentState = State.IDLE;
   private int tickCounter = 0;
   private int actionDelay = 0;
   private int listingsCount = 0;
   private long totalProfit = 0;
   private int itemsSold = 0;
   private int itemsBought = 0;
   private long lastRefreshTime = 0;
   
   // Кэш найденных товаров
   private List<AuctionItem> foundItems = new ArrayList<>();
   private Set<String> processedItems = ConcurrentHashMap.newKeySet();
   
   // Паттерны для парсинга цен
   private static final Pattern PRICE_PATTERN = Pattern.compile("([\\d,]+)\\s*(coins|монет|\\$)?", Pattern.CASE_INSENSITIVE);
   private static final Pattern AUCTION_ITEM_PATTERN = Pattern.compile("(.+)\\s*x?(\\d+)?");
   
   // Класс для хранения информации о предмете аукциона
   private static class AuctionItem {
      String name;
      int slotIndex;
      long price;
      int amount;
      boolean isGoodDeal;
      String rawName;
      
      public AuctionItem(String name, int slot, long price, int amount) {
         this.name = name;
         this.slotIndex = slot;
         this.price = price;
         this.amount = amount;
         this.rawName = name;
      }
   }
   
   public AutoSeller() {
      super("AutoSeller", "Auto sell/buy on auction", Module.Category.WORLD);
      this.addSettings(new Setting[]{
         this.mode, this.autoOpen, this.auctionCommand,
         this.sellPrice, this.minSellPrice, this.autoPrice, this.priceMultiplier,
         this.sellDelay, this.maxListings,
         this.maxBuyPrice, this.buyDelay, this.snipeMode, this.snipeThreshold,
         this.sellItems, this.buyItems,
         this.ignoreEnchanted, this.onlyStackable, this.minStackSize, this.ignoreRenamed,
         this.logTransactions, this.pauseOnFull, this.refreshInterval
      });
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      currentState = State.IDLE;
      tickCounter = 0;
      actionDelay = 0;
      foundItems.clear();
      
      // Автоматически открываем аукцион
      if (autoOpen.getValue()) {
         openAuction();
      }
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      currentState = State.IDLE;
   }
   
   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) return;
      if (mc.field_71439_g == null) return;
      
      tickCounter++;
      
      // Обработка задержки
      if (actionDelay > 0) {
         actionDelay--;
         return;
      }
      
      // Проверка интервала обновления
      if (tickCounter % refreshInterval.getValue() == 0) {
         lastRefreshTime = System.currentTimeMillis();
      }
      
      // Основная логика
      switch (currentState) {
         case IDLE:
            handleIdleState();
            break;
         case OPENING_AUCTION:
            // Ждём открытия GUI
            break;
         case CHECKING_ITEMS:
            checkAuctionItems();
            break;
         case SELLING:
            processSelling();
            break;
         case BUYING:
            processBuying();
            break;
         case WAITING:
            if (tickCounter % 100 == 0) {
               currentState = State.IDLE;
            }
            break;
      }
   }
   
   @SubscribeEvent
   public void onGuiOpen(GuiScreenEvent.InitGuiEvent event) {
      if (event.getGui() instanceof ChestScreen) {
         ChestScreen chestScreen = (ChestScreen) event.getGui();
         ITextComponent title = chestScreen.func_213108_m();
         
         if (title != null) {
            String titleStr = title.getString().toLowerCase();
            
            // Определяем тип открытого GUI
            if (titleStr.contains("auction") || titleStr.contains("аукцион") || 
                titleStr.contains("ah") || titleStr.contains("market")) {
               currentState = State.CHECKING_ITEMS;
               actionDelay = 10;
            }
         }
      }
   }
   
   private void handleIdleState() {
      String modeStr = mode.getValue();
      
      if (modeStr.equals("Sell") || modeStr.equals("Both")) {
         // Проверяем есть ли что продать
         if (hasItemsToSell()) {
            currentState = State.SELLING;
            return;
         }
      }
      
      if (modeStr.equals("Buy") || modeStr.equals("Both")) {
         // Проверяем деньги и ищем выгодные предложения
         currentState = State.BUYING;
      }
   }
   
   private void openAuction() {
      if (mc.field_71439_g != null) {
         String cmd = auctionCommand.getValue();
         mc.field_71439_g.func_71165_d(cmd);
         currentState = State.OPENING_AUCTION;
         actionDelay = 20;
      }
   }
   
   private boolean hasItemsToSell() {
      if (mc.field_71439_g == null) return false;
      
      String[] sellItemNames = sellItems.getValue().split(",");
      
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_75139_a(i).func_75211_c();
         if (stack.func_190926_b()) continue;
         
         String itemName = stack.func_200301_q().getString().toLowerCase();
         
         for (String target : sellItemNames) {
            if (itemName.contains(target.trim().toLowerCase())) {
               // Проверяем фильтры
               if (ignoreEnchanted.getValue() && stack.func_77948_v()) continue;
               if (onlyStackable.getValue() && stack.func_190916_E() < minStackSize.getValue()) continue;
               if (ignoreRenamed.getValue() && stack.func_200301_q() != null) continue;
               
               return true;
            }
         }
      }
      
      return false;
   }
   
   private void checkAuctionItems() {
      if (!(mc.field_71462_r instanceof ChestScreen)) return;
      
      ChestScreen chestScreen = (ChestScreen) mc.field_71462_r;
      Container container = chestScreen.func_147002_a();
      
      foundItems.clear();
      
      // Сканируем слоты аукциона
      for (int i = 0; i < container.func_75137_b() - 36; i++) {
         Slot slot = container.func_75139_a(i);
         ItemStack stack = slot.func_75211_c();
         
         if (stack.func_190926_b()) continue;
         
         // Парсим информацию о предмете
         AuctionItem item = parseAuctionItem(stack, i);
         if (item != null) {
            foundItems.add(item);
         }
      }
      
      // Сортируем по выгодности
      foundItems.sort((a, b) -> Long.compare(a.price, b.price));
   }
   
   private AuctionItem parseAuctionItem(ItemStack stack, int slotIndex) {
      String name = stack.func_200301_q().getString();
      long price = extractPriceFromLore(stack);
      int amount = stack.func_190916_E();
      
      if (price <= 0) return null;
      
      AuctionItem item = new AuctionItem(name, slotIndex, price, amount);
      item.isGoodDeal = isGoodDeal(item);
      
      return item;
   }
   
   private long extractPriceFromLore(ItemStack stack) {
      CompoundNBT tag = stack.func_77978_p();
      if (tag == null) return -1;
      
      CompoundNBT display = tag.func_74775_l("display");
      if (!display.func_74764_b("Lore")) return -1;
      
      ListNBT lore = display.func_74761_m("Lore");
      
      for (int i = 0; i < lore.size(); i++) {
         String line = lore.func_193060_a(i).replaceAll("§[0-9a-fk-or]", "");
         Matcher matcher = PRICE_PATTERN.matcher(line);
         
         if (matcher.find()) {
            String priceStr = matcher.group(1).replace(",", "").replace(".", "");
            try {
               return Long.parseLong(priceStr);
            } catch (NumberFormatException e) {
               continue;
            }
         }
      }
      
      return -1;
   }
   
   private boolean isGoodDeal(AuctionItem item) {
      if (item.price > maxBuyPrice.getValue()) return false;
      
      // Проверяем, входит ли предмет в список покупок
      String[] buyItemNames = buyItems.getValue().split(",");
      for (String target : buyItemNames) {
         if (item.name.toLowerCase().contains(target.trim().toLowerCase())) {
            // Проверяем снайп режим
            if (snipeMode.getValue()) {
               double pricePerItem = (double)item.price / item.amount;
               return pricePerItem < snipeThreshold.getValue();
            }
            return true;
         }
      }
      
      return false;
   }
   
   private void processSelling() {
      if (mc.field_71439_g == null) return;
      
      if (listingsCount >= maxListings.getValue()) {
         currentState = State.WAITING;
         return;
      }
      
      String[] sellItemNames = sellItems.getValue().split(",");
      
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_75139_a(i).func_75211_c();
         if (stack.func_190926_b()) continue;
         
         String itemName = stack.func_200301_q().getString().toLowerCase();
         
         for (String target : sellItemNames) {
            if (itemName.contains(target.trim().toLowerCase())) {
               // Проверяем фильтры
               if (ignoreEnchanted.getValue() && stack.func_77948_v()) continue;
               if (onlyStackable.getValue() && stack.func_190916_E() < minStackSize.getValue()) continue;
               
               // Рассчитываем цену
               long price = calculatePrice(stack);
               
               // Выставляем на продажу (симуляция - реальная реализация зависит от сервера)
               sellItem(stack, i, price);
               
               listingsCount++;
               actionDelay = sellDelay.getValue();
               
               if (logTransactions.getValue()) {
                  logTransaction("SELL", stack.func_200301_q().getString(), price);
               }
               
               itemsSold++;
               return;
            }
         }
      }
      
      currentState = State.IDLE;
   }
   
   private long calculatePrice(ItemStack stack) {
      if (!autoPrice.getValue()) {
         return (long)sellPrice.getValue();
      }
      
      // Базовая цена
      double basePrice = sellPrice.getValue();
      
      // Множитель для редких предметов
      String name = stack.func_200301_q().getString().toLowerCase();
      if (name.contains("netherite") || name.contains("незерит")) {
         basePrice *= 5;
      } else if (name.contains("diamond") || name.contains("алмаз")) {
         basePrice *= 2;
      } else if (name.contains("emerald") || name.contains("изумруд")) {
         basePrice *= 1.5;
      }
      
      // Множитель за зачарования
      if (stack.func_77948_v()) {
         basePrice *= 2;
      }
      
      // Учитываем количество
      basePrice *= stack.func_190916_E();
      
      // Применяем множитель пользователя
      basePrice *= priceMultiplier.getValue();
      
      return Math.max((long)basePrice, (long)minSellPrice.getValue());
   }
   
   private void sellItem(ItemStack stack, int slot, long price) {
      // Клик по предмету для продажи
      // Реальная реализация зависит от конкретного сервера
      if (mc.field_71462_r instanceof ChestScreen) {
         // Кликаем по слоту
         mc.field_71442_b.func_241211_a_(slot, 0, net.minecraft.inventory.container.ClickType.PICKUP);
         
         // Устанавливаем цену через команду
         mc.field_71439_g.func_71165_d("/ah sell " + price);
      }
   }
   
   private void processBuying() {
      if (mc.field_71462_r == null || !(mc.field_71462_r instanceof ChestScreen)) {
         // Если GUI не открыт, открываем аукцион
         if (autoOpen.getValue()) {
            openAuction();
         }
         return;
      }
      
      // Ищем выгодные предложения
      for (AuctionItem item : foundItems) {
         if (!item.isGoodDeal) continue;
         if (processedItems.contains(item.rawName)) continue;
         
         // Проверяем баланс (симуляция)
         if (item.price > maxBuyPrice.getValue()) continue;
         
         // Покупаем
         buyItem(item);
         
         processedItems.add(item.rawName);
         actionDelay = buyDelay.getValue();
         
         if (logTransactions.getValue()) {
            logTransaction("BUY", item.name, item.price);
         }
         
         itemsBought++;
         return;
      }
      
      currentState = State.IDLE;
   }
   
   private void buyItem(AuctionItem item) {
      if (mc.field_71462_r instanceof ChestScreen) {
         ChestScreen chestScreen = (ChestScreen) mc.field_71462_r;
         
         // Кликаем по слоту
         mc.field_71442_b.func_241211_a_(item.slotIndex, 0, net.minecraft.inventory.container.ClickType.PICKUP);
         
         // Подтверждаем покупку
         actionDelay = 10;
         mc.field_71442_b.func_241211_a_(item.slotIndex, 0, net.minecraft.inventory.container.ClickType.PICKUP);
      }
   }
   
   private void logTransaction(String type, String item, long price) {
      String message = String.format("[AutoSeller] %s: %s for %d coins", type, item, price);
      if (mc.field_71439_g != null) {
         mc.field_71439_g.func_145747_a(new StringTextComponent(message), mc.field_71439_g.func_145748_c_());
      }
   }
   
   public int getItemsSold() {
      return itemsSold;
   }
   
   public int getItemsBought() {
      return itemsBought;
   }
   
   public long getTotalProfit() {
      return totalProfit;
   }
   
   public State getCurrentState() {
      return currentState;
   }
}
