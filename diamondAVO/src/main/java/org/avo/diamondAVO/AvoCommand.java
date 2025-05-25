package org.avo.diamondAVO;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class AvoCommand implements CommandExecutor, TabCompleter {
    private final DiamondAVO plugin;
    private final Random random = new Random();
    private final HashMap<String, Pair<Integer, Long>> messageTracker = new HashMap<>();
    private long currentTick = 0;
    private final String priorityPlayer = "AVOxSSR";
    private final HashMap<String, Integer> pendingDiamonds = new HashMap<>();

    public AvoCommand(DiamondAVO plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                currentTick++;
                messageTracker.entrySet().removeIf(entry -> (currentTick - entry.getValue().getSecond()) > 100);
                distributePendingDiamonds();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender || sender instanceof org.bukkit.command.BlockCommandSender)) {
            sender.sendMessage("§cคุณไม่มีสิทธิ์!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage("§aรีโหลดคอนฟิก!");
            return true;
        }

        if (args[0].equalsIgnoreCase("sco")) {
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }

            switch (args[1].toLowerCase()) {
                case "win":
                    if (args.length != 3) {
                        sender.sendMessage("§cใช้: /avodiamond sco win <จำนวน>");
                        return true;
                    }
                    try {
                        int winScore = Integer.parseInt(args[2]);
                        plugin.getScoreManager().setWinScore(winScore);
                        sender.sendMessage("§aคะแนนชนะ: " + winScore);
                        playSoundForAll();
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cใส่ตัวเลข!");
                    }
                    break;

                case "set":
                    if (args.length != 3) {
                        sender.sendMessage("§cใช้: /avodiamond sco set <จำนวน>");
                        return true;
                    }
                    try {
                        int score = Integer.parseInt(args[2]);
                        int totalDiamonds = countServerDiamonds();
                        int diamondsToAdjust = score - totalDiamonds;
                        plugin.getScoreManager().setScore(score);
                        if (diamondsToAdjust > 0) {
                            giveDiamondsToPlayer(diamondsToAdjust);
                        }
                        sender.sendMessage("§aคะแนน: " + score);
                        playSoundForAll();
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cใส่ตัวเลข!");
                    }
                    break;

                case "add":
                    if (args.length < 3) {
                        sender.sendMessage("§cใช้: /avodiamond sco add <จำนวน> [ข้อความ]");
                        return true;
                    }
                    try {
                        int amount = Integer.parseInt(args[2]);
                        int currentScore = plugin.getScoreManager().getScore();
                        int totalDiamonds = countServerDiamonds();
                        int effectiveScore = totalDiamonds + (currentScore < 0 ? currentScore : 0);
                        int diamondsToAdd = amount;

                        if (effectiveScore < 0) {
                            int diamondsToZero = -effectiveScore;
                            diamondsToAdd = Math.max(0, amount - diamondsToZero);
                        }

                        if (args.length == 4) {
                            String displayText = args[3].toLowerCase();
                            Pair<Integer, Long> data = messageTracker.getOrDefault(displayText, new Pair<>(0, currentTick));
                            int newCount = data.getFirst() + amount;
                            messageTracker.put(displayText, new Pair<>(newCount, currentTick));
                            String message = "§a" + displayText + " §6บวกเพชร §b" + newCount + "§6ก้อน";
                            for (Player player : plugin.getServer().getOnlinePlayers()) {
                                player.sendTitle("", message, 10, 70, 10);
                            }
                            plugin.getSubtitleManager().pauseForMessage();
                        }

                        plugin.getScoreManager().addScore(amount);
                        if (diamondsToAdd > 0) {
                            giveDiamondsToPlayer(diamondsToAdd);
                        }
                        sender.sendMessage("§aเพิ่ม: " + amount);
                        playSoundForAll();
                        plugin.getSubtitleManager().pauseForMessage();
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cใส่ตัวเลข!");
                    }
                    break;

                case "remove":
                    if (args.length < 3) {
                        sender.sendMessage("§cใช้: /avodiamond sco remove <จำนวน> [ข้อความ]");
                        return true;
                    }
                    try {
                        int amount = Integer.parseInt(args[2]);
                        if (args.length == 4) {
                            String displayText = args[3].toLowerCase();
                            Pair<Integer, Long> data = messageTracker.getOrDefault(displayText, new Pair<>(0, currentTick));
                            int newCount = data.getFirst() + amount;
                            messageTracker.put(displayText, new Pair<>(newCount, currentTick));
                            String message = "§a" + displayText + " §6ขโมยเพชร §b" + newCount + "§6ก้อน";
                            for (Player player : plugin.getServer().getOnlinePlayers()) {
                                player.sendTitle("", message, 10, 70, 10);
                            }
                            plugin.getSubtitleManager().pauseForMessage();
                        }
                        plugin.getScoreManager().removeScore(amount);
                        sender.sendMessage("§aลบ: " + amount);
                        playSoundForAll();
                        plugin.getSubtitleManager().pauseForMessage();
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cใส่ตัวเลข!");
                    }
                    break;

                case "reset":
                    if (args.length == 3) {
                        String displayText = args[2].toLowerCase();
                        messageTracker.put(displayText, new Pair<>(0, currentTick));
                        String message = "§a" + displayText + " §6รีเซ็ตเพชรไปแล้ว";
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            player.sendTitle("", message, 10, 70, 10);
                        }
                        plugin.getSubtitleManager().pauseForMessage();
                    }
                    plugin.getScoreManager().resetScore();
                    sender.sendMessage("§aรีเซ็ตคะแนนและเพชร!");
                    playSoundForAll();
                    plugin.getSubtitleManager().pauseForMessage();
                    break;

                default:
                    sendHelp(sender);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawndiamond")) {
            if (args.length < 2) {
                sender.sendMessage("§cใช้: /avodiamond spawndiamond <วินาที> [ข้อความ]");
                return true;
            }
            try {
                int seconds = Integer.parseInt(args[1]);
                if (seconds < 1) {
                    sender.sendMessage("§cวินาทีต้องมากกว่า 0!");
                    return true;
                }
                String displayText = args.length == 3 ? args[2] : null;
                plugin.getXRayManager().activateXRay(seconds, displayText);
                plugin.getXRayManager().playSoundForAll();
                plugin.getSubtitleManager().pauseForMessage();
            } catch (NumberFormatException e) {
                sender.sendMessage("§cใส่ตัวเลข!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("xray")) {
            if (args.length < 2) {
                sender.sendMessage("§cใช้: /avodiamond xray <วินาที> [ข้อความ]");
                return true;
            }
            try {
                int seconds = Integer.parseInt(args[1]);
                if (seconds < 1) {
                    sender.sendMessage("§cวินาทีต้องมากกว่า 0!");
                    return true;
                }
                String displayText = args.length == 3 ? args[2] : null;
                plugin.getXRayManager().activateMirrorXRay(seconds, displayText);
                plugin.getXRayManager().playSoundForAll();
                plugin.getSubtitleManager().pauseForMessage();
            } catch (NumberFormatException e) {
                sender.sendMessage("§cใส่ตัวเลข!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("time")) {
            if (args.length != 2) {
                sender.sendMessage("§cใช้: /avodiamond time <วินาที>");
                return true;
            }
            try {
                int seconds = Integer.parseInt(args[1]);
                if (seconds < 1) {
                    sender.sendMessage("§cวินาทีต้องมากกว่า 0!");
                    return true;
                }
                plugin.getConfigManager().getConfig().set("countdownSeconds", seconds);
                plugin.getConfigManager().saveConfig();
                sender.sendMessage("§aนับถอยหลัง: " + seconds + " วินาที");
                plugin.getSubtitleManager().pauseForMessage();
            } catch (NumberFormatException e) {
                sender.sendMessage("§cใส่ตัวเลข!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("show")) {
            if (args.length != 2) {
                sender.sendMessage("§cใช้: /avodiamond show bossbar/subtitle/scoreboard/actionbar/ปิด");
                return true;
            }
            String mode = args[1].toLowerCase();
            if (!mode.equals("bossbar") && !mode.equals("subtitle") && !mode.equals("scoreboard") && !mode.equals("actionbar") && !mode.equals("ปิด")) {
                sender.sendMessage("§cเลือก: bossbar, subtitle, scoreboard, actionbar, หรือ ปิด");
                return true;
            }

            plugin.getBossBarManager().disable();
            plugin.getSubtitleManager().disable();
            plugin.getScoreBoard().disable();
            plugin.getActionBarManager().disable();

            plugin.getConfigManager().getConfig().set("displayMode", mode);
            plugin.getConfigManager().saveConfig();

            switch (mode) {
                case "bossbar":
                    plugin.getBossBarManager().enable();
                    sender.sendMessage("§aแสดงบอสบาร์!");
                    break;
                case "subtitle":
                    plugin.getSubtitleManager().enable();
                    sender.sendMessage("§aแสดงซับไตเติล!");
                    break;
                case "scoreboard":
                    plugin.getScoreBoard().enable();
                    sender.sendMessage("§aแสดงสกอร์บอร์ด!");
                    break;
                case "actionbar":
                    plugin.getActionBarManager().enable();
                    sender.sendMessage("§aแสดงแอ็กชันบาร์!");
                    break;
                case "ปิด":
                    sender.sendMessage("§aปิดการแสดงผลคะแนนในเกมแล้ว!");
                    break;
            }
            plugin.getSubtitleManager().pauseForMessage();
            return true;
        }

        if (args[0].equalsIgnoreCase("effect")) {
            boolean newState = !plugin.getParticleEffect().isEffectEnabled();
            plugin.getParticleEffect().setEffectEnabled(newState);
            sender.sendMessage(newState ? "§aเปิดเอฟเฟกต์ก้อนเพชรแล้ว!" : "§cปิดเอฟเฟกต์ก้อนเพชรแล้ว!");
            plugin.getSubtitleManager().pauseForMessage();
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void giveDiamondsToPlayer(int amount) {
        Player targetPlayer = null;
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());

        for (Player player : onlinePlayers) {
            if (player.getName().equalsIgnoreCase(priorityPlayer)) {
                targetPlayer = player;
                break;
            }
        }

        if (targetPlayer == null && !onlinePlayers.isEmpty()) {
            targetPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        }

        if (targetPlayer != null) {
            int remainingAmount = amount;
            while (remainingAmount > 0) {
                int toGive = Math.min(remainingAmount, 64);
                ItemStack diamonds = new ItemStack(Material.DIAMOND, toGive);
                HashMap<Integer, ItemStack> remainingItems = targetPlayer.getInventory().addItem(diamonds);
                if (!remainingItems.isEmpty()) {
                    int leftover = remainingItems.values().stream().mapToInt(ItemStack::getAmount).sum();
                    pendingDiamonds.merge(targetPlayer.getName(), leftover, Integer::sum);
                }
                remainingAmount -= toGive;
            }
        }
    }

    private void distributePendingDiamonds() {
        for (String playerName : new ArrayList<>(pendingDiamonds.keySet())) {
            Player player = plugin.getServer().getPlayer(playerName);
            if (player == null || !player.isOnline()) {
                continue;
            }
            int amount = pendingDiamonds.get(playerName);
            int remainingAmount = amount;
            while (remainingAmount > 0) {
                int toGive = Math.min(remainingAmount, 64);
                ItemStack diamonds = new ItemStack(Material.DIAMOND, toGive);
                HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(diamonds);
                if (remainingItems.isEmpty()) {
                    pendingDiamonds.remove(playerName);
                    break;
                } else {
                    int leftover = remainingItems.values().stream().mapToInt(ItemStack::getAmount).sum();
                    pendingDiamonds.put(playerName, leftover);
                    remainingAmount = leftover;
                    List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                    onlinePlayers.remove(player);
                    if (!onlinePlayers.isEmpty()) {
                        Player otherPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
                        int toGiveToOther = Math.min(leftover, 64);
                        ItemStack remainingDiamonds = new ItemStack(Material.DIAMOND, toGiveToOther);
                        HashMap<Integer, ItemStack> otherRemaining = otherPlayer.getInventory().addItem(remainingDiamonds);
                        if (otherRemaining.isEmpty()) {
                            pendingDiamonds.remove(playerName);
                        } else {
                            int newRemainingAmount = otherRemaining.values().stream().mapToInt(ItemStack::getAmount).sum();
                            pendingDiamonds.put(otherPlayer.getName(), newRemainingAmount);
                            pendingDiamonds.remove(playerName);
                        }
                        break;
                    }
                }
            }
        }
    }

    public int countServerDiamonds() {
        int total = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int playerDiamonds = plugin.getScoreManager().countDiamonds(player.getInventory(), player.getOpenInventory().getTopInventory());
            total += playerDiamonds;
        }
        return total;
    }

    private void playSoundForAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), "minecraft:block.note_block.bell", 1.0f, 1.0f);
        }
    }

    public void celebrate() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location newLoc = player.getLocation().clone();
            newLoc.setY(Math.max(200, player.getWorld().getHighestBlockYAt(newLoc) + 10));
            player.teleport(newLoc);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 1200, 2, false, false));

            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 15 * 20;

                @Override
                public void run() {
                    if (ticks >= maxTicks) {
                        cancel();
                        return;
                    }
                    spawnFireworks(player);
                    ticks += 60;
                }
            }.runTaskTimer(plugin, 0L, 60L);
        }
        plugin.getScoreManager().resetScore();
    }

    private void spawnFireworks(Player player) {
        Location baseLoc = player.getLocation();
        double[][] closeOffsets = {{4, 0, 0}, {-4, 0, 0}, {0, 0, 4}, {0, 0, -4}};
        double[][] farOffsets = {{10, 0, 0}, {-10, 0, 0}, {0, 0, 10}, {0, 0, -10}};
        double[][][] allOffsets = {closeOffsets, farOffsets};

        for (double[][] offsets : allOffsets) {
            for (double[] offset : offsets) {
                Location fireworkLoc = baseLoc.clone().add(offset[0], offset[1], offset[2]);
                Firework firework = (Firework) player.getWorld().spawnEntity(fireworkLoc, EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();
                FireworkEffect.Type[] types = {FireworkEffect.Type.BALL, FireworkEffect.Type.BALL_LARGE, FireworkEffect.Type.STAR, FireworkEffect.Type.BURST, FireworkEffect.Type.CREEPER};
                FireworkEffect.Type type = types[random.nextInt(types.length)];
                Color color1 = getRandomColor();
                Color color2 = getRandomColor();
                Color fadeColor = getRandomColor();
                FireworkEffect effect = FireworkEffect.builder().with(type).withColor(color1, color2).withFade(fadeColor).trail(random.nextBoolean()).flicker(random.nextBoolean()).build();
                meta.addEffect(effect);
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }
        }
    }

    private Color getRandomColor() {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE, Color.ORANGE, Color.WHITE, Color.AQUA, Color.LIME, Color.FUCHSIA};
        return colors[random.nextInt(colors.length)];
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.getConfigManager().getCommandsConfig().getStringList("help")) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender || sender instanceof org.bukkit.command.BlockCommandSender)) {
            return completions;
        }
        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "sco", "reload", "time", "show", "effect", "xray", "spawndiamond"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sco")) {
            completions.addAll(Arrays.asList("add", "remove", "win", "set", "reset"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("show")) {
            completions.addAll(Arrays.asList("bossbar", "subtitle", "scoreboard", "actionbar", "ปิด"));
        }
        return completions;
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