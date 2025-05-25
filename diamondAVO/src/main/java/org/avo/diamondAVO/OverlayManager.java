package org.avo.diamondAVO;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OverlayManager {
    private final DiamondAVO plugin;
    private HttpServer server;
    private String overlayHtmlContent;

    public OverlayManager(DiamondAVO plugin) {
        this.plugin = plugin;

        // ดึงสีจาก config.yml
        String overlayColor = plugin.getConfigManager().getConfig().getString("overlay.color", "#FFFFFF");
        String shadowColor = plugin.getConfigManager().getConfig().getString("overlay.shadowColor", "#000000");

        // โหลดไฟล์ overlay.html จาก resources
        try (InputStream is = plugin.getResource("overlay.html")) {
            if (is == null) {
                plugin.getLogger().severe("ไม่พบไฟล์ overlay.html ใน resources!");
                overlayHtmlContent = "<html><body><h1>Error: overlay.html not found</h1></body></html>";
            } else {
                // แทนที่ placeholder {OVERLAY_COLOR} และ {SHADOW_COLOR} ด้วยสีจาก config
                overlayHtmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("{OVERLAY_COLOR}", overlayColor)
                        .replace("{SHADOW_COLOR}", shadowColor);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถโหลด overlay.html ได้: " + e.getMessage());
            overlayHtmlContent = "<html><body><h1>Error loading overlay.html</h1></body></html>";
        }

        // เริ่ม Web Server
        startWebServer();
    }

    private void startWebServer() {
        try {
            // ดึงพอร์ตจาก config.yml
            int port = plugin.getConfigManager().getConfig().getInt("webOverlayPort", 8080);
            // Bind กับ 0.0.0.0 เพื่อให้สามารถเข้าถึงจากภายนอกได้
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            // เส้นทางสำหรับหน้า Overlay
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // เพิ่ม CORS
                    exchange.sendResponseHeaders(200, overlayHtmlContent.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(overlayHtmlContent.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            });

            // เส้นทางสำหรับดึงข้อมูลคะแนน
            server.createContext("/score", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    int score = plugin.getScoreManager().getScore();
                    int winScore = plugin.getScoreManager().getWinScore();
                    String displayText = score + "/" + winScore;

                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // เพิ่ม CORS
                    exchange.sendResponseHeaders(200, displayText.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(displayText.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            });

            // เส้นทางสำหรับไฟล์ฟอนต์
            server.createContext("/fonts/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String path = exchange.getRequestURI().getPath();
                    String fileName = path.substring("/fonts/".length());

                    try (InputStream is = plugin.getResource("fonts/" + fileName)) {
                        if (is == null) {
                            // ไฟล์ไม่พบ
                            exchange.sendResponseHeaders(404, 0);
                            OutputStream os = exchange.getResponseBody();
                            os.close();
                            return;
                        }

                        byte[] fileBytes = is.readAllBytes();
                        exchange.getResponseHeaders().set("Content-Type", "font/ttf");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // เพิ่ม CORS
                        exchange.sendResponseHeaders(200, fileBytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(fileBytes);
                        os.close();
                    } catch (IOException e) {
                        plugin.getLogger().severe("ไม่สามารถโหลดไฟล์ฟอนต์ " + fileName + ": " + e.getMessage());
                        exchange.sendResponseHeaders(500, 0);
                        OutputStream os = exchange.getResponseBody();
                        os.close();
                    }
                }
            });

            server.setExecutor(null); // ใช้ Default Executor
            server.start();
            plugin.getLogger().info("Web Overlay Server เริ่มทำงานที่พอร์ต " + port);
            plugin.getLogger().info("ใช้ URL นี้ใน OBS: http://localhost:" + port);

        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถเริ่ม Web Overlay Server ได้: " + e.getMessage());
        }
    }

    public void updateOverlay() {
        int score = plugin.getScoreManager().getScore();
        int winScore = plugin.getScoreManager().getWinScore();
        String displayText = score + "/" + winScore;

        // เขียนไฟล์ overlay.txt เสมอ
        try {
            Path filePath = Paths.get(plugin.getDataFolder().getPath(), "overlay.txt");
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, displayText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถเขียนไฟล์ overlay.txt ได้: " + e.getMessage());
        }

        // Web Overlay จะอัพเดทผ่านการเรียก /score
    }

    public void clearOverlay() {
        // ลบไฟล์ overlay.txt
        try {
            Path filePath = Paths.get(plugin.getDataFolder().getPath(), "overlay.txt");
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถลบไฟล์ overlay.txt ได้: " + e.getMessage());
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web Overlay Server หยุดทำงาน");
        }
    }
}