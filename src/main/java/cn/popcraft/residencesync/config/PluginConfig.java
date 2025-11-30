package cn.popcraft.residencesync.config;

import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 插件配置管理类
 * 
 * 管理插件的配置文件，包括数据库配置、权限配置等
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class PluginConfig {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    
    // 数据库配置
    private String databaseHost = "localhost";
    private int databasePort = 3306;
    private String databaseName = "residencesync";
    private String databaseUsername = "root";
    private String databasePassword = "password";
    
    // 服务器配置
    private String serverId = "";
    
    // 默认设置
    private boolean defaultCreateTpFlag = false;
    
    // 权限配置
    private Map<String, Integer> tpPermissions = new HashMap<>();
    private Map<String, Integer> countPermissions = new HashMap<>();
    
    public PluginConfig(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            createDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDatabaseConfig();
        loadServerConfig();
        loadPermissionConfig();
        loadSettings();
        
        LoggerUtil.info("插件配置已加载");
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        config = new YamlConfiguration();
        
        // 数据库配置
        config.set("database.host", databaseHost);
        config.set("database.port", databasePort);
        config.set("database.name", databaseName);
        config.set("database.username", databaseUsername);
        config.set("database.password", databasePassword);
        
        // 服务器配置
        config.set("serverId", serverId);
        
        // 设置
        config.set("settings.defaultCreateTpFlag", defaultCreateTpFlag);
        config.set("settings.language", "message_zh.yml");
        
        // 权限配置 - 传送时间
        config.set("permission.tp.ResLinkDefault", 3);
        config.set("permission.tp.ResLinkVIP1", 2);
        config.set("permission.tp.ResLinkVIP2", 0);
        
        // 权限配置 - 领地数量
        config.set("permission.count.ResLinkDefaultCount", 3);
        config.set("permission.count.ResLinkCountVIP1", 5);
        config.set("permission.count.ResLinkCountVIP2", 10);
        
        saveConfig();
    }
    
    /**
     * 加载数据库配置
     */
    private void loadDatabaseConfig() {
        databaseHost = config.getString("database.host", "localhost");
        databasePort = config.getInt("database.port", 3306);
        databaseName = config.getString("database.name", "residencesync");
        databaseUsername = config.getString("database.username", "root");
        databasePassword = config.getString("database.password", "password");
    }
    
    /**
     * 加载服务器配置
     */
    private void loadServerConfig() {
        serverId = config.getString("serverId", "");
        if (serverId.isEmpty()) {
            LoggerUtil.warning("未配置服务器ID，跨服功能可能无法正常工作");
        }
    }
    
    /**
     * 加载权限配置
     */
    private void loadPermissionConfig() {
        // 加载传送时间权限
        tpPermissions.clear();
        if (config.contains("permission.tp")) {
            for (String key : config.getConfigurationSection("permission.tp").getKeys(false)) {
                tpPermissions.put(key, config.getInt("permission.tp." + key));
            }
        }
        
        // 加载领地数量权限
        countPermissions.clear();
        if (config.contains("permission.count")) {
            for (String key : config.getConfigurationSection("permission.count").getKeys(false)) {
                countPermissions.put(key, config.getInt("permission.count." + key));
            }
        }
    }
    
    /**
     * 加载其他设置
     */
    private void loadSettings() {
        defaultCreateTpFlag = config.getBoolean("settings.defaultCreateTpFlag", false);
    }
    
    /**
     * 保存配置到文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            LoggerUtil.severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    // Getters
    
    public String getDatabaseHost() {
        return databaseHost;
    }
    
    public int getDatabasePort() {
        return databasePort;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public String getDatabaseUsername() {
        return databaseUsername;
    }
    
    public String getDatabasePassword() {
        return databasePassword;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public boolean isDefaultCreateTpFlag() {
        return defaultCreateTpFlag;
    }
    
    /**
     * 获取玩家的传送延迟时间（秒）
     */
    public int getPlayerTeleportDelay(org.bukkit.entity.Player player) {
        int minDelay = Integer.MAX_VALUE;
        
        for (Map.Entry<String, Integer> entry : tpPermissions.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                minDelay = Math.min(minDelay, entry.getValue());
            }
        }
        
        // 如果没有匹配到任何权限，使用默认值
        if (minDelay == Integer.MAX_VALUE) {
            return tpPermissions.getOrDefault("ResLinkDefault", 3);
        }
        
        return minDelay;
    }
    
    /**
     * 获取玩家的领地数量上限
     */
    public int getPlayerResidenceLimit(org.bukkit.entity.Player player) {
        int maxLimit = 0;
        
        for (Map.Entry<String, Integer> entry : countPermissions.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                maxLimit = Math.max(maxLimit, entry.getValue());
            }
        }
        
        // 如果没有匹配到任何权限，使用默认值
        if (maxLimit == 0) {
            return countPermissions.getOrDefault("ResLinkDefaultCount", 3);
        }
        
        return maxLimit;
    }
    
    /**
     * 获取所有传送权限配置
     */
    public Map<String, Integer> getTpPermissions() {
        return new HashMap<>(tpPermissions);
    }
    
    /**
     * 获取所有领地数量权限配置
     */
    public Map<String, Integer> getCountPermissions() {
        return new HashMap<>(countPermissions);
    }
}