package org.avo.diamondAVO;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class DiamondListener implements Listener {
    private final DiamondAVO plugin;
    private final DiamondParticleEffect particleEffect;

    public DiamondListener(DiamondAVO plugin) {
        this.plugin = plugin;
        this.particleEffect = plugin.getParticleEffect();
        startDiamondCounter();
    }

    private void startDiamondCounter() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateScore();
            }
        }.runTaskTimer(plugin, 0L, 10L); // อัปเดตทุก 0.5 วินาที
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getBossBarManager().addPlayer(player);
        plugin.getScoreBoard().createScoreboard(player);
        plugin.getXRayManager().getXRayBossBar().addPlayer(player);
        updateScore();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBossBarManager().removePlayer(event.getPlayer());
        updateScore();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        updateScore();
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DIAMOND) {
            updateScore();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        // ตรวจจับเมื่อหยิบเพชรขึ้นมาและค้างใน cursor
        if (cursor != null && cursor.getType() == Material.DIAMOND && (currentItem == null || currentItem.getType() != Material.DIAMOND)) {
            updateScore();
        }

        // ตรวจจับเมื่อหยิบเพชรจากคราฟติ้งและค้างใน cursor
        if (inventory instanceof CraftingInventory && cursor != null && cursor.getType() == Material.DIAMOND) {
            updateScore();
        }

        // ป้องกันการนับซ้ำเมื่อใส่เพชรลงคราฟติ้ง (ไม่ลดคะแนน)
        if (inventory instanceof CraftingInventory && currentItem != null && currentItem.getType() == Material.DIAMOND && cursor == null) {
            event.setCancelled(false); // อนุญาตให้ย้ายไปคราณ์ติ้งได้ตามปกติ
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();

        // ตรวจจับเมื่อลากเพชรและค้างใน cursor
        if (cursor != null && cursor.getType() == Material.DIAMOND) {
            updateScore();
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.DIAMOND_BLOCK) {
            // คราฟ Diamond Block สำเร็จ (9 เพชร -> 1 บล็อก)
            updateScore();
        } else if (result.getType() == Material.DIAMOND) {
            // คราฟจาก Diamond Block กลับเป็น 9 เพชร
            updateScore();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DIAMOND_ORE || event.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE) {
            // ไม่นับคะแนนทันที เพราะต้องคราฟก่อน
            int diamondCount = 0;
            for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
                if (drop.getType() == Material.DIAMOND) {
                    diamondCount += drop.getAmount();
                }
            }

            if (diamondCount > 0) {
                particleEffect.spawnDiamondParticles(diamondCount);
            }
        }
    }

    private void updateScore() {
        int totalDiamonds = countServerDiamonds();
        int currentScore = plugin.getScoreManager().getScore();
        int diamondsToClear = 0;

        if (currentScore < 0) {
            diamondsToClear = Math.min(totalDiamonds, -currentScore);
            if (diamondsToClear > 0) {
                removeDiamondsFromPlayers(diamondsToClear);
                totalDiamonds -= diamondsToClear;
            }
        }

        int newScore = totalDiamonds + (currentScore < 0 ? currentScore + diamondsToClear : 0);
        plugin.getScoreManager().setScore(newScore);
    }

    private int countServerDiamonds() {
        int total = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int playerDiamonds = countDiamonds(player.getInventory(), player.getOpenInventory().getTopInventory());
            total += playerDiamonds;
        }
        return total;
    }

    private int countDiamonds(Inventory inventory, Inventory craftingInventory) {
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

        // นับเพชรที่ค้างใน cursor ของผู้เล่น
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

    private void removeDiamondsFromPlayers(int amount) {
        int remaining = amount;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int playerDiamonds = countDiamonds(player.getInventory(), player.getOpenInventory().getTopInventory());
            if (remaining > 0 && playerDiamonds > 0) {
                int toRemove = Math.min(playerDiamonds, remaining);
                removeDiamonds(player.getInventory(), toRemove);
                remaining -= toRemove;
            }
        }
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
}