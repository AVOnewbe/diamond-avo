package org.avo.diamondAVO;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossBarManager {
    private final DiamondAVO plugin;
    private BossBar bossBar;
    private boolean isEnabled;

    public BossBarManager(DiamondAVO plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar("§6Diamond 0/60", BarColor.GREEN, BarStyle.SEGMENTED_12);
        this.isEnabled = plugin.getConfigManager().getConfig().getString("displayMode", "bossbar").equalsIgnoreCase("bossbar");
        if (isEnabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        }
        updateBossBar();
    }

    public void enable() {
        isEnabled = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
        updateBossBar();
    }

    public void disable() {
        isEnabled = false;
        bossBar.removeAll();
    }

    public void updateBossBar() {
        if (!isEnabled) return;
        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String color = plugin.getConfigManager().getConfig().getString("bossbar.color", "§6");
        String prefix = plugin.getConfigManager().getConfig().getString("prefix", "Diamond");
        String title = prefix.isEmpty() ? color + score + "/" + winScore : color + prefix + " " + score + "/" + winScore;
        bossBar.setTitle(title);
        double progress = Math.max(0, Math.min(1.0, (double) score / winScore));
        bossBar.setProgress(progress);
        bossBar.setColor(score < 0 ? BarColor.RED : BarColor.GREEN);
    }

    public void addPlayer(Player player) {
        if (isEnabled) {
            bossBar.addPlayer(player);
        }
    }

    public void removePlayer(Player player) {
        bossBar.removePlayer(player);
    }
}