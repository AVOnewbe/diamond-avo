package org.avo.diamondAVO;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.Material;
import java.util.Arrays;

public class ScoreManager {
    private final DiamondAVO plugin;
    private int score;
    private int winScore;

    public ScoreManager(DiamondAVO plugin) {
        this.plugin = plugin;
        this.score = plugin.getConfigManager().getScoreConfig().getInt("score", 0);
        this.winScore = plugin.getConfigManager().getScoreConfig().getInt("winScore", 60);
    }

    public int getScore() {
        return score;
    }

    public int getWinScore() {
        return winScore;
    }

    public void setScore(int newScore) {
        this.score = newScore;
        plugin.getConfigManager().getScoreConfig().set("score", score);
        plugin.getConfigManager().saveScoreConfig();
        plugin.getBossBarManager().updateBossBar();
        plugin.getSubtitleManager().updateSubtitle();
        plugin.getScoreBoard().updateScoreboard();
        plugin.getActionBarManager().updateActionBar();
        plugin.getOverlayManager().updateOverlay();
        checkWinCondition();
    }

    public void setWinScore(int newWinScore) {
        this.winScore = newWinScore;
        plugin.getConfigManager().getScoreConfig().set("winScore", winScore);
        plugin.getConfigManager().saveScoreConfig();
        plugin.getBossBarManager().updateBossBar();
        plugin.getSubtitleManager().updateSubtitle();
        plugin.getScoreBoard().updateScoreboard();
        plugin.getActionBarManager().updateActionBar();
        plugin.getOverlayManager().updateOverlay();
        checkWinCondition();
    }

    public void addScore(int amount) {
        score += amount;
        setScore(score);
    }

    public void removeScore(int amount) {
        int diamondsToRemove = amount;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int playerDiamonds = countDiamonds(player.getInventory(), player.getOpenInventory().getTopInventory());
            if (diamondsToRemove > 0 && playerDiamonds > 0) {
                int remove = Math.min(playerDiamonds, diamondsToRemove);
                removeDiamonds(player.getInventory(), remove);
                diamondsToRemove -= remove;
            }
        }
        score -= amount;
        setScore(score);
    }

    public void resetScore() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeDiamonds(player.getInventory(), countDiamonds(player.getInventory(), player.getOpenInventory().getTopInventory()));
        }
        score = 0;
        plugin.getConfigManager().getScoreConfig().set("score", score);
        plugin.getConfigManager().saveScoreConfig();
        plugin.getBossBarManager().updateBossBar();
        plugin.getSubtitleManager().updateSubtitle();
        plugin.getScoreBoard().updateScoreboard();
        plugin.getActionBarManager().updateActionBar();
        plugin.getOverlayManager().updateOverlay();
    }

    public void distributeDiamonds(int amount) {
        String priorityPlayer = plugin.getConfigManager().getConfig().getString("priorityPlayer", "AVOxSSR");
        Player target = plugin.getServer().getPlayer(priorityPlayer);
        if (target == null || !target.isOnline()) {
            Player[] players = plugin.getServer().getOnlinePlayers().toArray(new Player[0]);
            if (players.length > 0) {
                target = players[(int) (Math.random() * players.length)];
            }
        }
        if (target != null) {
            // แจกเป็น Diamond Block ถ้าจำนวนมากกว่า 9
            int blocks = amount / 9;
            int diamonds = amount % 9;
            if (blocks > 0) {
                target.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK, blocks));
            }
            if (diamonds > 0) {
                target.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
            }
        }
    }

    public int countDiamonds(Inventory inventory, Inventory craftingInventory) {
        int count = 0;

        // นับเพชรในคลัง
        count += Arrays.stream(inventory.getContents())
                .filter(item -> item != null && item.getType() == Material.DIAMOND)
                .mapToInt(ItemStack::getAmount)
                .sum();

        // นับ Diamond Block ในคลัง (1 บล็อก = 9 เพชร)
        count += Arrays.stream(inventory.getContents())
                .filter(item -> item != null && item.getType() == Material.DIAMOND_BLOCK)
                .mapToInt(item -> item.getAmount() * 9)
                .sum();

        // นับเพชรในช่องคราฟติ้ง
        if (craftingInventory instanceof CraftingInventory) {
            CraftingInventory craftingInv = (CraftingInventory) craftingInventory;
            ItemStack[] matrix = craftingInv.getMatrix();

            // นับเฉพาะเพชรในช่องคราฟติ้ง
            count += Arrays.stream(matrix)
                    .filter(item -> item != null && item.getType() == Material.DIAMOND)
                    .mapToInt(ItemStack::getAmount)
                    .sum();

            // นับ Diamond Block ในช่องคราฟติ้ง (1 บล็อก = 9 เพชร)
            count += Arrays.stream(matrix)
                    .filter(item -> item != null && item.getType() == Material.DIAMOND_BLOCK)
                    .mapToInt(item -> item.getAmount() * 9)
                    .sum();

            // ไม่นับผลลัพธ์ในช่องคราฟติ้งจนกว่าการคราฟจะสำเร็จ (ผ่าน CraftItemEvent)
        }

        // นับเพชรหรือ Diamond Block ที่ค้างใน cursor
        Player player = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.getOpenInventory().getTopInventory() == craftingInventory || p.getInventory() == inventory)
                .findFirst().orElse(null);
        if (player != null) {
            ItemStack cursor = player.getOpenInventory().getCursor();
            if (cursor != null) {
                if (cursor.getType() == Material.DIAMOND) {
                    count += cursor.getAmount();
                } else if (cursor.getType() == Material.DIAMOND_BLOCK) {
                    count += cursor.getAmount() * 9;
                }
            }
        }

        return count;
    }

    private void removeDiamonds(Inventory inventory, int amount) {
        int remaining = amount;

        // ลบ Diamond Block ก่อน (1 บล็อก = 9 เพชร)
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
                int diamondEquivalent = item.getAmount() * 9;
                if (diamondEquivalent <= remaining) {
                    remaining -= diamondEquivalent;
                    inventory.setItem(i, null);
                } else {
                    int blocksToRemove = remaining / 9;
                    remaining -= blocksToRemove * 9;
                    item.setAmount(item.getAmount() - blocksToRemove);
                    if (item.getAmount() == 0) {
                        inventory.setItem(i, null);
                    }
                }
            }
        }

        // ลบเพชรที่เหลือ
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.DIAMOND) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private void checkWinCondition() {
        if (score >= winScore && !plugin.getSubtitleManager().isCountingDown()) {
            int countdownSeconds = plugin.getConfigManager().getConfig().getInt("countdownSeconds", 15);
            plugin.getSubtitleManager().startCountdown(countdownSeconds);
        } else if (score < winScore && plugin.getSubtitleManager().isCountingDown()) {
            plugin.getSubtitleManager().stopCountdown();
        }
    }
}