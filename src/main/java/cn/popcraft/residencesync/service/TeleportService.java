package cn.popcraft.residencesync.service;

import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.util.LoggerUtil;
import cn.popcraft.residencesync.util.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 传送服务类
 * 
 * 处理玩家传送相关的逻辑，包括跨服传送、传送延迟等
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class TeleportService {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();
    
    public TeleportService(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 启动跨服传送流程
     */
    public CompletableFuture<Boolean> initiateCrossServerTeleport(Player player, ResidenceData residence) {
        if (residence == null) {
            plugin.getMessageConfig().sendMessage(player, "resIsNull");
            return CompletableFuture.completedFuture(false);
        }
        
        // 检查传送权限
        if (!hasTeleportPermission(player, residence)) {
            plugin.getMessageConfig().sendMessage(player, "notPermissionTp");
            return CompletableFuture.completedFuture(false);
        }
        
        // 如果领地在本服务器，直接传送
        if (residence.getServerId().equals(plugin.getPluginConfig().getServerId())) {
            return teleportToLocalResidence(player, residence);
        }
        
        // 跨服传送
        return teleportToRemoteResidence(player, residence);
    }
    
    /**
     * 检查玩家是否有传送权限
     */
    private boolean hasTeleportPermission(Player player, ResidenceData residence) {
        // 检查是否为领地所有者
        if (residence.getOwnerUuidAsUUID().equals(player.getUniqueId())) {
            return true;
        }
        
        // 检查是否有管理员权限
        if (player.hasPermission("residencesync.admin")) {
            return true;
        }
        
        // 检查是否有强制传送权限
        if (player.hasPermission("residencesync.force.tp")) {
            return true;
        }
        
        // 检查是否有residence.tp权限
        return player.hasPermission("residence.command.tp");
    }
    
    /**
     * 传送到本地领地
     */
    private CompletableFuture<Boolean> teleportToLocalResidence(Player player, ResidenceData residence) {
        try {
            // 创建传送位置
            Location teleportLocation = createTeleportLocation(residence);
            if (teleportLocation == null) {
                plugin.getMessageConfig().sendMessage(player, "targetLocationIsNull");
                return CompletableFuture.completedFuture(false);
            }
            
            // 检查位置安全性
            if (!plugin.getResidenceService().isTeleportLocationSafe(teleportLocation)) {
                plugin.getMessageConfig().sendMessage(player, "targetLocationIsNull");
                return CompletableFuture.completedFuture(false);
            }
            
            // 执行传送
            performTeleport(player, teleportLocation);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            LoggerUtil.severe("本地传送失败: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 传送到远程服务器领地
     */
    private CompletableFuture<Boolean> teleportToRemoteResidence(Player player, ResidenceData residence) {
        try {
            // 显示寻找其他服务器的消息
            plugin.getMessageConfig().sendMessage(player, "tryFindOtherServerRes");
            
            // 这里需要实现跨服通信逻辑
            // 暂时模拟跨服传送过程
            return processRemoteServerTeleport(player, residence);
            
        } catch (Exception e) {
            LoggerUtil.severe("远程传送失败: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 处理远程服务器传送
     */
    private CompletableFuture<Boolean> processRemoteServerTeleport(Player player, ResidenceData residence) {
        // TODO: 实现真正的跨服通信
        // 这里应该通过BungeeCord或Redis将玩家传送到对应服务器
        
        LoggerUtil.info("模拟跨服传送玩家 " + player.getName() + " 到服务器 " + residence.getServerId());
        
        // 模拟跨服传送延迟
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟网络延迟
                Thread.sleep(1000);
                
                // 在实际实现中，这里应该发送跨服传送请求
                // 并处理返回结果
                
                LoggerUtil.info("跨服传送完成");
                return true;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }
    
    /**
     * 执行传送（包含延迟逻辑）
     */
    private void performTeleport(Player player, Location location) {
        int delay = plugin.getPluginConfig().getPlayerTeleportDelay(player);
        
        if (delay <= 0) {
            // 立即传送
            player.teleport(location);
            return;
        }
        
        // 开始传送倒计时
        startTeleportCountdown(player, location, delay);
    }
    
    /**
     * 开始传送倒计时
     */
    private void startTeleportCountdown(Player player, Location location, int delay) {
        // 取消之前的传送任务
        cancelActiveTeleport(player);
        
        TeleportTask task = new TeleportTask(player, location, delay);
        activeTeleports.put(player.getUniqueId(), task);
        task.start();
    }
    
    /**
     * 取消活跃的传送任务
     */
    public void cancelActiveTeleport(Player player) {
        TeleportTask task = activeTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            plugin.getMessageConfig().sendMessage(player, "countDownMoveCancel");
        }
    }
    
    /**
     * 创建传送位置
     */
    private Location createTeleportLocation(ResidenceData residence) {
        // 获取领地中心点
        Vector3D center = residence.getCenter();
        
        // 创建初始传送位置
        Location teleportLocation = new Location(
                Bukkit.getWorld(residence.getWorld()),
                center.getX(),
                center.getY(),
                center.getZ()
        );
        
        // 查找最近的可用位置
        return plugin.getResidenceService().getNearestSafeLocation(teleportLocation, 10);
    }
    
    /**
     * 传送任务内部类
     */
    private class TeleportTask {
        private final Player player;
        private final Location targetLocation;
        private final int totalDelay;
        private int remainingDelay;
        private BukkitTask countdownTask;
        private boolean cancelled = false;
        
        public TeleportTask(Player player, Location targetLocation, int totalDelay) {
            this.player = player;
            this.targetLocation = targetLocation;
            this.totalDelay = totalDelay;
            this.remainingDelay = totalDelay;
        }
        
        public void start() {
            // 显示初始消息
            plugin.getMessageConfig().sendMessage(player, "tryTpRes", 
                    Map.of("resName", "目标位置"));
            
            // 开始倒计时
            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled || !player.isOnline()) {
                        cancel();
                        cleanup();
                        return;
                    }
                    
                    // 检查玩家是否移动了
                    if (hasPlayerMoved()) {
                        cancelled = true;
                        cancel();
                        plugin.getMessageConfig().sendMessage(player, "countDownMoveCancel");
                        cleanup();
                        return;
                    }
                    
                    remainingDelay--;
                    
                    if (remainingDelay > 0) {
                        plugin.getMessageConfig().sendMessage(player, "countDown", 
                                Map.of("time", String.valueOf(remainingDelay)));
                    } else {
                        // 传送完成
                        cancel();
                        performFinalTeleport();
                        cleanup();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // 每秒执行一次
        }
        
        private boolean hasPlayerMoved() {
            // 检查玩家是否发生了显著移动
            return false; // 简化实现，实际需要记录初始位置
        }
        
        private void performFinalTeleport() {
            try {
                player.teleport(targetLocation);
                plugin.getMessageConfig().sendMessage(player, "playerTpSuccess", 
                        Map.of("resName", "目标位置"));
            } catch (Exception e) {
                LoggerUtil.severe("最终传送失败: " + e.getMessage(), e);
                plugin.getMessageConfig().sendMessage(player, "targetLocationIsNull");
            }
        }
        
        public void cancel() {
            cancelled = true;
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
            }
        }
        
        private void cleanup() {
            activeTeleports.remove(player.getUniqueId());
        }
    }
    
    /**
     * 检查玩家是否正在传送
     */
    public boolean isPlayerTeleporting(Player player) {
        return activeTeleports.containsKey(player.getUniqueId());
    }
    
    /**
     * 获取玩家的传送剩余时间
     */
    public int getRemainingTeleportTime(Player player) {
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        return task != null ? task.remainingDelay : 0;
    }
    
    /**
     * 强制取消所有传送任务
     */
    public void cancelAllTeleports() {
        for (TeleportTask task : activeTeleports.values()) {
            task.cancel();
        }
        activeTeleports.clear();
    }
}