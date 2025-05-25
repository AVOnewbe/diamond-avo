package org.avo.diamondAVO;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {
    private final DiamondAVO plugin;
    private boolean isEnabled;

    public ActionBarManager(DiamondAVO plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getConfigManager().getConfig().getString("displayMode", "off").equalsIgnoreCase("actionbar");
        if (isEnabled) {
            startActionBarTask();
        }
    }

    public void enable() {
        isEnabled = true;
        startActionBarTask();
    }

    public void disable() {
        isEnabled = false;
        clearActionBar();
    }

    public void updateActionBar() {
        if (!isEnabled) return;
        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String color = plugin.getConfigManager().getConfig().getString("actionbar.color", "§6");
        String prefix = plugin.getConfigManager().getConfig().getString("prefix", "Diamond");
        String message = prefix.isEmpty() ? color + score + "/" + winScore : color + prefix + " " + score + "/" + winScore;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a actionbar \"" + message + "\"");
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled) {
                    cancel();
                    return;
                }
                updateActionBar();
            }
        }.runTaskTimer(plugin, 0L, 20L); // อัปเดตทุก 1 วินาที
    }

    private void clearActionBar() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title @a actionbar \"\"");
    }
}