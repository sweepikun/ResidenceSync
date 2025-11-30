package cn.popcraft.residencesync.listener;

import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.util.LoggerUtil;
import com.bekvon.residence.Residence;
import com.bekvon.residence.protection.ClaimedResidence;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 领地事件监听器
 * 
 * 处理与Residence插件相关的事件，监听领地变化并进行同步
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class ResidenceListener implements Listener {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    
    public ResidenceListener(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听领地交互事件，用于同步领地数据
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 这里可以监听玩家的交互行为来同步领地数据
        // 实际实现需要与Residence插件的API进行集成
    }
    
    /**
     * 同步单个领地数据到数据库
     */
    private void syncResidenceData(ClaimedResidence residence) {
        try {
            if (residence == null) {
                return;
            }
            
            String residenceName = residence.getName();
            String ownerUuid = residence.getOwner().toString();
            String serverId = plugin.getPluginConfig().getServerId();
            String world = residence.getWorld().getName();
            
            // 获取领地边界
            double x1 = residence.getX1();
            double y1 = residence.getY1();
            double z1 = residence.getZ1();
            double x2 = residence.getX2();
            double y2 = residence.getY2();
            double z2 = residence.getZ2();
            
            // 创建ResidenceData对象
            ResidenceData residenceData = new ResidenceData(
                    residenceName,
                    ownerUuid,
                    serverId,
                    world,
                    x1, y1, z1,
                    x2, y2, z2,
                    new java.sql.Timestamp(System.currentTimeMillis()),
                    new java.sql.Timestamp(System.currentTimeMillis())
            );
            
            // 同步到数据库
            plugin.getDatabaseManager().addOrUpdateResidence(residenceData)
                    .thenAccept(success -> {
                        if (success) {
                            LoggerUtil.debug("已同步领地数据: " + residenceName);
                        } else {
                            LoggerUtil.warning("同步领地数据失败: " + residenceName);
                        }
                    });
            
        } catch (Exception e) {
            LoggerUtil.severe("同步领地数据时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量同步所有领地数据
     */
    public void syncAllResidences() {
        try {
            // 检查Residence插件是否可用
            Residence residencePlugin = Residence.getInstance();
            if (residencePlugin == null) {
                LoggerUtil.warning("Residence插件未加载，无法同步数据");
                return;
            }
            
            // 获取所有领地
            java.util.Map<String, ClaimedResidence> residences = residencePlugin.getResidenceManager().getResidences();
            
            if (residences.isEmpty()) {
                LoggerUtil.info("未找到任何领地数据");
                return;
            }
            
            LoggerUtil.info("开始同步 " + residences.size() + " 个领地数据...");
            
            // 逐个同步领地数据
            int successCount = 0;
            int failCount = 0;
            
            for (ClaimedResidence residence : residences.values()) {
                try {
                    syncSingleResidence(residence);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    LoggerUtil.warning("同步领地失败: " + residence.getName() + " - " + e.getMessage());
                }
            }
            
            LoggerUtil.info("数据同步完成: 成功 " + successCount + " 个，失败 " + failCount + " 个");
            
        } catch (Exception e) {
            LoggerUtil.severe("批量同步领地数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步单个领地数据
     */
    private void syncSingleResidence(ClaimedResidence residence) {
        try {
            String residenceName = residence.getName();
            String ownerUuid = residence.getOwner().toString();
            String serverId = plugin.getPluginConfig().getServerId();
            String world = residence.getWorld().getName();
            
            // 获取领地边界
            double x1 = residence.getX1();
            double y1 = residence.getY1();
            double z1 = residence.getZ1();
            double x2 = residence.getX2();
            double y2 = residence.getY2();
            double z2 = residence.getZ2();
            
            // 创建ResidenceData对象
            ResidenceData residenceData = new ResidenceData(
                    residenceName,
                    ownerUuid,
                    serverId,
                    world,
                    x1, y1, z1,
                    x2, y2, z2,
                    new java.sql.Timestamp(System.currentTimeMillis()),
                    new java.sql.Timestamp(System.currentTimeMillis())
            );
            
            // 同步到数据库
            plugin.getDatabaseManager().addOrUpdateResidence(residenceData).join();
            
        } catch (Exception e) {
            throw new RuntimeException("同步单个领地失败: " + residence.getName(), e);
        }
    }
    
    /**
     * 清理已删除的领地数据
     */
    public void cleanupRemovedResidences() {
        try {
            // 这里需要实现与本地Residence插件的对比，找出已删除的领地
            // 并从数据库中清理
            
            LoggerUtil.debug("开始清理已删除的领地数据");
            
            // TODO: 实现清理逻辑
            
        } catch (Exception e) {
            LoggerUtil.severe("清理已删除领地数据失败: " + e.getMessage(), e);
        }
    }
}