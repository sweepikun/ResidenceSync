package cn.popcraft.residencesync.placeholder;

import cn.popcraft.residencesync.service.ResidenceService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * PlaceholderAPI扩展类
 * 
 * 提供自定义占位符，用于在聊天、计分板等地方显示领地相关信息
 * 
 * 支持的占位符：
 * - %residencesync_ressize% - 玩家拥有的领地数量
 * - %residencesync_reslist_<索引>% - 玩家拥有的领地名称列表
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class PlaceholderExpansion extends PlaceholderExpansion {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private final ResidenceService residenceService;
    
    public PlaceholderExpansion(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
        this.residenceService = plugin.getResidenceService();
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "residencesync";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "MiniMax Agent";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_");
        
        if (parts.length == 0) {
            return null;
        }
        
        switch (parts[0].toLowerCase()) {
            case "ressize":
                return handleResidenceSize(player);
                
            case "reslist":
                return handleResidenceList(player, parts);
                
            case "rescount":
                return handleResidenceCount(player);
                
            case "reslimit":
                return handleResidenceLimit(player);
                
            case "serverid":
                return plugin.getPluginConfig().getServerId();
                
            case "totalplayers":
                return handleTotalPlayers();
                
            default:
                return null;
        }
    }
    
    /**
     * 处理领地数量占位符
     */
    private String handleResidenceSize(OfflinePlayer player) {
        if (player == null || !player.hasPlayedBefore()) {
            return "0";
        }
        
        try {
            // 异步获取领地数量
            CompletableFuture<Integer> future = residenceService.getPlayerResidenceCount(player.getPlayer());
            // 等待结果（注意：这可能会阻塞线程）
            return future.get().toString();
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("获取玩家领地数量失败: " + e.getMessage());
            return "0";
        }
    }
    
    /**
     * 处理领地列表占位符
     */
    private String handleResidenceList(OfflinePlayer player, String[] parts) {
        if (player == null || !player.hasPlayedBefore()) {
            return "";
        }
        
        // 检查是否有索引参数
        if (parts.length < 2) {
            return "";
        }
        
        String indexStr = parts[1];
        int index;
        
        try {
            index = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            return "";
        }
        
        try {
            CompletableFuture<List<String>> future = residenceService.getPlayerAccessibleResidenceNames(player.getPlayer());
            List<String> residenceNames = future.get();
            
            if (index <= 0 || index > residenceNames.size()) {
                return "";
            }
            
            return residenceNames.get(index - 1); // 转换为0基索引
            
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("获取玩家领地列表失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 处理领地数量（简化版本）
     */
    private String handleResidenceCount(OfflinePlayer player) {
        return handleResidenceSize(player);
    }
    
    /**
     * 处理领地数量限制
     */
    private String handleResidenceLimit(OfflinePlayer player) {
        if (player == null || !player.hasPlayedBefore()) {
            return "0";
        }
        
        try {
            org.bukkit.entity.Player bukkitPlayer = player.getPlayer();
            if (bukkitPlayer == null) {
                return "0";
            }
            
            int limit = plugin.getPluginConfig().getPlayerResidenceLimit(bukkitPlayer);
            return String.valueOf(limit);
        } catch (Exception e) {
            plugin.getLogger().warning("获取玩家领地限制失败: " + e.getMessage());
            return "0";
        }
    }
    
    /**
     * 处理在线玩家总数
     */
    private String handleTotalPlayers() {
        try {
            int onlineCount = plugin.getServer().getOnlinePlayers().size();
            return String.valueOf(onlineCount);
        } catch (Exception e) {
            plugin.getLogger().warning("获取在线玩家数量失败: " + e.getMessage());
            return "0";
        }
    }
    
    /**
     * 获取玩家的所有领地信息（用于复杂占位符）
     */
    public String onRequest(OfflinePlayer player, String params, String[] args) {
        // 这个方法可以用于更复杂的占位符处理
        if (args.length == 0) {
            return null;
        }
        
        String operation = args[0];
        
        switch (operation) {
            case "detail":
                return handleResidenceDetail(player, args);
                
            case "server":
                return handleResidenceServer(player, args);
                
            default:
                return null;
        }
    }
    
    /**
     * 处理领地详情占位符
     */
    private String handleResidenceDetail(OfflinePlayer player, String[] args) {
        if (args.length < 2) {
            return "";
        }
        
        int index;
        try {
            index = Integer.parseInt(args[1]) - 1; // 转换为0基索引
        } catch (NumberFormatException e) {
            return "";
        }
        
        try {
            CompletableFuture<List<String>> future = residenceService.getPlayerAccessibleResidenceNames(player.getPlayer());
            List<String> residenceNames = future.get();
            
            if (index < 0 || index >= residenceNames.size()) {
                return "";
            }
            
            String residenceName = residenceNames.get(index);
            
            // 可以在这里返回更详细的信息，如服务器ID、位置等
            // 暂时只返回名称
            return residenceName;
            
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("获取领地详情失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 处理领地服务器信息占位符
     */
    private String handleResidenceServer(OfflinePlayer player, String[] args) {
        if (args.length < 2) {
            return "";
        }
        
        int index;
        try {
            index = Integer.parseInt(args[1]) - 1; // 转换为0基索引
        } catch (NumberFormatException e) {
            return "";
        }
        
        try {
            CompletableFuture<List<String>> future = residenceService.getPlayerAccessibleResidenceNames(player.getPlayer());
            List<String> residenceNames = future.get();
            
            if (index < 0 || index >= residenceNames.size()) {
                return "";
            }
            
            String residenceName = residenceNames.get(index);
            
            // 获取领地所在服务器
            // 这里需要从数据库查询，简化实现返回当前服务器
            return plugin.getPluginConfig().getServerId();
            
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("获取领地服务器信息失败: " + e.getMessage());
            return "";
        }
    }
}