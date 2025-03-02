package cn.bixinian;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoRtp implements ClientModInitializer {
	public static boolean isRunning = false;
	private static final MinecraftClient client = MinecraftClient.getInstance();

	private static Set<String> targetGroups;
	private static Set<String> targetPlayers;
	private static Thread runningThread;  // 用来保存当前执行的线程
	private static boolean isHealing = false;  // 标志玩家是否正在回血

	@Override
	public void onInitializeClient() {
		// 这里注册客户端相关的命令
		CommandManager.registerCommands();
		// 其他初始化代码（如事件监听、配置等）
	}

	public static void startAutoRtp(String biomes, String players) {
		if (isRunning) {
			sendPlayerMessage("自动RTP已经启动，请勿重复开启！");
			return;
		}
		sendPlayerMessage("自动RTP已启动！由Bixinian开发，仅用于学习交流用途，请勿传播本插件！");
		// 调试输出
		sendPlayerMessage("目标地形：" + biomes);
		sendPlayerMessage("目标玩家：" + players);
		isRunning = true;
		targetGroups = biomes == null
			? null
			: Stream.of(biomes.split(",")).collect(Collectors.toSet());
		targetPlayers = players == null
			? null
			: Stream.of(players.split(",")).collect(Collectors.toSet());

		runningThread = new Thread(() -> {
			while (isRunning) {
				try {
					if (client.player != null) {
						// 获取玩家当前坐标
						BlockPos initialPos = client.player.getBlockPos();

						sendPlayerMessage("正在寻找符合条件的地形...");
						client.player.networkHandler.sendChatCommand("rtp");

						long startTime = System.currentTimeMillis();
						long maxWaitTime = 30 * 1000; // 设置最大等待时间为30秒
						boolean hasMoved = false;

						// 等待服务器响应，同时检测玩家坐标是否变化
						while (System.currentTimeMillis() - startTime < maxWaitTime) {
							BlockPos currentPos = client.player.getBlockPos();
							if (!currentPos.equals(initialPos)) {
								hasMoved = true;
								break; // 如果玩家坐标发生变化，跳出循环
							}
							Thread.sleep(500); // 每500ms检查一次位置变化
						}

						// 等待10秒地形加载
						Thread.sleep(10000);

						// 如果在30秒内玩家坐标没有变化，则重新进行循环
						if (!hasMoved) {
							sendPlayerMessage("RTP等待超时，重新执行...");
							continue; // 重新开始当前循环
						}

						// 获取玩家周围7x7范围的地形集合
						Set<String> nearbyBiomes = getNearbyBiomes(client.player.getBlockPos());

						// 取目标地形集合与当前地形集合交集地形
						Set<String> hasBiomes = new HashSet<>();
						if (targetGroups != null) {
							for (String nearbyBiome : nearbyBiomes) {
								if (BiomeChecker.isTargetBiome(nearbyBiome, targetGroups)) {
									hasBiomes.add(nearbyBiome);
								}
							}
						} else {
							hasBiomes = nearbyBiomes;
						}

						// 获取当前玩家所在的地形（仅用于home名称）
						String BiomeName = BiomeChecker.getBiomeNameAtPosition(client.player.getBlockPos());

						// 获取周围遗迹
						String result = StructureDetector.detectStructure();

						sendPlayerMessage("当前周围地形：" + nearbyBiomes);
						sendPlayerMessage(result);

						// 判断玩家周围地形与目标地形集合是否有交集
						if (targetGroups == null || !Collections.disjoint(targetGroups, nearbyBiomes)) {

							// 如果未指定地形，则可以使用重复的home名称，否则使用不重复的名称
							String homeName;
							if (targetGroups == null) {
								homeName = BiomeName.replace("minecraft:", "");  // 可以重复的home名称
							} else {
								// 使用随机两个字符避免重复
								String randomSuffix = generateRandomSuffix();
								homeName = BiomeName.replace("minecraft:", "") + randomSuffix;  // 不重复的home名称
							}

							// 执行设置家命令
							if (homeName.length() > 15) {
								// 删除所有下划线
								homeName = homeName.replace("_", "");

								// 如果删除下划线后字符串长度仍然超过15，则截取长度
								if (homeName.length() > 15) {
									// 删除字符串开头的部分，直到长度不超过15
									homeName = homeName.substring(homeName.length() - 15);
								}
							}
							client.player.networkHandler.sendChatCommand("sethome " + homeName);

							// 如果指定了玩家列表，发送地形信息给玩家
							if (targetPlayers != null) {
								for (String player : targetPlayers) {
									client.player.networkHandler.sendChatCommand("tell " + player + " 找到地形" + String.join(",", hasBiomes) + result);
									Thread.sleep(1000);
									client.player.networkHandler.sendChatCommand("tpahere " + player);
									Thread.sleep(1000);
								}
							}
						}
					}
					Thread.sleep(2 * 60 * 1000); // 每 2 分钟
				} catch (InterruptedException e) {
					// 被中断时，直接退出循环
					isRunning = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		runningThread.start(); // 启动线程

		// 启动血量检查线程
		startHealthCheckThread();
	}

	// 获取玩家周围7x7范围的所有地形
	private static Set<String> getNearbyBiomes(BlockPos playerPos) {
		Set<String> biomes = new HashSet<>();
		int chunkX = playerPos.getX() >> 4;
		int chunkZ = playerPos.getZ() >> 4;
		// 检索方块高度
		int chunkY  = 0;

		// 遍历7x7的区域
		int range = 7;
		for (int x = chunkX - range; x <= chunkX + range; x++) {
			for (int z = chunkZ - range; z <= chunkZ + range; z++) {
				for (int offsetX = 0; offsetX < 16; offsetX += 3) {  // 按3格间隔
					for (int offsetZ = 0; offsetZ < 16; offsetZ += 3) {
						BlockPos pos = new BlockPos((x << 4) + offsetX, chunkY, (z << 4) + offsetZ);
						String biome = BiomeChecker.getCurrentBiomeChineseName(pos);
						biomes.add(biome);
					}
				}
			}
		}
		return biomes;
	}

	public static void stopAutoRtp() {
		if (runningThread != null && runningThread.isAlive()) {
			runningThread.interrupt(); // 中断线程，终止等待
		}
		isRunning = false;
		sendPlayerMessage("自动RTP已关闭！");
	}

	public static void sendPlayerMessage(String message) {
		if (client.player != null) {
			client.player.sendMessage(Text.of(message), false);
		}
	}

	// 生成随机1个字符
	private static String generateRandomSuffix() {
		int leftLimit = 97; // 'a'
		int rightLimit = 122; // 'z'
		int targetStringLength = 2;
		Random random = new Random();

		StringBuilder buffer = new StringBuilder(targetStringLength);
		for (int i = 0; i < targetStringLength; i++) {
			int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
			buffer.append((char) randomLimitedInt);
		}

		return buffer.toString();
	}

	// 血量检查线程
	private static void startHealthCheckThread() {
		new Thread(() -> {
			try {
				checkAndHandlePlayerHealth();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	// 检查并处理玩家血量
	private static void checkAndHandlePlayerHealth() {
		while (isRunning) {
			try {
				Thread.sleep(1000); // 每秒检查一次
				ClientPlayerEntity player = client.player;
				if (player != null) {
					float health = player.getHealth();
					float maxHealth = player.getMaxHealth();

					// 获取玩家当前血量的百分比
					float healthPercentage = (health / maxHealth) * 100;

					// 如果血量低于30%，执行/home zc 并暂停RTP线程
					if (healthPercentage < 30 && !isHealing) {
						sendPlayerMessage("血量低于30%，执行 /home zc 并暂停RTP");
						client.player.networkHandler.sendChatCommand("home zc");
						isHealing = true;  // 设置回血状态
						stopAutoRtp();  // 暂停RTP线程
					}
					// 如果血量恢复至95%，执行/home 1 并继续RTP线程
					if (healthPercentage >= 95 && isHealing) {
						sendPlayerMessage("血量恢复至95%以上，执行 /home 1 并继续RTP");
						client.player.networkHandler.sendChatCommand("home 1");
						isHealing = false;  // 重置回血状态
						startAutoRtp(targetGroups != null ? String.join(",", targetGroups) : null, targetPlayers != null ? String.join(",", targetPlayers) : null);  // 继续RTP线程
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

