package com.gondas.client.setting;

import java.util.Arrays;
import java.util.List;

public abstract class Setting {
   protected String name;

   public String getName() {
      return this.name;
   }

   public static class Str extends Setting {
      private String value;

      public Str(String name, String value) {
         this.name = name;
         this.value = value;
      }

      public String getValue() {
         return this.value;
      }

      public void setValue(String value) {
         this.value = value;
      }
   }

   public static class Mode extends Setting {
      private String value;
      private List<String> modes;

      public Mode(String name, String... modes) {
         this.name = name;
         this.modes = Arrays.asList(modes);
         this.value = modes[0];
      }

      public String getValue() {
         return this.value;
      }

      public void setValue(String value) {
         if (this.modes.contains(value)) {
            this.value = value;
         }
      }

      public List<String> getModes() {
         return this.modes;
      }
   }

   public static class Double extends Setting {
      private double value;
      private double min;
      private double max;

      public Double(String name, double value, double min, double max) {
         this.name = name;
         this.value = value;
         this.min = min;
         this.max = max;
      }

      public double getValue() {
         return this.value;
      }

      public void setValue(double value) {
         this.value = Math.max(this.min, Math.min(this.max, value));
      }

      public double getMin() {
         return this.min;
      }

      public double getMax() {
         return this.max;
      }
   }

   public static class Int extends Setting {
      private int value;
      private int min;
      private int max;

      public Int(String name, int value, int min, int max) {
         this.name = name;
         this.value = value;
         this.min = min;
         this.max = max;
      }

      public int getValue() {
         return this.value;
      }

      public void setValue(int value) {
         this.value = Math.max(this.min, Math.min(this.max, value));
      }

      public int getMin() {
         return this.min;
      }

      public int getMax() {
         return this.max;
      }
   }

   public static class Boolean extends Setting {
      private boolean value;

      public Boolean(String name, boolean value) {
         this.name = name;
         this.value = value;
      }

      public boolean getValue() {
         return this.value;
      }

      public void setValue(boolean value) {
         this.value = value;
      }
   }
}
