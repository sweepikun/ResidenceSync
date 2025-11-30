package cn.popcraft.residencesync.service;

import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.util.LoggerUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨服服务类
 * 
 * 处理跨服务器通信，包括数据同步、传送请求等
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class CrossServerService {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private final Gson gson = new Gson();
    private final Map<String, ServerInfo> connectedServers = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportRequest> pendingTeleports = new ConcurrentHashMap<>();
    
    // 跨服通信频道
    private static final String CHANNEL = "residencesync:message";
    
    public CrossServerService(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化跨服服务
     */
    public void initialize() {
        // 检查是否为BungeeCord环境
        if (isBungeeEnvironment()) {
            registerChannel();
            LoggerUtil.info("跨服服务已初始化（BungeeCord环境）");
        } else {
            LoggerUtil.warning("非BungeeCord环境，跨服功能将受限");
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        pendingTeleports.clear();
        connectedServers.clear();
    }
    
    /**
     * 检查是否为BungeeCord环境
     */
    private boolean isBungeeEnvironment() {
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 注册插件消息频道
     */
    private void registerChannel() {
        if (Bukkit.getServer().spigot().getConfig().getBoolean("settings.bungeecord", false)) {
            try {
                // 这里需要使用反射或依赖来注册频道
                LoggerUtil.info("正在注册跨服消息频道...");
                // 实际实现需要调用BungeeCord API
            } catch (Exception e) {
                LoggerUtil.warning("注册跨服消息频道失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 发送跨服消息
     */
    private void sendCrossServerMessage(String serverName, String messageType, JsonObject data) {
        try {
            JsonObject packet = new JsonObject();
            packet.addProperty("type", messageType);
            packet.addProperty("fromServer", plugin.getPluginConfig().getServerId());
            packet.addProperty("toServer", serverName);
            packet.addProperty("timestamp", System.currentTimeMillis());
            packet.add("data", data);
            
            String jsonMessage = gson.toJson(packet);
            
            // 发送消息到BungeeCord
            // TODO: 实现真正的跨服消息发送
            
            LoggerUtil.debug("发送跨服消息到 " + serverName + ": " + messageType);
            
        } catch (Exception e) {
            LoggerUtil.severe("发送跨服消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理接收到的跨服消息
     */
    public void handleIncomingMessage(String message) {
        try {
            JsonObject packet = JsonParser.parseString(message).getAsJsonObject();
            String messageType = packet.get("type").getAsString();
            String fromServer = packet.get("fromServer").getAsString();
            JsonObject data = packet.get("data").getAsJsonObject();
            
            switch (messageType) {
                case "SERVER_CONNECT":
                    handleServerConnect(fromServer, data);
                    break;
                case "SERVER_DISCONNECT":
                    handleServerDisconnect(fromServer);
                    break;
                case "TELEPORT_REQUEST":
                    handleTeleportRequest(fromServer, data);
                    break;
                case "TELEPORT_RESPONSE":
                    handleTeleportResponse(fromServer, data);
                    break;
                case "RESIDENCE_QUERY":
                    handleResidenceQuery(fromServer, data);
                    break;
                case "RESIDENCE_RESPONSE":
                    handleResidenceResponse(fromServer, data);
                    break;
                default:
                    LoggerUtil.warning("未知的跨服消息类型: " + messageType);
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("处理跨服消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理服务器连接消息
     */
    private void handleServerConnect(String serverName, JsonObject data) {
        ServerInfo serverInfo = gson.fromJson(data, ServerInfo.class);
        connectedServers.put(serverName, serverInfo);
        LoggerUtil.info("服务器 " + serverName + " 已连接");
    }
    
    /**
     * 处理服务器断开消息
     */
    private void handleServerDisconnect(String serverName) {
        connectedServers.remove(serverName);
        LoggerUtil.info("服务器 " + serverName + " 已断开连接");
    }
    
    /**
     * 处理传送请求
     */
    private void handleTeleportRequest(String fromServer, JsonObject data) {
        try {
            TeleportRequest request = gson.fromJson(data, TeleportRequest.class);
            
            // 验证传送请求
            Player targetPlayer = Bukkit.getPlayer(request.targetPlayerName);
            if (targetPlayer == null) {
                sendTeleportResponse(fromServer, request.requestId, false, "玩家不在线");
                return;
            }
            
            // 检查权限
            if (!hasCrossServerPermission(targetPlayer)) {
                sendTeleportResponse(fromServer, request.requestId, false, "权限不足");
                return;
            }
            
            // 保存请求
            pendingTeleports.put(request.requestId, request);
            
            // 发送确认消息给玩家
            plugin.getMessageConfig().sendMessage(targetPlayer, "tryTpRes", 
                    Map.of("resName", request.residenceName));
            
            // TODO: 这里应该处理真正的跨服传送逻辑
            
        } catch (Exception e) {
            LoggerUtil.severe("处理传送请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理传送响应
     */
    private void handleTeleportResponse(String fromServer, JsonObject data) {
        try {
            TeleportResponse response = gson.fromJson(data, TeleportResponse.class);
            
            // 查找对应的请求
            TeleportRequest request = pendingTeleports.remove(response.requestId);
            if (request == null) {
                LoggerUtil.warning("找不到对应的传送请求: " + response.requestId);
                return;
            }
            
            // 通知玩家传送结果
            Player sourcePlayer = Bukkit.getPlayer(request.sourcePlayerName);
            if (sourcePlayer != null && sourcePlayer.isOnline()) {
                if (response.success) {
                    // 传送成功，通知玩家
                    plugin.getMessageConfig().sendMessage(sourcePlayer, "playerTpSuccess", 
                            Map.of("resName", request.residenceName));
                } else {
                    // 传送失败，显示错误信息
                    plugin.getMessageConfig().sendMessage(sourcePlayer, "notPermissionTp", 
                            Map.of("resName", request.residenceName));
                }
            }
            
        } catch (Exception e) {
            LoggerUtil.severe("处理传送响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理领地查询请求
     */
    private void handleResidenceQuery(String fromServer, JsonObject data) {
        try {
            String residenceName = data.get("residenceName").getAsString();
            
            // 在本地查找领地
            plugin.getResidenceService().findResidenceCrossServer(residenceName)
                    .thenAccept(residence -> {
                        if (residence != null) {
                            JsonObject response = gson.toJsonTree(residence).getAsJsonObject();
                            sendCrossServerMessage(fromServer, "RESIDENCE_RESPONSE", response);
                        } else {
                            JsonObject response = new JsonObject();
                            response.addProperty("found", false);
                            sendCrossServerMessage(fromServer, "RESIDENCE_RESPONSE", response);
                        }
                    });
            
        } catch (Exception e) {
            LoggerUtil.severe("处理领地查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理领地查询响应
     */
    private void handleResidenceResponse(String fromServer, JsonObject data) {
        // 这个方法通常用于异步处理查询结果
        // 实际实现需要根据具体需求来设计
        LoggerUtil.debug("收到领地查询响应");
    }
    
    /**
     * 检查玩家是否有跨服权限
     */
    private boolean hasCrossServerPermission(Player player) {
        return player.hasPermission("residencesync.crossserver") ||
               player.hasPermission("residencesync.admin");
    }
    
    /**
     * 发送传送响应
     */
    private void sendTeleportResponse(String toServer, UUID requestId, boolean success, String reason) {
        TeleportResponse response = new TeleportResponse(requestId, success, reason);
        JsonObject data = gson.toJsonTree(response).getAsJsonObject();
        sendCrossServerMessage(toServer, "TELEPORT_RESPONSE", data);
    }
    
    /**
     * 请求跨服传送
     */
    public CompletableFuture<Boolean> requestCrossServerTeleport(Player player, String residenceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID requestId = UUID.randomUUID();
                
                TeleportRequest request = new TeleportRequest(
                        requestId,
                        player.getName(),
                        player.getUniqueId(),
                        residenceName,
                        plugin.getPluginConfig().getServerId()
                );
                
                // 发送到所有服务器查询
                broadcastMessage("TELEPORT_REQUEST", gson.toJsonTree(request).getAsJsonObject());
                
                // 等待响应（简化实现）
                return true;
                
            } catch (Exception e) {
                LoggerUtil.severe("请求跨服传送失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 广播消息到所有服务器
     */
    private void broadcastMessage(String messageType, JsonObject data) {
        for (String serverName : connectedServers.keySet()) {
            sendCrossServerMessage(serverName, messageType, data);
        }
    }
    
    /**
     * 获取连接状态
     */
    public Map<String, ServerInfo> getConnectedServers() {
        return new HashMap<>(connectedServers);
    }
    
    /**
     * 检查是否有其他服务器连接
     */
    public boolean hasConnectedServers() {
        return !connectedServers.isEmpty();
    }
    
    // 内部数据类
    
    /**
     * 服务器信息类
     */
    public static class ServerInfo {
        public String serverName;
        public String serverId;
        public long lastSeen;
        public int playerCount;
        
        public ServerInfo() {}
        
        public ServerInfo(String serverName, String serverId) {
            this.serverName = serverName;
            this.serverId = serverId;
            this.lastSeen = System.currentTimeMillis();
        }
    }
    
    /**
     * 传送请求类
     */
    public static class TeleportRequest {
        public UUID requestId;
        public String sourcePlayerName;
        public UUID sourcePlayerUuid;
        public String residenceName;
        public String targetServerId;
        
        public TeleportRequest() {}
        
        public TeleportRequest(UUID requestId, String sourcePlayerName, UUID sourcePlayerUuid, 
                             String residenceName, String targetServerId) {
            this.requestId = requestId;
            this.sourcePlayerName = sourcePlayerName;
            this.sourcePlayerUuid = sourcePlayerUuid;
            this.residenceName = residenceName;
            this.targetServerId = targetServerId;
        }
    }
    
    /**
     * 传送响应类
     */
    public static class TeleportResponse {
        public UUID requestId;
        public boolean success;
        public String reason;
        
        public TeleportResponse() {}
        
        public TeleportResponse(UUID requestId, boolean success, String reason) {
            this.requestId = requestId;
            this.success = success;
            this.reason = reason;
        }
    }
}