package cn.popcraft.residencesync.service;

import cn.popcraft.residencesync.database.DatabaseManager;
import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 领地服务类
 * 
 * 提供领地相关的业务逻辑处理
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class ResidenceService {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private final DatabaseManager databaseManager;
    
    public ResidenceService(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }
    
    /**
     * 获取玩家的所有领地（跨服）
     */
    public CompletableFuture<List<ResidenceData>> getPlayerResidences(Player player) {
        return databaseManager.getPlayerResidences(player.getUniqueId());
    }
    
    /**
     * 获取玩家的领地数量
     */
    public CompletableFuture<Integer> getPlayerResidenceCount(Player player) {
        return getPlayerResidences(player).thenApply(List::size);
    }
    
    /**
     * 检查玩家是否拥有指定领地
     */
    public CompletableFuture<Boolean> playerOwnsResidence(Player player, String residenceName) {
        return getPlayerResidences(player).thenApply(residences -> 
            residences.stream().anyMatch(res -> res.getName().equalsIgnoreCase(residenceName))
        );
    }
    
    /**
     * 根据名称和服务器ID获取领地
     */
    public CompletableFuture<ResidenceData> getResidence(String residenceName, String serverId) {
        return databaseManager.getResidence(residenceName, serverId);
    }
    
    /**
     * 跨服查找领地
     */
    public CompletableFuture<ResidenceData> findResidenceCrossServer(String residenceName) {
        // 首先在本地服务器查找
        return getResidence(residenceName, plugin.getPluginConfig().getServerId())
                .thenCompose(residence -> {
                    if (residence != null) {
                        return CompletableFuture.completedFuture(residence);
                    }
                    
                    // 如果本地没有，在其他服务器查找
                    // 这里需要实现跨服通信逻辑
                    LoggerUtil.info("正在跨服查找领地: " + residenceName);
                    return findResidenceInOtherServers(residenceName);
                });
    }
    
    /**
     * 在其他服务器查找领地
     */
    private CompletableFuture<ResidenceData> findResidenceInOtherServers(String residenceName) {
        // TODO: 实现跨服通信查找逻辑
        // 这里应该通过BungeeCord或者Redis等方式查询其他服务器
        LoggerUtil.info("在其他服务器中查找领地: " + residenceName);
        
        // 暂时返回null，实际实现时需要查询其他服务器
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 检查玩家是否可以创建新领地
     */
    public CompletableFuture<Boolean> canPlayerCreateResidence(Player player) {
        return getPlayerResidenceCount(player).thenCompose(count -> {
            int limit = plugin.getPluginConfig().getPlayerResidenceLimit(player);
            return CompletableFuture.completedFuture(count < limit);
        });
    }
    
    /**
     * 添加新领地
     */
    public CompletableFuture<Boolean> addResidence(ResidenceData residence) {
        return databaseManager.addOrUpdateResidence(residence);
    }
    
    /**
     * 删除领地
     */
    public CompletableFuture<Boolean> deleteResidence(String residenceName, String serverId) {
        return databaseManager.deleteResidence(residenceName, serverId);
    }
    
    /**
     * 检查玩家是否有权限操作指定领地
     */
    public CompletableFuture<Boolean> hasResidencePermission(Player player, String residenceName, String permission) {
        return playerOwnsResidence(player, residenceName).thenCompose(owns -> {
            if (owns) {
                return CompletableFuture.completedFuture(true);
            }
            
            // 检查是否有管理员权限
            if (player.hasPermission("residencesync.admin")) {
                return CompletableFuture.completedFuture(true);
            }
            
            // 检查是否有强制操作权限
            if (permission.equals("force") && player.hasPermission("residencesync.force")) {
                return CompletableFuture.completedFuture(true);
            }
            
            return CompletableFuture.completedFuture(false);
        });
    }
    
    /**
     * 获取玩家的可操作领地列表
     */
    public CompletableFuture<List<String>> getPlayerAccessibleResidenceNames(Player player) {
        return getPlayerResidences(player).thenApply(residences -> 
            residences.stream()
                    .map(ResidenceData::getName)
                    .sorted()
                    .toList()
        );
    }
    
    /**
     * 验证领地传送位置的安全性
     */
    public boolean isTeleportLocationSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        // 检查位置是否为空气方块且脚下有实体方块
        Location checkLocation = location.clone();
        
        // 检查传送位置的方块是否为空气
        if (!checkLocation.getBlock().getType().isAir()) {
            return false;
        }
        
        // 检查脚下是否有实体方块
        Location belowLocation = checkLocation.clone().subtract(0, 1, 0);
        if (belowLocation.getBlock().getType().isAir()) {
            return false;
        }
        
        // 检查头顶空间
        Location aboveLocation = checkLocation.clone().add(0, 2, 0);
        for (int y = 0; y <= 2; y++) {
            Location checkY = checkLocation.clone().add(0, y, 0);
            if (!checkY.getBlock().getType().isAir()) {
                return false;
            }
        }
        
        // 检查附近是否有岩浆或危险方块
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location nearbyLocation = location.clone().add(x, y, z);
                    if (nearbyLocation.getBlock().getType().name().contains("LAVA") ||
                        nearbyLocation.getBlock().getType().name().contains("FIRE")) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 获取最近的可用传送点
     */
    public Location getNearestSafeLocation(Location location, int maxRadius) {
        if (isTeleportLocationSafe(location)) {
            return location;
        }
        
        // 搜索附近的可用位置
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 2; y++) {
                        Location checkLocation = location.clone().add(x, y, z);
                        if (isTeleportLocationSafe(checkLocation)) {
                            return checkLocation;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 更新玩家在数据库中的信息
     */
    public CompletableFuture<Boolean> updatePlayerInfo(Player player) {
        return databaseManager.updatePlayer(player.getUniqueId(), player.getName());
    }
    
    /**
     * 获取玩家名称
     */
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return CompletableFuture.completedFuture(player.getName());
        }
        
        return databaseManager.getPlayerName(uuid);
    }
    
    /**
     * 验证领地名称是否有效
     */
    public boolean isValidResidenceName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String trimmedName = name.trim();
        
        // 检查长度
        if (trimmedName.length() > 30) {
            return false;
        }
        
        // 检查是否包含非法字符
        return trimmedName.matches("^[a-zA-Z0-9_一-龯]+$");
    }
    
    /**
     * 格式化领地坐标范围显示
     */
    public String formatResidenceArea(ResidenceData residence) {
        return String.format("(%s: %s, %s, %s) 到 (%s: %s, %s, %s)",
                residence.getWorld(),
                Math.floor(residence.getX1()), Math.floor(residence.getY1()), Math.floor(residence.getZ1()),
                residence.getWorld(),
                Math.floor(residence.getX2()), Math.floor(residence.getY2()), Math.floor(residence.getZ2()));
    }
}