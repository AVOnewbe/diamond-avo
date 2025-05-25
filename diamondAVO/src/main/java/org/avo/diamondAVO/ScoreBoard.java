package org.avo.diamondAVO;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class ScoreBoard {
    private final DiamondAVO plugin;
    private boolean isEnabled;

    public ScoreBoard(DiamondAVO plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getConfigManager().getConfig().getString("displayMode", "bossbar").equalsIgnoreCase("scoreboard");
        if (isEnabled) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                createScoreboard(player);
            }
        }
    }

    public void enable() {
        isEnabled = true;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            createScoreboard(player);
        }
    }

    public void disable() {
        isEnabled = false;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setScoreboard(plugin.getServer().getScoreboardManager().getNewScoreboard());
        }
    }

    public void updateScoreboard() {
        if (!isEnabled) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    public void createScoreboard(Player player) {
        if (!isEnabled) return;
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        String titleColor = plugin.getConfigManager().getConfig().getString("scoreboard.titleColor", "§b");
        Objective objective = scoreboard.registerNewObjective("diamond", Criteria.DUMMY,
                titleColor + player.getName());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String prefix = plugin.getConfigManager().getConfig().getString("prefix", "Diamond");
        String diamondColor = plugin.getConfigManager().getConfig().getString("scoreboard.diamondColor", "§6");
        String scoreColor = plugin.getConfigManager().getConfig().getString("scoreboard.scoreColor", "§b");
        String diamondText = prefix.isEmpty() ? "" : diamondColor + prefix;
        objective.getScore(diamondText).setScore(2);
        objective.getScore(scoreColor + score + "/" + winScore).setScore(1);

        player.setScoreboard(scoreboard);
    }

    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("diamond");
        if (objective == null) {
            createScoreboard(player);
            return;
        }

        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String prefix = plugin.getConfigManager().getConfig().getString("prefix", "Diamond");
        String diamondColor = plugin.getConfigManager().getConfig().getString("scoreboard.diamondColor", "§6");
        String scoreColor = plugin.getConfigManager().getConfig().getString("scoreboard.scoreColor", "§b");
        String diamondText = prefix.isEmpty() ? "" : diamondColor + prefix;

        // ลบคะแนนเก่า
        for (String entry : scoreboard.getEntries()) {
            if (entry.equals(diamondText)) continue;
            if (entry.equals(scoreColor + (score - 1) + "/" + winScore)) {
                scoreboard.resetScores(entry);
            }
        }

        // อัปเดตคะแนนใหม่
        objective.getScore(diamondText).setScore(2);
        objective.getScore(scoreColor + score + "/" + winScore).setScore(1);
    }
}