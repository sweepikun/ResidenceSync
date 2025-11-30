package cn.popcraft.residencesync;

import cn.popcraft.residencesync.commands.AdminCommand;
import cn.popcraft.residencesync.commands.ResidenceCommand;
import cn.popcraft.residencesync.config.PluginConfig;
import cn.popcraft.residencesync.config.MessageConfig;
import cn.popcraft.residencesync.database.DatabaseManager;
import cn.popcraft.residencesync.listener.PlayerListener;
import cn.popcraft.residencesync.listener.ResidenceListener;
import cn.popcraft.residencesync.placeholder.PlaceholderExpansion;
import cn.popcraft.residencesync.service.ResidenceService;
import cn.popcraft.residencesync.service.TeleportService;
import cn.popcraft.residencesync.service.CrossServerService;
import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 领地跨服同步插件主类
 * 
 * 该插件为 Residence 插件添加跨服功能，支持：
 * - 跨服传送
 * - 跨服领地管理
 * - 领地数量限制
 * - 传送延迟控制
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class ResidenceSyncPlugin extends JavaPlugin {
    
    private static ResidenceSyncPlugin instance;
    
    private DatabaseManager databaseManager;
    private PluginConfig pluginConfig;
    private MessageConfig messageConfig;
    private ResidenceService residenceService;
    private TeleportService teleportService;
    private CrossServerService crossServerService;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            LoggerUtil.info("正在启动领地跨服同步插件...");
            
            // 验证服务器版本
            if (!validateServerVersion()) {
                LoggerUtil.severe("插件仅支持 1.19.3 版本，请检查服务器版本");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 检查依赖插件
            if (!checkDependencies()) {
                LoggerUtil.severe("缺少必要的依赖插件，插件已禁用");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 初始化配置文件
            initializeConfigs();
            
            // 初始化数据库
            initializeDatabase();
            
            // 初始化服务
            initializeServices();
            
            // 注册事件监听器
            registerListeners();
            
            // 注册命令
            registerCommands();
            
            // 注册占位符API扩展
            registerPlaceholderAPI();
            
            LoggerUtil.info("领地跨服同步插件启动完成！");
            
        } catch (Exception e) {
            LoggerUtil.severe("插件启动时发生错误: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        LoggerUtil.info("正在关闭领地跨服同步插件...");
        
        try {
            // 关闭数据库连接
            if (databaseManager != null) {
                databaseManager.close();
            }
            
            // 关闭其他资源
            if (crossServerService != null) {
                crossServerService.cleanup();
            }
            
            LoggerUtil.info("领地跨服同步插件已关闭");
        } catch (Exception e) {
            LoggerUtil.warning("插件关闭时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 验证服务器版本
     */
    private boolean validateServerVersion() {
        String version = getServer().getVersion();
        return version.contains("1.19.3") || version.contains("1.19.4");
    }
    
    /**
     * 检查依赖插件
     */
    private boolean checkDependencies() {
        // 检查 Residence 插件
        if (getServer().getPluginManager().getPlugin("Residence") == null) {
            LoggerUtil.severe("未找到 Residence 插件！请先安装 Residence 插件");
            return false;
        }
        
        LoggerUtil.info("依赖插件检查完成");
        return true;
    }
    
    /**
     * 初始化配置文件
     */
    private void initializeConfigs() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.loadConfig();
        
        messageConfig = new MessageConfig(this);
        messageConfig.loadConfig();
        
        LoggerUtil.info("配置文件初始化完成");
    }
    
    /**
     * 初始化数据库
     */
    private void initializeDatabase() {
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            throw new RuntimeException("数据库初始化失败");
        }
        LoggerUtil.info("数据库初始化完成");
    }
    
    /**
     * 初始化服务
     */
    private void initializeServices() {
        residenceService = new ResidenceService(this);
        teleportService = new TeleportService(this);
        crossServerService = new CrossServerService(this);
        
        LoggerUtil.info("服务初始化完成");
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ResidenceListener(this), this);
        
        LoggerUtil.info("事件监听器注册完成");
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        // 管理员命令
        if (getCommand("residencesync") != null) {
            getCommand("residencesync").setExecutor(new AdminCommand(this));
        }
        
        // 拦截并处理 Residence 命令
        if (getCommand("res") != null) {
            getCommand("res").setExecutor(new ResidenceCommand(this));
        }
        
        LoggerUtil.info("命令注册完成");
    }
    
    /**
     * 注册占位符API扩展
     */
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderExpansion(this).register();
            LoggerUtil.info("占位符API扩展已注册");
        } else {
            LoggerUtil.warning("未找到 PlaceholderAPI，占位符功能将不可用");
        }
    }
    
    /**
     * 获取插件实例
     */
    public static ResidenceSyncPlugin getInstance() {
        return instance;
    }
    
    /**
     * 获取数据库管理器
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * 获取插件配置
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
    
    /**
     * 获取消息配置
     */
    public MessageConfig getMessageConfig() {
        return messageConfig;
    }
    
    /**
     * 获取领地服务
     */
    public ResidenceService getResidenceService() {
        return residenceService;
    }
    
    /**
     * 获取传送服务
     */
    public TeleportService getTeleportService() {
        return teleportService;
    }
    
    /**
     * 获取跨服服务
     */
    public CrossServerService getCrossServerService() {
        return crossServerService;
    }
}