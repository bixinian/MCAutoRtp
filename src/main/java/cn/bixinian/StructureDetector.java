package cn.bixinian;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.chunk.Chunk;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.*;
import java.util.*;

import static cn.bixinian.BiomeChecker.getBiomeNameAtPosition;

public class StructureDetector {

      private static final MinecraftClient client = MinecraftClient.getInstance();

      // 遗迹字典（包括遗迹名称、对应地形和对应方块）
      private static final Map<String, StructureInfo> structureDictionary = new HashMap<>();

      static {
            // 示例：添加不同类型的遗迹信息
            structureDictionary.put("鬼武士", new StructureInfo("针叶林", Blocks.COBBLED_DEEPSLATE_SLAB));
            structureDictionary.put("幽魂", new StructureInfo("桦木森林", Blocks.PRISMARINE));
            structureDictionary.put("十字军", new StructureInfo("黑森林", Blocks.CYAN_TERRACOTTA));
            structureDictionary.put("十字军-雪原", new StructureInfo("积雪针叶林", Blocks.CYAN_TERRACOTTA));
            structureDictionary.put("迅猛龙王", new StructureInfo("恶地", Blocks.GRANITE));
            structureDictionary.put("感染掠夺者", new StructureInfo("热带高原", Blocks.SCULK_CATALYST));
            structureDictionary.put("奴役铁傀儡", new StructureInfo("热带草原", Blocks.POLISHED_ANDESITE));
            structureDictionary.put("木偶机师", new StructureInfo("热带草原", Blocks.POLISHED_GRANITE_STAIRS));
            // 更多遗迹可以在此添加
      }

      /**
       * 检测玩家周围区块中是否存在特定结构
       * 该方法通过检查玩家所在区块及其周围区块来确定是否存在特定的遗迹结构
       *
       * @return 如果发现特定结构，则返回结构的描述信息；如果没有发现，则返回提示信息
       */
      public static String detectStructure() {
            BlockPos playerPos = client.player.getBlockPos();
            int chunkX = playerPos.getX() >> 4;  // 当前区块坐标
            int chunkZ = playerPos.getZ() >> 4;

            // 设置检查范围（例如3x3个区块）
            int range = 7;

            // 创建一个线程池，最多使用多少个线程
            ExecutorService executorService = Executors.newFixedThreadPool(64);  // 可根据需要调整线程池的大小
            List<Callable<String>> tasks = new ArrayList<>();

            // 遍历所有检查区块，并为每个区块创建任务
            for (int x = chunkX - range; x <= chunkX + range; x++) {
                  for (int z = chunkZ - range; z <= chunkZ + range; z++) {
                        final int finalX = x;
                        final int finalZ = z;
                        tasks.add(() -> {
                              Chunk chunk = client.world.getChunk(finalX, finalZ);
                              return checkForStructureInChunk(chunk);
                        });
                  }
            }

            try {
                  // 执行所有任务，并等待结果
                  List<Future<String>> results = executorService.invokeAll(tasks);

                  // 遍历每个任务的结果
                  for (Future<String> result : results) {
                        String structureResult = result.get();
                        if (structureResult != null) {
                              // 如果发现遗迹，返回匹配信息
                              return structureResult;
                        }
                  }
            } catch (InterruptedException | ExecutionException e) {
                  e.printStackTrace();
            } finally {
                  // 关闭线程池
                  executorService.shutdown();
            }

            // 如果没有找到遗迹，返回没有发现的信息
            return "周围没有发现遗迹";
      }

      /**
       * 检查指定区块是否包含遗迹方块
       * @param chunk 区块
       * @return 匹配的遗迹信息，若无匹配则返回 null
       */
      private static String checkForStructureInChunk(Chunk chunk) {
            if (client.world == null) return null;

            int chunkPosX = chunk.getPos().x; // 区块的 X 坐标（世界坐标中的区块位置）
            int chunkPosZ = chunk.getPos().z; // 区块的 Z 坐标


            // 遍历区块中的每个位置
            for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                        int topY = chunk.getTopY(); // 区块的顶部高度
                        int startHeight = Math.min(200, topY); // 从高度200或地形高度开始向下查找
                        int nonAirBlockCount = 0; // 连续非空气方块计数

                        for (int y = startHeight; y >= chunk.getBottomY(); y--) {
                              BlockPos pos = new BlockPos(chunkPosX * 16 + x, y, chunkPosZ * 16 + z); // 计算世界坐标
                              Block block = chunk.getBlockState(pos).getBlock();

                              // 如果不是空气方块
                              if (!block.getDefaultState().isAir()) {
                                    nonAirBlockCount++;
                                    if (nonAirBlockCount >= 4) {
                                          break; // 结束当前Y轴的检查
                                    }
                              } else {
                                    nonAirBlockCount = 0; // 重置计数
                              }

                              // 获取当前地形的英文名称
                              String biomeName = getBiomeNameAtPosition(pos);

                              // 遍历遗迹字典，查找匹配的方块及其对应的地形
                              for (Map.Entry<String, StructureInfo> entry : structureDictionary.entrySet()) {
                                    StructureInfo structureInfo = entry.getValue();

                                    // 获取遗迹字典中地形的所有变种地形（包括目标地形和变种）
                                    Set<String> relatedBiomes = BiomeDictionary.getAllBiomesForGroup(structureInfo.getBiome());
                                    // 如果遗迹地形并不是BiomeDictionary类集合而是单一地形或其组成的集合
                                    if (relatedBiomes == null || relatedBiomes.isEmpty()) {
                                          relatedBiomes = Stream.of(structureInfo.getBiome().split(",")).collect(Collectors.toSet());
                                    }

                                    if (structureInfo.getBlock().equals(block) && BiomeChecker.isTargetBiome(biomeName, relatedBiomes)) {
                                          // 返回遗迹的匹配信息
                                          return "发现疑似 " + entry.getKey() + " 遗迹！位于坐标：" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                                    }
                              }
                        }
                  }
            }

            return null; // 如果没有发现遗迹，返回 null
      }

      /**
       * 判断区块中的方块是否属于某个遗迹
       * @param block 方块
       * @return 是否是遗迹方块
       */
      private static boolean isStructureBlock(Block block) {
            // 遍历所有遗迹字典中的方块，如果当前方块匹配，则为遗迹方块
            for (StructureInfo structure : structureDictionary.values()) {
                  if (structure.getBlock().equals(block)) {
                        return true;
                  }
            }
            return false;
      }

      /**
       * 根据方块获取对应的遗迹名称
       * @param block 方块
       * @return 遗迹名称
       */
      private static String getStructureNameByBlock(Block block) {
            for (Map.Entry<String, StructureInfo> entry : structureDictionary.entrySet()) {
                  if (entry.getValue().getBlock().equals(block)) {
                        return entry.getKey();
                  }
            }
            return null;
      }

      /**
       * 遗迹信息类，包含遗迹名称、对应地形和方块
       */
      private static class StructureInfo {
            private final String biome;  // 对应地形
            private final Block block;   // 对应方块

            public StructureInfo(String biome, Block block) {
                  this.biome = biome;
                  this.block = block;
            }

            public String getBiome() {
                  return biome;
            }

            public Block getBlock() {
                  return block;
            }
      }
}
