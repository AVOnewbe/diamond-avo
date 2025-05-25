package org.avo.diamondAVO;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionHandler implements Listener {
    private final JavaPlugin plugin;

    public ExplosionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // วนลูปผ่านบล็อกที่ได้รับผลกระทบจากการระเบิด
        for (Block block : event.blockList()) {
            Material material = block.getType();
            // ถ้าเป็น DIAMOND_ORE หรือ DEEPSLATE_DIAMOND_ORE
            if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                // ดีบักเพื่อตรวจสอบ
                plugin.getLogger().info("Explosion detected at " + block.getLocation() + " for " + material);
                // ลบการดรอปไอเทม
                block.getDrops().clear();
                // เปลี่ยนบล็อกเป็น AIR โดยตรงเพื่อให้แน่ใจว่าไม่มีการดรอป
                block.setType(Material.AIR);
            }
        }
    }
}