package cn.bixinian;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;

import static cn.bixinian.AutoRtp.sendPlayerMessage;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CommandManager {
      private static final MinecraftClient client = MinecraftClient.getInstance();

      public static void registerCommands() {
            // 注册命令
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                  // 注册 start_run 命令
                  dispatcher.register(literal("start_run")
                          .then(argument("biomes", StringArgumentType.string())  // 允许多个地形，以逗号分隔，并且是可选的
                                  .suggests((context, builder) -> {
                                        // 获取所有已知的地形并提供补全建议（包括中英文）
                                        BiomeDictionary.getAllBiomes().forEach(biome -> {
                                              String biomeName = biome.replace("minecraft:", "");  // 去掉 minecraft:
                                              String biomeChineseName = BiomeDictionary.getChineseName(biome);

                                              // 英文名和中文名同时作为补全选项
                                              builder.suggest(biomeName);
                                              builder.suggest(biomeChineseName);
                                        });
                                        return builder.buildFuture();
                                  })
                                  .then(argument("players", StringArgumentType.string())
                                          .suggests((context, builder) -> {
                                                // 获取所有在线玩家并提供补全建议
                                                client.getNetworkHandler().getPlayerList().forEach(player ->
                                                        builder.suggest(player.getProfile().getName()));  // 使用 String 类型进行补全
                                                return builder.buildFuture();
                                          })
                                          .executes(context -> {
                                                String biomes = StringArgumentType.getString(context, "biomes");
                                                String players = StringArgumentType.getString(context, "players");
                                                // 判断 biomes 和 players 是否为空字符串
                                                if (biomes.isEmpty()) {
                                                      biomes = null;
                                                }
                                                if (players.isEmpty()) {
                                                      players = null;
                                                }
                                                AutoRtp.startAutoRtp(biomes, players);  // 启动自动RTP
                                                return 1;
                                          })))
                          .executes(context -> {
                                String biomes = StringArgumentType.getString(context, "biomes");
                                // 判断 biomes 是否为空字符串
                                if (biomes.isEmpty()) {
                                      biomes = null; // 默认地形
                                }
                                AutoRtp.startAutoRtp(biomes, null);  // 启动自动RTP，没有指定玩家
                                return 1;
                          }));

                  // 注册 stop_run 命令
                  dispatcher.register(literal("stop_run").executes(context -> {
                        AutoRtp.stopAutoRtp();  // 停止自动RTP
                        return 1;
                  }));

                  // 注册 find_structure 命令
                  dispatcher.register(literal("find_structure")
                          .executes(context -> {
                                // 执行遗迹检测
                                String result = StructureDetector.detectStructure();
                                sendPlayerMessage(result);
                                return 1;
                          }));
            });
      }
}
