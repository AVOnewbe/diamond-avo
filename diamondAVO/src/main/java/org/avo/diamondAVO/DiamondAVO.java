package org.avo.diamondAVO;

import org.bukkit.plugin.java.JavaPlugin;

public class DiamondAVO extends JavaPlugin {
    private ConfigManager configManager;
    private ScoreManager scoreManager;
    private SubtitleManager subtitleManager;
    private DiamondParticleEffect particleEffect;
    private AvoCommand avoCommand;
    private BossBarManager bossBarManager;
    private ScoreBoard scoreBoard;
    private ActionBarManager actionBarManager;
    private XRayManager xRayManager;
    private OverlayManager overlayManager;
    private ExplosionHandler explosionHandler;

    @Override
    public void onEnable() {
        // สร้างอินสแตนซ์ของคลาสจัดการต่างๆ
        configManager = new ConfigManager(this);
        scoreManager = new ScoreManager(this);
        subtitleManager = new SubtitleManager(this);
        particleEffect = new DiamondParticleEffect(this);
        bossBarManager = new BossBarManager(this);
        scoreBoard = new ScoreBoard(this);
        actionBarManager = new ActionBarManager(this);
        xRayManager = new XRayManager(this);
        overlayManager = new OverlayManager(this);
        explosionHandler = new ExplosionHandler(this);

        // ลงทะเบียนคำสั่งและตัวจัดการอีเวนต์
        avoCommand = new AvoCommand(this);
        getCommand("avodiamond").setExecutor(avoCommand);
        getCommand("avodiamond").setTabCompleter(avoCommand);
        getServer().getPluginManager().registerEvents(new DiamondListener(this), this);

        // เริ่มต้นโหมดการแสดงผล
        initializeDisplayMode();

        getLogger().info("DiamondAVO เปิดใช้งาน!");
    }

    @Override
    public void onDisable() {
        // ปิดการแสดงผลทั้งหมด
        bossBarManager.disable();
        subtitleManager.disable();
        scoreBoard.disable();
        actionBarManager.disable();
        xRayManager.getXRayBossBar().setVisible(false);

        // หยุด Web Server
        overlayManager.stopServer();

        getLogger().info("DiamondAVO ปิดใช้งาน!");
    }

    private void initializeDisplayMode() {
        String displayMode = configManager.getConfig().getString("displayMode", "off");
        switch (displayMode.toLowerCase()) {
            case "bossbar":
                bossBarManager.enable();
                break;
            case "subtitle":
                subtitleManager.enable();
                break;
            case "scoreboard":
                scoreBoard.enable();
                break;
            case "actionbar":
                actionBarManager.enable();
                break;
            case "off":
                // ไม่เปิดโหมดใดๆ
                break;
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public SubtitleManager getSubtitleManager() {
        return subtitleManager;
    }

    public DiamondParticleEffect getParticleEffect() {
        return particleEffect;
    }

    public AvoCommand getAvoCommand() {
        return avoCommand;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public XRayManager getXRayManager() {
        return xRayManager;
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }
}