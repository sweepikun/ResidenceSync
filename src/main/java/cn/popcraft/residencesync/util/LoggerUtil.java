package cn.popcraft.residencesync.util;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 日志工具类
 * 
 * 提供统一的日志输出功能，支持不同级别的日志记录
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class LoggerUtil {
    
    private static JavaPlugin plugin;
    
    /**
     * 初始化日志工具
     */
    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * 记录信息级别日志
     */
    public static void info(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        } else {
            System.out.println("[INFO] " + message);
        }
    }
    
    /**
     * 记录警告级别日志
     */
    public static void warning(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        } else {
            System.err.println("[WARNING] " + message);
        }
    }
    
    /**
     * 记录严重错误级别日志
     */
    public static void severe(String message) {
        if (plugin != null) {
            plugin.getLogger().severe(message);
        } else {
            System.err.println("[SEVERE] " + message);
        }
    }
    
    /**
     * 记录严重错误级别日志（带异常）
     */
    public static void severe(String message, Throwable throwable) {
        if (plugin != null) {
            plugin.getLogger().severe(message, throwable);
        } else {
            System.err.println("[SEVERE] " + message);
            throwable.printStackTrace();
        }
    }
    
    /**
     * 记录调试级别日志
     */
    public static void debug(String message) {
        if (plugin != null && plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        } else {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    /**
     * 记录成功操作日志
     */
    public static void success(String message) {
        if (plugin != null) {
            plugin.getLogger().info("§a[SUCCESS] " + message);
        } else {
            System.out.println("[SUCCESS] " + message);
        }
    }
    
    /**
     * 记录失败操作日志
     */
    public static void failure(String message) {
        if (plugin != null) {
            plugin.getLogger().warning("§c[FAILURE] " + message);
        } else {
            System.err.println("[FAILURE] " + message);
        }
    }
}