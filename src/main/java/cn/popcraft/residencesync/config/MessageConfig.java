package cn.popcraft.residencesync.config;

import cn.popcraft.residencesync.util.LoggerUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息配置文件管理类
 * 
 * 管理插件的多语言消息配置，支持 RGB 颜色代码
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class MessageConfig {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private File messageFile;
    private FileConfiguration messageConfig;
    
    // RGB颜色代码模式
    private static final Pattern RGB_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public MessageConfig(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
        this.messageFile = new File(plugin.getDataFolder(), "message_zh.yml");
    }
    
    /**
     * 加载消息配置文件
     */
    public void loadConfig() {
        if (!messageFile.exists()) {
            plugin.getDataFolder().mkdirs();
            createDefaultMessages();
        }
        
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        LoggerUtil.info("消息配置文件已加载");
    }
    
    /**
     * 创建默认消息配置
     */
    private void createDefaultMessages() {
        messageConfig = new YamlConfiguration();
        
        // 插件标题
        messageConfig.set("PluginTitle", "&7|&e⭐&#C0FF3E领地跨服同步&e⭐&7|");
        
        // 传送相关消息
        messageConfig.set("tryTpRes", "%PluginTitle% 您正在尝试传送至领地 %resName%");
        messageConfig.set("tryFindOtherServerRes", "%PluginTitle% 正在尝试在其他服务器中寻找领地");
        messageConfig.set("notPermissionTp", "%PluginTitle% 您没有权限传送到 %resName% 领地");
        messageConfig.set("targetLocationIsNull", "%PluginTitle% 目的地不安全，无法传送");
        messageConfig.set("playerTpSuccess", "%PluginTitle% 您已传送至领地 %resName%");
        messageConfig.set("tpPermissionCancel", "%PluginTitle% 您没有 residence.command.tp 权限");
        
        // 领地操作消息
        messageConfig.set("renameUsage", "%PluginTitle% 用法：/res rename [旧领地名字] [新领地名字]");
        messageConfig.set("resIsNull", "%PluginTitle% 领地不存在或请前往对应的服务器修改");
        messageConfig.set("noIsOwner", "%PluginTitle% 您不是该领地的主人");
        messageConfig.set("giveUsage", "%PluginTitle% 用法：/res give [领地名字] [玩家名字]");
        messageConfig.set("giveSuccess", "%PluginTitle% 领地 %resName% 给予成功，新的领地主人为 %targetPlayer%");
        messageConfig.set("renameSuccess", "%PluginTitle% 领地 %resName% 改名成功，新的领地名为 %newResName%");
        messageConfig.set("renameCancel", "%PluginTitle% 新的领地名已存在，改名被取消");
        messageConfig.set("givePermissionCancel", "%PluginTitle% 您没有 residence.command.give 权限");
        
        // 创建领地消息
        messageConfig.set("createSuccess", "%PluginTitle% 领地 %resName% 创建成功");
        messageConfig.set("createFailNoMoney", "%PluginTitle% 创建失败：资金不足");
        messageConfig.set("createFailIsMax", "%PluginTitle% 创建失败：您的领地数量已达到最大上限");
        messageConfig.set("createFailNoEmpty", "%PluginTitle% 创建失败：已存在名为 %resName% 的领地");
        
        // 权限相关消息
        messageConfig.set("noIsOp", "%PluginTitle% 您不是管理员");
        messageConfig.set("playerIsOffline", "%PluginTitle% 玩家：%targetPlayer% 不在线");
        messageConfig.set("playerIsNull", "%PluginTitle% 玩家 %player% 不存在");
        
        // 传送倒计时消息
        messageConfig.set("countDown", "%PluginTitle% 传送倒计时 %time% 秒，请勿移动");
        messageConfig.set("countDownMoveCancel", "%PluginTitle% 您在传送时移动，传送已取消");
        
        // 控制台消息
        messageConfig.set("consoleReject", "%PluginTitle% 此命令仅限控制台使用");
        
        // 管理员命令消息
        messageConfig.set("renameResLinkUsage", "%PluginTitle% 用法：/ResidenceSync rename [旧领地名字] [新领地名字]");
        messageConfig.set("giveResLinkUsage", "%PluginTitle% 用法：/ResidenceSync give [领地名字] [玩家名字]");
        messageConfig.set("getuuidResLinkUsage", "%PluginTitle% 用法：/ResidenceSync getuuid [玩家名字]");
        
        // 领地列表消息
        messageConfig.set("listResTableIsNull", "%PluginTitle% 玩家 %player% 没有领地");
        
        // 领地列表格式
        List<String> listFormat = new ArrayList<>();
        listFormat.add("&#ccffbb玩家 %player% 的领地列表");
        listFormat.add("&l&#7472a5————————————");
        listFormat.add("&#ccffbb - %resname%");
        listFormat.add("&l&#7472a5————————————");
        messageConfig.set("ListResTable", listFormat);
        
        // 帮助信息标题
        List<String> helpTitle = new ArrayList<>();
        helpTitle.add("&l&#7472a5——————&r&7【&e⭐&#C0FF3E领地跨服同步&e⭐&7】&l&#7472a5——————");
        helpTitle.add("&#ccffbb/ResidenceSync reload - 重载配置文件");
        helpTitle.add("&#ccffbb/ResidenceSync rename [旧领地名字] [新领地名字] - 管理员给玩家领地改名");
        helpTitle.add("&#ccffbb/ResidenceSync give [领地名字] [玩家名字] - 管理员将某个领地强制给予某个玩家");
        helpTitle.add("&#ccffbb/ResidenceSync getuuid [玩家名字] - 管理员获取玩家UUID");
        helpTitle.add("&#ccffbb/ResidenceSync save - 立刻进行一次当前子服的所有数据的重新载入数据库");
        messageConfig.set("HelpTitle", helpTitle);
        
        saveMessages();
    }
    
    /**
     * 保存消息配置到文件
     */
    public void saveMessages() {
        try {
            messageConfig.save(messageFile);
        } catch (IOException e) {
            LoggerUtil.severe("无法保存消息配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 重新加载消息配置
     */
    public void reloadMessages() {
        loadConfig();
    }
    
    /**
     * 获取格式化后的消息
     * 
     * @param path 消息路径
     * @param placeholders 占位符替换
     * @return 格式化后的消息
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messageConfig.getString(path, "");
        
        if (message.isEmpty()) {
            return "消息未找到: " + path;
        }
        
        // 替换占位符
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        // 替换标题占位符
        String title = messageConfig.getString("PluginTitle", "&7[ResidenceSync]&7|");
        message = message.replace("%PluginTitle%", title);
        
        // 处理 RGB 颜色代码
        message = processRGBColors(message);
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取格式化后的消息（简单版本）
     * 
     * @param path 消息路径
     * @return 格式化后的消息
     */
    public String getMessage(String path) {
        return getMessage(path, null);
    }
    
    /**
     * 处理 RGB 颜色代码
     * 
     * @param message 原始消息
     * @return 处理后的消息
     */
    private String processRGBColors(String message) {
        Matcher matcher = RGB_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String rgbCode = matcher.group(1);
            String replacement = "&#" + rgbCode;
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 获取多行消息列表
     * 
     * @param path 消息路径
     * @return 消息列表
     */
    public List<String> getMessageList(String path) {
        List<String> messages = messageConfig.getStringList(path);
        List<String> formattedMessages = new ArrayList<>();
        
        for (String message : messages) {
            formattedMessages.add(getMessage(message));
        }
        
        return formattedMessages;
    }
    
    /**
     * 发送消息给玩家
     * 
     * @param player 目标玩家
     * @param path 消息路径
     * @param placeholders 占位符替换
     */
    public void sendMessage(org.bukkit.entity.Player player, String path, Map<String, String> placeholders) {
        if (player != null) {
            player.sendMessage(getMessage(path, placeholders));
        }
    }
    
    /**
     * 发送消息给玩家（简单版本）
     * 
     * @param player 目标玩家
     * @param path 消息路径
     */
    public void sendMessage(org.bukkit.entity.Player player, String path) {
        sendMessage(player, path, null);
    }
    
    /**
     * 发送多行消息给玩家
     * 
     * @param player 目标玩家
     * @param path 消息路径
     */
    public void sendMessageList(org.bukkit.entity.Player player, String path) {
        if (player != null) {
            List<String> messages = getMessageList(path);
            for (String message : messages) {
                player.sendMessage(message);
            }
        }
    }
}