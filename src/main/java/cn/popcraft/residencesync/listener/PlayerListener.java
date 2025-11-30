package cn.popcraft.residencesync.listener;

import cn.popcraft.residencesync.service.ResidenceService;
import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.Map;

/**
 * 玩家事件监听器
 * 
 * 处理与玩家相关的游戏事件
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class PlayerListener implements Listener {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private final ResidenceService residenceService;
    
    public PlayerListener(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
        this.residenceService = plugin.getResidenceService();
    }
    
    /**
     * 玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 更新玩家信息到数据库
        residenceService.updatePlayerInfo(player)
                .thenRun(() -> plugin.getLogger().info("已更新玩家信息: " + player.getName()));
        
        // 检查玩家是否有等待中的跨服传送
        checkPendingCrossServerTeleports(player);
    }
    
    /**
     * 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 取消玩家的活跃传送任务
        plugin.getTeleportService().cancelActiveTeleport(player);
        
        // 更新玩家最后在线时间
        residenceService.updatePlayerInfo(player);
    }
    
    /**
     * 玩家移动事件 - 用于检测传送时移动
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否正在传送
        if (plugin.getTeleportService().isPlayerTeleporting(player)) {
            // 检查移动距离是否超过了阈值
            if (isSignificantMove(event)) {
                // 取消传送
                plugin.getTeleportService().cancelActiveTeleport(player);
            }
        }
    }
    
    /**
     * 玩家重生事件
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // 取消所有传送任务
        plugin.getTeleportService().cancelActiveTeleport(player);
    }
    
    /**
     * 玩家死亡事件
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 取消所有传送任务
        plugin.getTeleportService().cancelActiveTeleport(player);
    }
    
    /**
     * 玩家尝试使用命令事件
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        
        // 检查是否是传送相关命令
        if (message.startsWith("/res tp")) {
            // 解析命令参数
            String[] parts = message.split(" ");
            if (parts.length >= 3) {
                String residenceName = parts[2];
                
                // 检查玩家是否有权限
                if (!player.hasPermission("residence.command.tp") && !player.hasPermission("residencesync.tp")) {
                    plugin.getMessageConfig().sendMessage(player, "tpPermissionCancel");
                    event.setCancelled(true);
                    return;
                }
                
                // 检查领地是否存在
                plugin.getResidenceService().findResidenceCrossServer(residenceName)
                        .thenAccept(residence -> {
                            if (residence == null) {
                                plugin.getMessageConfig().sendMessage(player, "resIsNull");
                                return;
                            }
                            
                            // 开始跨服传送流程
                            plugin.getMessageConfig().sendMessage(player, "tryTpRes", 
                                    Map.of("resName", residenceName));
                            
                            plugin.getTeleportService().initiateCrossServerTeleport(player, residence);
                        });
                
                event.setCancelled(true); // 取消原始命令，让插件处理
            }
        }
        
        // 检查是否是领地创建命令
        if (message.startsWith("/res create")) {
            // 检查玩家的领地数量限制
            plugin.getResidenceService().canPlayerCreateResidence(player)
                    .thenAccept(canCreate -> {
                        if (!canCreate) {
                            plugin.getMessageConfig().sendMessage(player, "createFailIsMax");
                        }
                    });
        }
    }
    
    /**
     * 检查是否有显著的移动
     */
    private boolean isSignificantMove(PlayerMoveEvent event) {
        // 计算移动距离
        double distance = event.getFrom().distance(event.getTo());
        
        // 设置移动阈值（方块数）
        double threshold = 0.5;
        
        return distance > threshold;
    }
    
    /**
     * 检查等待中的跨服传送
     */
    private void checkPendingCrossServerTeleports(Player player) {
        // 这里应该检查是否有等待中的跨服传送请求
        // 实际实现需要与跨服服务配合
        
        LoggerUtil.debug("检查玩家 " + player.getName() + " 的等待传送");
    }
}