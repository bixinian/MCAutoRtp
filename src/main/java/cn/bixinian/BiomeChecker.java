package cn.bixinian;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import java.util.Set;

import static cn.bixinian.BiomeDictionary.getAllBiomesForGroup;
import static cn.bixinian.BiomeDictionary.getChineseName;

public class BiomeChecker {
      private static final MinecraftClient client = MinecraftClient.getInstance();

      /**
       * 获取指定位置的地形名称
       */
      public static String getBiomeNameAtPosition(BlockPos pos) {
            if (client.player != null) {
                  RegistryEntry<Biome> biomeEntry = client.player.getWorld().getBiome(pos);
                  return biomeEntry.getKey().map(key -> key.getValue().toString()).orElse("unknown");
            }
            return "unknown";
      }

      /**
       * 获取坐标位置所处地形的中文名称。
       * 如果没有对应的中文名，则返回英文名称。
       */
      public static String getCurrentBiomeChineseName(BlockPos pos) {
            String englishName = getBiomeNameAtPosition(pos);
            return getChineseName(englishName);
      }

      /**
       * 检查是否是目标地形（支持主群系名称和单一地形名称）。
       */
      public static boolean isTargetBiome(String currentBiome, Set<String> targetGroups) {
            String chineseName = getChineseName(currentBiome); // 获取地形的中文名称
            String englishName = currentBiome.replace("minecraft:", ""); // 去掉 "minecraft:" 前缀
            // 匹配地形组
            for (String group : targetGroups) {
                  if (getAllBiomesForGroup(group).contains(chineseName)) {
                        return true;
                  }
            }
            // 直接匹配地形名称（中英文）
            if (targetGroups.contains(chineseName) || targetGroups.contains(englishName)) {
                  return true;
            }
            return false;
      }

}