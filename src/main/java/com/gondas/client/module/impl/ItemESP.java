package com.gondas.client.module.impl;

import com.gondas.client.module.Module;
import com.gondas.client.setting.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ItemESP extends Module {
   private Setting.Double range = new Setting.Double("Range", 50.0D, 10.0D, 100.0D);
   private Setting.Boolean names = new Setting.Boolean("Names", true);
   private Setting.Mode language = new Setting.Mode("Language", new String[]{"Russian", "English", "Both"});
   private Setting.Boolean showCount = new Setting.Boolean("ShowCount", true);
   private Setting.Double lineWidth = new Setting.Double("LineWidth", 2.0D, 1.0D, 5.0D);
   private Setting.Boolean filled = new Setting.Boolean("Filled", true);
   private Setting.Boolean showIcon = new Setting.Boolean("ShowIcon", true);
   private Setting.Double nameScale = new Setting.Double("NameScale", 1.0D, 0.5D, 2.0D);
   private Setting.Boolean showDurability = new Setting.Boolean("ShowDurability", true);
   private Setting.Boolean showEnchants = new Setting.Boolean("ShowEnchants", true);
   private Setting.Boolean showValue = new Setting.Boolean("ShowValue", false);
   
   // Полный словарь переводов
   private static final Map<String, String> TRANSLATIONS = new HashMap<>();
   
   static {
      // Материалы
      TRANSLATIONS.put("diamond", "Алмаз");
      TRANSLATIONS.put("gold", "Золото");
      TRANSLATIONS.put("iron", "Железо");
      TRANSLATIONS.put("emerald", "Изумруд");
      TRANSLATIONS.put("netherite", "Незерит");
      TRANSLATIONS.put("stone", "Камень");
      TRANSLATIONS.put("wood", "Дерево");
      TRANSLATIONS.put("wooden", "Деревянный");
      TRANSLATIONS.put("cobblestone", "Булыжник");
      TRANSLATIONS.put("cobble", "Булыжник");
      TRANSLATIONS.put("coal", "Уголь");
      TRANSLATIONS.put("charcoal", "Древесный уголь");
      TRANSLATIONS.put("redstone", "Редстоун");
      TRANSLATIONS.put("lapis", "Лазурит");
      TRANSLATIONS.put("quartz", "Кварц");
      TRANSLATIONS.put("copper", "Медь");
      TRANSLATIONS.put("amethyst", "Аметист");
      TRANSLATIONS.put("ancient debris", "Древние обломки");
      
      // Инструменты
      TRANSLATIONS.put("sword", "Меч");
      TRANSLATIONS.put("pickaxe", "Кирка");
      TRANSLATIONS.put("axe", "Топор");
      TRANSLATIONS.put("shovel", "Лопата");
      TRANSLATIONS.put("hoe", "Мотыга");
      TRANSLATIONS.put("shears", "Ножницы");
      TRANSLATIONS.put("flint and steel", "Огниво");
      
      // Броня
      TRANSLATIONS.put("helmet", "Шлем");
      TRANSLATIONS.put("chestplate", "Нагрудник");
      TRANSLATIONS.put("leggings", "Поножи");
      TRANSLATIONS.put("boots", "Ботинки");
      TRANSLATIONS.put("shield", "Щит");
      
      // Еда
      TRANSLATIONS.put("apple", "Яблоко");
      TRANSLATIONS.put("golden apple", "Золотое яблоко");
      TRANSLATIONS.put("enchanted golden apple", "Зачарованное золотое яблоко");
      TRANSLATIONS.put("bread", "Хлеб");
      TRANSLATIONS.put("porkchop", "Свинина");
      TRANSLATIONS.put("beef", "Говядина");
      TRANSLATIONS.put("chicken", "Курица");
      TRANSLATIONS.put("mutton", "Баранина");
      TRANSLATIONS.put("rabbit", "Кролик");
      TRANSLATIONS.put("fish", "Рыба");
      TRANSLATIONS.put("salmon", "Лосось");
      TRANSLATIONS.put("cod", "Треска");
      TRANSLATIONS.put("tropical fish", "Тропическая рыба");
      TRANSLATIONS.put("pufferfish", "Рыба-фугу");
      TRANSLATIONS.put("potato", "Картофель");
      TRANSLATIONS.put("baked potato", "Печёный картофель");
      TRANSLATIONS.put("carrot", "Морковь");
      TRANSLATIONS.put("golden carrot", "Золотая морковь");
      TRANSLATIONS.put("melon", "Арбуз");
      TRANSLATIONS.put("berry", "Ягода");
      TRANSLATIONS.put("sweet berries", "Сладкие ягоды");
      TRANSLATIONS.put("glow berries", "Светящиеся ягоды");
      
      // Зелья и расходники
      TRANSLATIONS.put("potion", "Зелье");
      TRANSLATIONS.put("splash potion", "Взрывное зелье");
      TRANSLATIONS.put("lingering potion", "Оседающее зелье");
      TRANSLATIONS.put("arrow", "Стрела");
      TRANSLATIONS.put("spectral arrow", "Призрачная стрела");
      TRANSLATIONS.put("tipped arrow", "Отравленная стрела");
      TRANSLATIONS.put("ender pearl", "Эндер-жемчуг");
      TRANSLATIONS.put("eye of ender", "Око Эндера");
      TRANSLATIONS.put("blaze rod", "Стержень ифрита");
      TRANSLATIONS.put("blaze powder", "Порошок ифрита");
      TRANSLATIONS.put("bone", "Кость");
      TRANSLATIONS.put("bone meal", "Костная мука");
      TRANSLATIONS.put("gunpowder", "Порох");
      TRANSLATIONS.put("string", "Нить");
      TRANSLATIONS.put("feather", "Перо");
      TRANSLATIONS.put("leather", "Кожа");
      TRANSLATIONS.put("slimeball", "Слизь");
      TRANSLATIONS.put("egg", "Яйцо");
      TRANSLATIONS.put("snowball", "Снежок");
      
      // Блоки и ресурсы
      TRANSLATIONS.put("log", "Бревно");
      TRANSLATIONS.put("planks", "Доски");
      TRANSLATIONS.put("stick", "Палка");
      TRANSLATIONS.put("crafting table", "Верстак");
      TRANSLATIONS.put("furnace", "Печь");
      TRANSLATIONS.put("blast furnace", "Плавильная печь");
      TRANSLATIONS.put("smoker", "Коптильня");
      TRANSLATIONS.put("chest", "Сундук");
      TRANSLATIONS.put("ender chest", "Эндер-сундук");
      TRANSLATIONS.put("shulker box", "Шалкеровый ящик");
      TRANSLATIONS.put("barrel", "Бочка");
      TRANSLATIONS.put("bed", "Кровать");
      TRANSLATIONS.put("book", "Книга");
      TRANSLATIONS.put("enchanted book", "Зачарованная книга");
      TRANSLATIONS.put("paper", "Бумага");
      TRANSLATIONS.put("bookshelf", "Книжная полка");
      
      // Редкие предметы
      TRANSLATIONS.put("nether star", "Звезда Ада");
      TRANSLATIONS.put("beacon", "Маяк");
      TRANSLATIONS.put("conduit", "Кондуктор");
      TRANSLATIONS.put("elytra", "Элитры");
      TRANSLATIONS.put("trident", "Трезубец");
      TRANSLATIONS.put("totem", "Тотем");
      TRANSLATIONS.put("totem of undying", "Тотем бессмертия");
      TRANSLATIONS.put("enchanted golden apple", "Зачарованное золотое яблоко");
      TRANSLATIONS.put("dragon egg", "Яйцо дракона");
      TRANSLATIONS.put("dragon head", "Голова дракона");
      TRANSLATIONS.put("beacon", "Маяк");
      TRANSLATIONS.put("wither rose", "Роза иссушителя");
      TRANSLATIONS.put("wither skeleton skull", "Череп скелета-иссушителя");
      TRANSLATIONS.put("skeleton skull", "Череп скелета");
      TRANSLATIONS.put("zombie head", "Голова зомби");
      TRANSLATIONS.put("creeper head", "Голова крипера");
      
      // Зачарования
      TRANSLATIONS.put("enchanted", "Зачарованный");
      TRANSLATIONS.put("enchantment", "Зачарование");
      
      // Редкость
      TRANSLATIONS.put("rare", "Редкий");
      TRANSLATIONS.put("epic", "Эпический");
      TRANSLATIONS.put("legendary", "Легендарный");
      TRANSLATIONS.put("common", "Обычный");
      TRANSLATIONS.put("uncommon", "Необычный");
      
      // Разное
      TRANSLATIONS.put("bucket", "Ведро");
      TRANSLATIONS.put("water bucket", "Ведро воды");
      TRANSLATIONS.put("lava bucket", "Ведро лавы");
      TRANSLATIONS.put("milk bucket", "Ведро молока");
      TRANSLATIONS.put("powder snow bucket", "Ведро рыхлого снега");
      TRANSLATIONS.put("compass", "Компас");
      TRANSLATIONS.put("clock", "Часы");
      TRANSLATIONS.put("map", "Карта");
      TRANSLATIONS.put("spyglass", "Подзорная труба");
      TRANSLATIONS.put("recovery compass", "Компас восстановления");
      
      // Вёдра с мобами
      TRANSLATIONS.put("cod bucket", "Ведро с треской");
      TRANSLATIONS.put("salmon bucket", "Ведро с лососем");
      TRANSLATIONS.put("tropical fish bucket", "Ведро с тропической рыбой");
      TRANSLATIONS.put("pufferfish bucket", "Ведро с рыбой-фугу");
      TRANSLATIONS.put("axolotl bucket", "Ведро с аксолотлем");
      TRANSLATIONS.put("tadpole bucket", "Ведро с головастиком");
      
      // Спавнеры
      TRANSLATIONS.put("spawn", "Яйцо призыва");
      TRANSLATIONS.put("spawn egg", "Яйцо призыва");
   }

   public ItemESP() {
      super("ItemESP", "Highlight dropped items with names", Module.Category.RENDER);
      this.addSettings(new Setting[]{
         this.range, this.names, this.language, this.showCount, 
         this.lineWidth, this.filled, this.showIcon, this.nameScale,
         this.showDurability, this.showEnchants, this.showValue
      });
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (mc.field_71441_e == null || mc.field_71439_g == null) return;
      
      MatrixStack matrix = event.getMatrixStack();
      Vector3d camPos = mc.field_71460_t.func_215316_n().func_216785_c();
      
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableLighting();
      RenderSystem.disableTexture();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.lineWidth((float)this.lineWidth.getValue());
      
      for (Entity e : mc.field_71441_e.func_217416_b()) {
         if (!(e instanceof ItemEntity)) continue;
         
         double dist = mc.field_71439_g.func_70032_d(e);
         if (dist > this.range.getValue()) continue;
         
         ItemEntity item = (ItemEntity)e;
         ItemStack stack = item.func_92059_d();
         
         // Определяем цвет
         float[] colors = getItemColor(stack);
         float r = colors[0];
         float g = colors[1];
         float b = colors[2];
         
         double x = e.func_226277_ct_() - camPos.field_72450_a;
         double y = e.func_226278_cu_() - camPos.field_72448_b;
         double z = e.func_226281_cx_() - camPos.field_72449_c;
         
         matrix.func_227860_a_();
         Matrix4f matrix4f = matrix.func_227866_c_().func_227870_a_();
         Tessellator tessellator = Tessellator.func_178181_a();
         BufferBuilder buffer = tessellator.func_178180_c();
         
         // Рисуем заполненный бокс
         if (this.filled.getValue()) {
            buffer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
            float alpha = 0.35F;
            buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
            tessellator.func_78381_a();
         }

         // Рисуем линии
         buffer.func_181668_a(1, DefaultVertexFormats.field_181706_f);
         float alpha = 1.0F;
         
         // Нижняя грань
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         
         // Верхняя грань
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         
         // Вертикальные линии
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z - 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x + 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         buffer.func_227888_a_(matrix4f, (float)x - 0.25F, (float)y + 0.5F, (float)z + 0.25F).func_227885_a_(r, g, b, alpha).func_181675_d();
         
         tessellator.func_78381_a();
         matrix.func_227865_b_();
      }
      
      // Рендерим названия
      if (this.names.getValue()) {
         this.renderNames(matrix, camPos);
      }

      RenderSystem.enableDepthTest();
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }
   
   private float[] getItemColor(ItemStack stack) {
      String itemName = stack.func_200301_q().getString().toLowerCase();
      
      // Алмаз - голубой
      if (itemName.contains("diamond") || itemName.contains("алмаз")) {
         return new float[]{0.0F, 0.8F, 1.0F};
      }
      // Золото - жёлтый
      if (itemName.contains("gold") || itemName.contains("золот")) {
         return new float[]{1.0F, 0.85F, 0.0F};
      }
      // Изумруд - зелёный
      if (itemName.contains("emerald") || itemName.contains("изумруд")) {
         return new float[]{0.0F, 1.0F, 0.4F};
      }
      // Незерит - тёмно-красный
      if (itemName.contains("netherite") || itemName.contains("незерит")) {
         return new float[]{0.5F, 0.3F, 0.3F};
      }
      // Железо - серебристый
      if (itemName.contains("iron") || itemName.contains("желез")) {
         return new float[]{0.9F, 0.9F, 0.9F};
      }
      // Зачарованные - фиолетовый
      if (stack.func_77948_v() || itemName.contains("enchanted") || itemName.contains("зачарован")) {
         return new float[]{0.8F, 0.0F, 1.0F};
      }
      // Редкие предметы - оранжевый
      if (itemName.contains("rare") || itemName.contains("редк")) {
         return new float[]{1.0F, 0.5F, 0.0F};
      }
      // Тотем - жёлтый
      if (itemName.contains("totem") || itemName.contains("тотем")) {
         return new float[]{1.0F, 0.9F, 0.0F};
      }
      // Элитры - голубой
      if (itemName.contains("elytra") || itemName.contains("элитр")) {
         return new float[]{0.3F, 0.7F, 1.0F};
      }
      // Звезда Ада - жёлтый
      if (itemName.contains("nether star") || itemName.contains("звезда ада")) {
         return new float[]{1.0F, 1.0F, 0.5F};
      }
      // Эндер - фиолетовый
      if (itemName.contains("ender") || itemName.contains("эндер")) {
         return new float[]{0.3F, 0.0F, 0.5F};
      }
      
      // По умолчанию - жёлтый
      return new float[]{1.0F, 1.0F, 0.0F};
   }

   private void renderNames(MatrixStack matrix, Vector3d camPos) {
      for (Entity e : mc.field_71441_e.func_217416_b()) {
         if (!(e instanceof ItemEntity)) continue;
         
         double dist = mc.field_71439_g.func_70032_d(e);
         if (dist > this.range.getValue()) continue;
         
         ItemEntity item = (ItemEntity)e;
         ItemStack stack = item.func_92059_d();
         String name = getDisplayName(stack, dist);
         
         double x = e.func_226277_ct_() - camPos.field_72450_a;
         double y = e.func_226278_cu_() - camPos.field_72448_b + 0.7D;
         double z = e.func_226281_cx_() - camPos.field_72449_c;
         
         this.renderBillboardText(matrix, name, x, y, z, (float)this.nameScale.getValue());
      }
   }
   
   private String getDisplayName(ItemStack stack, double distance) {
      String originalName = stack.func_200301_q().getString();
      String name = originalName;
      String lang = this.language.getValue();
      
      if (lang.equals("Russian") || lang.equals("Both")) {
         name = translateToRussian(originalName);
      }
      
      // Добавляем количество
      if (this.showCount.getValue() && stack.func_190916_E() > 1) {
         name = name + " x" + stack.func_190916_E();
      }
      
      // Добавляем прочность
      if (this.showDurability.getValue() && stack.func_77984_f()) {
         int maxDur = stack.func_77958_k();
         int dur = maxDur - stack.func_77952_i();
         name = name + " [" + dur + "/" + maxDur + "]";
      }
      
      // Добавляем дистанцию
      name = name + " (" + String.format("%.1f", distance) + "m)";
      
      return name;
   }

   private void renderBillboardText(MatrixStack matrix, String text, double x, double y, double z, float scale) {
      matrix.func_227860_a_();
      matrix.func_227861_a_(x, y, z);
      matrix.func_227863_a_(mc.field_71460_t.func_215316_n().func_227995_f_());
      matrix.func_227862_a_(-scale * 0.025F, -scale * 0.025F, scale * 0.025F);
      RenderSystem.enableTexture();
      int width = mc.field_71466_p.func_78256_a(text);
      mc.field_71466_p.func_238421_b_(matrix, text, (float)(-width) / 2.0F, 0.0F, -1);
      matrix.func_227865_b_();
   }

   private String translateToRussian(String name) {
      String lower = name.toLowerCase();
      String result = name;
      
      // Сортируем переводы по длине (сначала длинные фразы)
      String[] sortedKeys = TRANSLATIONS.keySet().stream()
         .sorted((a, b) -> b.length() - a.length())
         .toArray(String[]::new);
      
      for (String key : sortedKeys) {
         if (lower.contains(key)) {
            String translation = TRANSLATIONS.get(key);
            // Заменяем с сохранением регистра первого символа
            result = result.replaceAll("(?i)" + key, translation);
         }
      }
      
      return result;
   }
}
