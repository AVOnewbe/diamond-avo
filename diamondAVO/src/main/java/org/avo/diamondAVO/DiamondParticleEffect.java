package org.avo.diamondAVO;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class DiamondParticleEffect {
    private final DiamondAVO plugin;
    private boolean isEffectEnabled;

    public DiamondParticleEffect(DiamondAVO plugin) {
        this.plugin = plugin;
        this.isEffectEnabled = plugin.getConfigManager().getConfig().getBoolean("effectEnabled", false);
    }

    public boolean isEffectEnabled() {
        return isEffectEnabled;
    }

    public void setEffectEnabled(boolean enabled) {
        this.isEffectEnabled = enabled;
        plugin.getConfigManager().getConfig().set("effectEnabled", enabled);
        plugin.getConfigManager().saveConfig();
    }

    public void spawnDiamondParticles(int diamondCount) {
        if (!isEffectEnabled) return; // ไม่สร้างเอฟเฟกต์ถ้าปิดอยู่

        // 5 วินาทีต่อชิ้น รวมเวลา = 5 วินาที * จำนวนเพชร
        int durationTicks = 10 * 20 * diamondCount; // 5 วินาที = 100 ticks ต่อชิ้น

        // สร้างไดมอนด์ก้อนสำหรับผู้เล่นแต่ละคน
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            List<Item> diamondItems = new ArrayList<>();

            // สร้างไดมอนด์ก้อน 1 ชิ้นต่อผู้เล่น
            ItemStack diamondStack = new ItemStack(Material.DIAMOND, 1);
            Location startLoc = player.getLocation().add(1.5, 1, 0); // เริ่มจากระดับเอว ห่าง 1.5 บล็อก
            Item diamond = player.getWorld().dropItem(startLoc, diamondStack);
            diamond.setPickupDelay(Integer.MAX_VALUE); // ป้องกันการเก็บ
            diamond.setInvulnerable(true); // ทำให้ไม่ถูกทำลาย
            diamond.setCustomName("EffectDiamond"); // ตั้งชื่อเพื่อระบุว่าเป็นไดมอนด์สำหรับเอฟเฟกต์
            diamond.setCustomNameVisible(false);
            diamond.setGlowing(true); // เพิ่มขอบเรืองแสง

            diamondItems.add(diamond);

            new BukkitRunnable() {
                int ticks = 0;
                final double radius = 1.5; // รัศวณการหมุน 1.5 บล็อก
                final double speed = 0.1; // ความเร็วในการหมุน (ปรับได้)

                @Override
                public void run() {
                    if (ticks >= durationTicks || !player.isOnline()) {
                        // ลบไดมอนด์ก้อนเมื่อครบเวลา หรือผู้เล่นออกจากเกม
                        for (Item item : diamondItems) {
                            if (item != null && !item.isDead()) {
                                item.remove();
                            }
                        }
                        cancel();
                        return;
                    }

                    // ตำแหน่งของผู้เล่น (ระดับเอว)
                    Location playerLoc = player.getLocation().add(0, 1, 0);

                    // อัปเดตตำแหน่งไดมอนด์
                    for (Item item : diamondItems) {
                        if (item != null && !item.isDead()) {
                            Location itemLoc = item.getLocation();

                            // คำนวณเวกเตอร์จากผู้เล่นไปยังไดมอนด์
                            Vector toItem = itemLoc.toVector().subtract(playerLoc.toVector());
                            toItem.setY(0); // ให้อยู่ในระนาบ XZ เท่านั้น (ไม่ขึ้น-ลง)

                            // ตรวจสอบระยะห่าง และปรับถ้าจำเป็น
                            double currentDistance = toItem.length();
                            if (currentDistance != 0) { // ป้องกันการหารด้วย 0
                                if (Math.abs(currentDistance - radius) > 0.1) {
                                    // ปรับระยะห่างให้เท่ากับรัศมี
                                    toItem.normalize().multiply(radius);
                                    itemLoc = playerLoc.clone().add(toItem);
                                    item.teleport(itemLoc);
                                }
                            } else {
                                // ถ้าไดมอนด์อยู่ในตำแหน่งเดียวกับผู้เล่น ให้ย้ายออกไป
                                itemLoc = playerLoc.clone().add(radius, 0, 0);
                                item.teleport(itemLoc);
                                toItem = itemLoc.toVector().subtract(playerLoc.toVector());
                            }

                            // คำนวณทิศทางการหมุน (หมุน 90 องศาในระนาบ XZ)
                            Vector direction = new Vector(-toItem.getZ(), 0, toItem.getX());
                            direction.normalize().multiply(speed);

                            // ตั้งค่า velocity เพื่อให้เคลื่อนที่ราบรื่น
                            item.setVelocity(direction);

                            // ป้องกันการเคลื่อนที่ในแกน Y (รักษาระดับความสูง)
                            Vector currentVelocity = item.getVelocity();
                            item.setVelocity(new Vector(currentVelocity.getX(), 0, currentVelocity.getZ()));
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L); // อัปเดตทุก tick (20 ครั้งต่อวินาที)
        }
    }
}