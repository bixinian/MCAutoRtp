package cn.bixinian;

import java.util.*;

public class BiomeDictionary {
      // 地形英文名到中文名的映射
      private static final Map<String, String> biomeMap = new HashMap<>();
      // 中文主群系到所有相关地形的映射
      private static final Map<String, Set<String>> biomeGroupMap = new HashMap<>();

      static {
            // 添加所有具体地形和对应的中文名称
            biomeMap.put("minecraft:forest", "森林");
            biomeMap.put("minecraft:birch_forest", "桦木森林");
            biomeMap.put("minecraft:old_growth_birch_forest", "原始桦木森林");
            biomeMap.put("minecraft:dark_forest", "黑森林");
            biomeMap.put("minecraft:plains", "平原");
            biomeMap.put("minecraft:taiga", "针叶林");
            biomeMap.put("minecraft:snowy_taiga", "积雪针叶林");
            biomeMap.put("minecraft:old_growth_pine_taiga", "原始松树针叶林");
            biomeMap.put("minecraft:old_growth_spruce_taiga", "原始云杉针叶林");
            biomeMap.put("minecraft:savanna", "热带草原");
            biomeMap.put("minecraft:savanna_plateau", "热带高原");
            biomeMap.put("minecraft:badlands", "恶地");
            biomeMap.put("minecraft:wooded_badlands", "疏林恶地");
            biomeMap.put("minecraft:eroded_badlands", "风蚀恶地");
            biomeMap.put("minecraft:jungle", "丛林");
            biomeMap.put("minecraft:desert", "沙漠");
            biomeMap.put("minecraft:swamp", "沼泽");
            biomeMap.put("minecraft:snowy_tundra", "雪原");

            // 添加主群系到所有变种群系的归并
            biomeGroupMap.put("桦木森林", Set.of("桦木森林", "原始桦木森林"));
            biomeGroupMap.put("针叶林", Set.of("针叶林", "积雪针叶林", "原始松树针叶林", "原始云杉针叶林"));
            biomeGroupMap.put("热带草原", Set.of("热带草原", "热带高原"));
            biomeGroupMap.put("恶地", Set.of("恶地", "疏林恶地", "风蚀恶地"));
            biomeGroupMap.put("森林", Set.of("森林", "黑森林", "桦木森林", "原始桦木森林"));
            biomeGroupMap.put("沙漠", Set.of("沙漠"));
            biomeGroupMap.put("丛林", Set.of("丛林"));
            biomeGroupMap.put("平原", Set.of("平原"));
            biomeGroupMap.put("雪原", Set.of("雪原"));
            biomeGroupMap.put("沼泽", Set.of("沼泽"));
      }

      /**
       * 根据英文地形获取中文名称。
       * 如果没有对应中文名，则返回英文名，且去除 "minecraft:" 前缀。
       */
      public static String getChineseName(String biome) {
            // 去除 minecraft: 前缀
            String nameWithoutPrefix = biome.replace("minecraft:", "");
            return biomeMap.getOrDefault(biome, nameWithoutPrefix);
      }

      /**
       * 根据中文地形主群系名称，获取对应的所有变种地形。
       */
      public static Set<String> getAllBiomesForGroup(String chineseGroup) {
            return biomeGroupMap.getOrDefault(chineseGroup, Set.of());
      }

      /**
       * 返回所有地形名称。
       */
      public static Set<String> getAllBiomes() {
            return new HashSet<>(biomeMap.keySet());
      }
}
