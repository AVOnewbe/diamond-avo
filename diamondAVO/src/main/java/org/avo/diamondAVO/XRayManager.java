package org.avo.diamondAVO;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class XRayManager implements Listener {
    private final DiamondAVO plugin;
    private boolean isXRayActive = false;
    private boolean isMirrorXRayActive = false;
    private int remainingTicks = 0;
    private int mirrorRemainingTicks = 0;
    private final Map<String, Material> originalBlocks = new HashMap<>();
    private final Map<String, Material> mirrorOriginalBlocks = new HashMap<>();
    private final Map<String, Material> bedrockOriginalBlocks = new HashMap<>();
    private final Map<String, Pair<Integer, Long>> messageTracker = new HashMap<>();
    private long currentTick = 0;
    private BossBar xRayBossBar;
    private BossBar mirrorXRayBossBar;
    private BukkitRunnable timerTask;
    private BukkitRunnable mirrorTimerTask;
    private final Set<Location> protectedGlassBlocks = new HashSet<>();
    private final Set<Location> protectedBedrockGlassBlocks = new HashSet<>();
    private final Set<Material> protectedBlocks = new HashSet<>();

    public XRayManager(DiamondAVO plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        xRayBossBar = plugin.getServer().createBossBar("SpawnDiamond: 0 วินาที", BarColor.PURPLE, BarStyle.SOLID);
        xRayBossBar.setVisible(false);
        mirrorXRayBossBar = plugin.getServer().createBossBar("Mirror X-Ray: 0 วินาที", BarColor.BLUE, BarStyle.SOLID);
        mirrorXRayBossBar.setVisible(false);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            xRayBossBar.addPlayer(player);
            mirrorXRayBossBar.addPlayer(player);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                currentTick++;
                messageTracker.entrySet().removeIf(entry -> (currentTick - entry.getValue().getSecond()) > 100);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // เพิ่มบล็อกที่ห้ามเปลี่ยนแปลงลงใน Set
        protectedBlocks.addAll(Arrays.asList(
                // เตียง (ทุกสี)
                Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED, Material.LIGHT_BLUE_BED,
                Material.YELLOW_BED, Material.LIME_BED, Material.PINK_BED, Material.GRAY_BED,
                Material.LIGHT_GRAY_BED, Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
                Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED,

                // ประตู (ทุกประเภท)
                Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR,
                Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.MANGROVE_DOOR, Material.CHERRY_DOOR,
                Material.BAMBOO_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR,

                // หีบ
                Material.CHEST, Material.TRAPPED_CHEST,

                // โต๊ะคราฟ
                Material.CRAFTING_TABLE,

                // เอนเดอร์เชสต์
                Material.ENDER_CHEST,

                // ชัลเกอร์บ็อกซ์ (ทุกสี)
                Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
                Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
                Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX,

                // ไอเทมเฟรม (ทั้งแบบปกติและเรืองแสง)
                Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME
        ));
    }

    public void activateXRay(int seconds, String displayText) {
        remainingTicks += seconds * 20;

        if (displayText != null && !displayText.isEmpty()) {
            displayText = displayText.toLowerCase();
            Pair<Integer, Long> data = messageTracker.getOrDefault(displayText, new Pair<>(0, currentTick));
            int newDuration = data.getFirst() + seconds;
            messageTracker.put(displayText, new Pair<>(newDuration, currentTick));
            String message = "§a" + displayText + " §6เปิดโปรให้ §b" + newDuration + "§6วิ";
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendTitle("", message, 10, 70, 10);
            }
        }

        if (isXRayActive) {
            revertBlocks();
        }

        isXRayActive = true;
        changeBlocksToDiamondOre();
        startXRayTimer();
    }

    public void activateMirrorXRay(int seconds, String displayText) {
        mirrorRemainingTicks += seconds * 20;

        if (displayText != null && !displayText.isEmpty()) {
            displayText = displayText.toLowerCase();
            Pair<Integer, Long> data = messageTracker.getOrDefault(displayText, new Pair<>(0, currentTick));
            int newDuration = data.getFirst() + seconds;
            messageTracker.put(displayText, new Pair<>(newDuration, currentTick));
            String message = "§a" + displayText + " §6เปิดตาทิพ §b" + newDuration + "§6วิ";
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendTitle("", message, 10, 70, 10);
            }
        }

        if (isMirrorXRayActive) {
            revertMirrorBlocks();
        }

        isMirrorXRayActive = true;
        changeBlocksToGlass();
        startMirrorXRayTimer();

        // ดีบักเพื่อตรวจสอบว่า BossBar ทำงานหรือไม่
        plugin.getLogger().info("Mirror X-Ray activated for " + seconds + " seconds. BossBar visibility: " + mirrorXRayBossBar.isVisible());
    }

    private void changeBlocksToDiamondOre() {
        originalBlocks.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location center = player.getLocation();
            int radius = 6;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location loc = center.clone().add(x, y, z);
                        Block block = loc.getBlock();
                        Material material = block.getType();
                        String blockKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

                        // ข้ามถ้าเป็น DIAMOND_ORE หรือ DEEPSLATE_DIAMOND_ORE เพื่อป้องกันการเปลี่ยนซ้ำ
                        if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                            continue;
                        }

                        // ข้ามบล็อกที่ห้ามเปลี่ยนแปลง
                        if (protectedBlocks.contains(material)) {
                            continue;
                        }

                        // ข้ามถ้าบล็อกนี้ถูกบันทึกแล้ว (ป้องกันการซ้ำซ้อน)
                        if (originalBlocks.containsKey(blockKey)) {
                            continue;
                        }

                        if (material != Material.AIR) {
                            originalBlocks.put(blockKey, material);
                            block.setType(Material.DIAMOND_ORE);
                        }
                    }
                }
            }
        }
    }

    private void changeBlocksToGlass() {
        mirrorOriginalBlocks.clear();
        bedrockOriginalBlocks.clear();
        protectedGlassBlocks.clear();
        protectedBedrockGlassBlocks.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location center = player.getLocation();
            int radius = 16;
            int playerY = center.getBlockY();
            int maxY = playerY + 30;
            int minY = -64;

            for (int x = -radius; x <= radius; x++) {
                for (int y = maxY - center.getBlockY(); y >= minY - center.getBlockY(); y--) {
                    for (int z = -radius; z <= radius; z++) {
                        Location loc = center.clone().add(x, y, z);
                        Block block = loc.getBlock();
                        Material material = block.getType();
                        String blockKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

                        // ข้ามถ้าเป็น GLASS หรือ LIGHT_BLUE_STAINED_GLASS เพื่อป้องกันการเปลี่ยนซ้ำ
                        if (material == Material.GLASS || material == Material.LIGHT_BLUE_STAINED_GLASS) {
                            continue;
                        }

                        // ข้ามบล็อกที่ห้ามเปลี่ยนแปลง
                        if (protectedBlocks.contains(material)) {
                            continue;
                        }

                        // ข้ามถ้าบล็อกนี้ถูกบันทึกแล้ว (ป้องกันการซ้ำซ้อน)
                        if (mirrorOriginalBlocks.containsKey(blockKey) || bedrockOriginalBlocks.containsKey(blockKey)) {
                            continue;
                        }

                        if (material == Material.BEDROCK) {
                            bedrockOriginalBlocks.put(blockKey, material);
                            block.setType(Material.LIGHT_BLUE_STAINED_GLASS);
                            protectedBedrockGlassBlocks.add(loc);
                        } else if (material != Material.DIAMOND_ORE && material != Material.DEEPSLATE_DIAMOND_ORE && material != Material.AIR) {
                            mirrorOriginalBlocks.put(blockKey, material);
                            block.setType(Material.GLASS);
                            protectedGlassBlocks.add(loc);
                        }
                    }
                }
            }
        }
    }

    private void revertBlocks() {
        for (Map.Entry<String, Material> entry : originalBlocks.entrySet()) {
            String[] coords = entry.getKey().split(",");
            String worldName = coords[0];
            int x = Integer.parseInt(coords[1]);
            int y = Integer.parseInt(coords[2]);
            int z = Integer.parseInt(coords[3]);
            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            Material originalMaterial = entry.getValue();
            Block block = loc.getBlock();

            // ตรวจสอบว่าบล็อกยังเป็น DIAMOND_ORE หรือไม่ เพื่อป้องกันการเปลี่ยนแปลงที่ไม่คาดคิด
            if (block.getType() == Material.DIAMOND_ORE) {
                block.setType(originalMaterial);
            }
        }
        originalBlocks.clear();
    }

    private void revertMirrorBlocks() {
        for (Map.Entry<String, Material> entry : mirrorOriginalBlocks.entrySet()) {
            String[] coords = entry.getKey().split(",");
            String worldName = coords[0];
            int x = Integer.parseInt(coords[1]);
            int y = Integer.parseInt(coords[2]);
            int z = Integer.parseInt(coords[3]);
            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            Material originalMaterial = entry.getValue();
            Block block = loc.getBlock();

            // ตรวจสอบว่าบล็อกยังเป็น GLASS หรือไม่ เพื่อป้องกันการเปลี่ยนแปลงที่ไม่คาดคิด
            if (block.getType() == Material.GLASS) {
                block.setType(originalMaterial);
            }
        }
        for (Map.Entry<String, Material> entry : bedrockOriginalBlocks.entrySet()) {
            String[] coords = entry.getKey().split(",");
            String worldName = coords[0];
            int x = Integer.parseInt(coords[1]);
            int y = Integer.parseInt(coords[2]);
            int z = Integer.parseInt(coords[3]);
            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            Material originalMaterial = entry.getValue();
            Block block = loc.getBlock();

            // ตรวจสอบว่าบล็อกยังเป็น LIGHT_BLUE_STAINED_GLASS หรือไม่
            if (block.getType() == Material.LIGHT_BLUE_STAINED_GLASS) {
                block.setType(originalMaterial);
            }
        }
        mirrorOriginalBlocks.clear();
        bedrockOriginalBlocks.clear();
        protectedGlassBlocks.clear();
        protectedBedrockGlassBlocks.clear();
    }

    private void startXRayTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        xRayBossBar.setVisible(true);
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingTicks <= 0) {
                    revertBlocks();
                    isXRayActive = false;
                    xRayBossBar.setVisible(false);
                    cancel();
                    return;
                }
                int secondsLeft = remainingTicks / 20;
                xRayBossBar.setTitle("SpawnDiamond: " + secondsLeft + " วินาที");
                xRayBossBar.setProgress(Math.min(1.0, (double) remainingTicks / (remainingTicks + 20)));
                remainingTicks--;
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void startMirrorXRayTimer() {
        if (mirrorTimerTask != null) {
            mirrorTimerTask.cancel();
        }

        mirrorXRayBossBar.setVisible(true);
        mirrorTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (mirrorRemainingTicks <= 0) {
                    revertMirrorBlocks();
                    isMirrorXRayActive = false;
                    mirrorXRayBossBar.setVisible(false);
                    cancel();
                    return;
                }
                int secondsLeft = mirrorRemainingTicks / 20;
                mirrorXRayBossBar.setTitle("Mirror X-Ray: " + secondsLeft + " วินาที");
                mirrorXRayBossBar.setProgress(Math.min(1.0, (double) mirrorRemainingTicks / (mirrorRemainingTicks + 20)));
                mirrorRemainingTicks--;
            }
        };
        mirrorTimerTask.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isXRayActive && !isMirrorXRayActive) return;

        Location loc = event.getBlock().getLocation();
        String blockKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        if (event.getBlock().getType() == Material.LIGHT_BLUE_STAINED_GLASS && protectedBedrockGlassBlocks.contains(loc)) {
            event.setCancelled(true);
            return;
        }

        if (isXRayActive && originalBlocks.containsKey(blockKey)) {
            originalBlocks.remove(blockKey);
            plugin.getLogger().info("Block at " + blockKey + " was broken and removed from originalBlocks.");
        }
        if (isMirrorXRayActive && mirrorOriginalBlocks.containsKey(blockKey)) {
            mirrorOriginalBlocks.remove(blockKey);
            protectedGlassBlocks.remove(loc);
            plugin.getLogger().info("Mirror block at " + blockKey + " was broken and removed from mirrorOriginalBlocks.");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getType() == Material.GLASS && protectedGlassBlocks.contains(block.getLocation()));
        event.blockList().removeIf(block -> block.getType() == Material.LIGHT_BLUE_STAINED_GLASS && protectedBedrockGlassBlocks.contains(block.getLocation()));
    }

    public void playSoundForAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), "minecraft:block.note_block.bell", 1.0f, 1.0f);
        }
    }

    public BossBar getXRayBossBar() {
        return xRayBossBar;
    }

    private static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }
}