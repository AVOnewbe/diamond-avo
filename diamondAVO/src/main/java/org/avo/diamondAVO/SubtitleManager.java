package org.avo.diamondAVO;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class SubtitleManager {
    private final DiamondAVO plugin;
    private boolean isEnabled;
    private boolean isCountingDown;
    private int countdownSeconds;
    private BukkitRunnable countdownTask;
    private BukkitRunnable subtitleTask;
    private final Random random = new Random();
    private boolean isPausedForMessage = false;

    public SubtitleManager(DiamondAVO plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getConfigManager().getConfig().getString("displayMode", "bossbar").equalsIgnoreCase("subtitle");
        this.isCountingDown = false;
        this.countdownSeconds = 0;
        if (isEnabled) {
            startSubtitleTask();
        }
    }

    public void enable() {
        isEnabled = true;
        startSubtitleTask();
    }

    public void disable() {
        isEnabled = false;
        stopCountdown();
        clearSubtitle();
        if (subtitleTask != null) {
            subtitleTask.cancel();
            subtitleTask = null;
        }
    }

    public void updateSubtitle() {
        if (!isEnabled || isCountingDown || isPausedForMessage) return;
        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String color = plugin.getConfigManager().getConfig().getString("subtitle.color", "§6");
        String prefix = plugin.getConfigManager().getConfig().getString("prefix", "Diamond");
        String subtitle = prefix.isEmpty() ? color + score + "/" + winScore : color + prefix + " " + score + "/" + winScore;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("", subtitle, 0, 70, 0);
        }
    }

    public void startCountdown(int seconds) {
        plugin.getLogger().info("SubtitleManager: เริ่มนับถอยหลัง " + seconds + " วินาที");
        isCountingDown = true;
        countdownSeconds = seconds;
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        clearSubtitle();
        countdownTask = new BukkitRunnable() {
            final String[] colors = {"§a", "§b", "§e", "§d", "§9"};
            ArrayList<String> availableColors = new ArrayList<>(Arrays.asList(colors));
            String currentColor = availableColors.get(random.nextInt(availableColors.size()));
            int colorChangeCounter = 0;

            @Override
            public void run() {
                if (!isCountingDown || isPausedForMessage) return;

                if (countdownSeconds <= 0) {
                    plugin.getLogger().info("SubtitleManager: นับถอยหลังครบแล้ว เรียก celebrate()");
                    new AvoCommand(plugin).celebrate();
                    stopCountdown();
                    clearTitle();
                    return;
                }

                // เปลี่ยนสีทุก 5 วินาที (รันทุก 1 วินาที ดังนั้น 5 วินาที = 5 รอบ)
                String color;
                if (countdownSeconds <= 3) {
                    color = "§c"; // 3, 2, 1 เป็นสีแดง
                } else {
                    if (colorChangeCounter % 5 == 0) { // ทุก 5 วินาที
                        availableColors.remove(currentColor);
                        if (availableColors.isEmpty()) {
                            availableColors.addAll(Arrays.asList(colors));
                            availableColors.remove(currentColor);
                        }
                        currentColor = availableColors.get(random.nextInt(availableColors.size()));
                    }
                    color = currentColor;
                    colorChangeCounter++;
                }

                String title = color + countdownSeconds;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendTitle(title, "", 0, 25, 0);
                    player.playSound(player.getLocation(), "minecraft:item.trident.return", 1.0f, 1.0f);
                }

                countdownSeconds--;
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void pauseForMessage() {
        isPausedForMessage = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                isPausedForMessage = false;
                if (isCountingDown) {
                    // กลับไปแสดงการนับถอยหลัง
                    String color = countdownSeconds <= 3 ? "§c" : "§e"; // ปรับสีตามสถานะ
                    String title = color + countdownSeconds;
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.sendTitle(title, "", 0, 25, 0);
                    }
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    public void stopCountdown() {
        plugin.getLogger().info("SubtitleManager: หยุดการนับถอยหลัง");
        isCountingDown = false;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        clearTitle();
        if (isEnabled) {
            updateSubtitle();
        }
    }

    private void startSubtitleTask() {
        if (subtitleTask != null) {
            subtitleTask.cancel();
        }
        subtitleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled || isCountingDown || isPausedForMessage) {
                    if (!isEnabled) cancel();
                    return;
                }
                updateSubtitle();
            }
        };
        subtitleTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void clearSubtitle() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("", "", 0, 0, 0);
        }
    }

    public void clearTitle() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("", "", 0, 0, 0);
        }
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }
}