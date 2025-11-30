package cn.popcraft.residencesync.commands;

import cn.popcraft.residencesync.config.MessageConfig;
import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.service.ResidenceService;
import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员命令类
 * 
 * 处理 /ResidenceSync 相关命令，用于插件管理和维护
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class AdminCommand implements CommandExecutor {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    
    public AdminCommand(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 显示帮助信息
            plugin.getMessageConfig().sendMessageList(sender, "HelpTitle");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "rename":
                return handleRename(sender, args);
            case "give":
                return handleGive(sender, args);
            case "getuuid":
                return handleGetUuid(sender, args);
            case "save":
                return handleSave(sender);
            case "info":
                return handleInfo(sender, args);
            case "help":
                return handleHelp(sender);
            default:
                plugin.getMessageConfig().sendMessage(sender, "renameResLinkUsage");
                return true;
        }
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        try {
            // 重载配置文件
            plugin.getPluginConfig().reloadConfig();
            plugin.getMessageConfig().reloadMessages();
            
            LoggerUtil.info("配置文件已重载");
            plugin.getMessageConfig().sendMessage(sender, "createSuccess", 
                    Map.of("resName", "配置文件重载"));
            
        } catch (Exception e) {
            LoggerUtil.severe("重载配置文件失败: " + e.getMessage(), e);
            plugin.getMessageConfig().sendMessage(sender, "createFailNoMoney", 
                    Map.of("resName", "配置文件重载"));
        }
        
        return true;
    }
    
    /**
     * 处理重命名命令
     */
    private boolean handleRename(CommandSender sender, String[] args) {
        if (args.length != 3) {
            plugin.getMessageConfig().sendMessage(sender, "renameResLinkUsage");
            return true;
        }
        
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        String oldName = args[1];
        String newName = args[2];
        
        // 验证领地名称
        if (!plugin.getResidenceService().isValidResidenceName(newName)) {
            plugin.getMessageConfig().sendMessage(sender, "createFailNoEmpty", 
                    Map.of("resName", "无效的领地名称"));
            return true;
        }
        
        // 执行重命名操作
        performRename(sender, oldName, newName);
        
        return true;
    }
    
    /**
     * 处理给予命令
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length != 3) {
            plugin.getMessageConfig().sendMessage(sender, "giveResLinkUsage");
            return true;
        }
        
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        String residenceName = args[1];
        String targetPlayerName = args[2];
        
        // 获取目标玩家
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            plugin.getMessageConfig().sendMessage(sender, "playerIsOffline", 
                    Map.of("targetPlayer", targetPlayerName));
            return true;
        }
        
        // 执行给予操作
        performGive(sender, residenceName, targetPlayer);
        
        return true;
    }
    
    /**
     * 处理获取UUID命令
     */
    private boolean handleGetUuid(CommandSender sender, String[] args) {
        if (args.length != 2) {
            plugin.getMessageConfig().sendMessage(sender, "getuuidResLinkUsage");
            return true;
        }
        
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        String playerName = args[1];
        Player player = Bukkit.getPlayer(playerName);
        
        if (player != null) {
            sender.sendMessage("§a玩家 " + playerName + " 的UUID: §e" + player.getUniqueId());
        } else {
            // 尝试从数据库获取
            plugin.getResidenceService().getPlayerName(playerName)
                    .thenAccept(uuid -> {
                        if (uuid != null) {
                            sender.sendMessage("§a玩家 " + playerName + " 的UUID: §e" + uuid);
                        } else {
                            plugin.getMessageConfig().sendMessage(sender, "playerIsNull", 
                                    Map.of("player", playerName));
                        }
                    });
        }
        
        return true;
    }
    
    /**
     * 处理保存命令
     */
    private boolean handleSave(CommandSender sender) {
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        sender.sendMessage("§a开始同步Residence插件数据...");
        
        plugin.getDatabaseManager().syncResidenceData()
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a数据同步完成！");
                    } else {
                        sender.sendMessage("§c数据同步失败！");
                    }
                });
        
        return true;
    }
    
    /**
     * 处理信息命令
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§c用法: /ResidenceSync info <玩家名称|领地名称>");
            return true;
        }
        
        if (!hasPermission(sender, "residencesync.admin")) {
            plugin.getMessageConfig().sendMessage(sender, "noIsOp");
            return true;
        }
        
        String query = args[1];
        
        // 首先尝试作为玩家名称查询
        Player player = Bukkit.getPlayer(query);
        if (player != null) {
            showPlayerInfo(sender, player);
            return true;
        }
        
        // 然后尝试作为领地名称查询
        showResidenceInfo(sender, query);
        
        return true;
    }
    
    /**
     * 处理帮助命令
     */
    private boolean handleHelp(CommandSender sender) {
        plugin.getMessageConfig().sendMessageList(sender, "HelpTitle");
        return true;
    }
    
    /**
     * 执行重命名操作
     */
    private void performRename(CommandSender sender, String oldName, String newName) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        plugin.getDatabaseManager().getResidence(oldName, serverId)
                .thenCompose(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(sender, "resIsNull");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 检查新名称是否已存在
                    return plugin.getDatabaseManager().getResidence(newName, serverId)
                            .thenCompose(existingResidence -> {
                                if (existingResidence != null) {
                                    plugin.getMessageConfig().sendMessage(sender, "renameCancel");
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                // 更新领地信息
                                ResidenceData updatedResidence = new ResidenceData(
                                        newName,
                                        residence.getOwnerUuid(),
                                        residence.getServerId(),
                                        residence.getWorld(),
                                        residence.getX1(),
                                        residence.getY1(),
                                        residence.getZ1(),
                                        residence.getX2(),
                                        residence.getY2(),
                                        residence.getZ2(),
                                        residence.getCreationTime(),
                                        residence.getLastModified()
                                );
                                
                                // 删除旧记录，添加新记录
                                return plugin.getDatabaseManager().deleteResidence(oldName, serverId)
                                        .thenCompose(deleteSuccess -> {
                                            if (!deleteSuccess) {
                                                plugin.getMessageConfig().sendMessage(sender, "createFailNoMoney", 
                                                        Map.of("resName", "删除原领地"));
                                                return CompletableFuture.completedFuture(false);
                                            }
                                            
                                            return plugin.getDatabaseManager().addOrUpdateResidence(updatedResidence)
                                                    .thenCompose(addSuccess -> {
                                                        if (addSuccess) {
                                                            plugin.getMessageConfig().sendMessage(sender, "renameSuccess", 
                                                                    Map.of("resName", oldName, "newResName", newName));
                                                        } else {
                                                            plugin.getMessageConfig().sendMessage(sender, "createFailNoMoney", 
                                                                    Map.of("resName", "添加新领地"));
                                                        }
                                                        return CompletableFuture.completedFuture(addSuccess);
                                                    });
                                        });
                            });
                });
    }
    
    /**
     * 执行给予操作
     */
    private void performGive(CommandSender sender, String residenceName, Player targetPlayer) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        plugin.getDatabaseManager().getResidence(residenceName, serverId)
                .thenCompose(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(sender, "resIsNull");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 更新领地所有者
                    ResidenceData updatedResidence = new ResidenceData(
                            residence.getName(),
                            targetPlayer.getUniqueId().toString(),
                            residence.getServerId(),
                            residence.getWorld(),
                            residence.getX1(),
                            residence.getY1(),
                            residence.getZ1(),
                            residence.getX2(),
                            residence.getY2(),
                            residence.getZ2(),
                            residence.getCreationTime(),
                            residence.getLastModified()
                    );
                    
                    return plugin.getDatabaseManager().addOrUpdateResidence(updatedResidence)
                            .thenCompose(success -> {
                                if (success) {
                                    plugin.getMessageConfig().sendMessage(sender, "giveSuccess", 
                                            Map.of("resName", residenceName, "targetPlayer", targetPlayer.getName()));
                                } else {
                                    plugin.getMessageConfig().sendMessage(sender, "createFailNoMoney", 
                                            Map.of("resName", "给予领地"));
                                }
                                return CompletableFuture.completedFuture(success);
                            });
                });
    }
    
    /**
     * 显示玩家信息
     */
    private void showPlayerInfo(CommandSender sender, Player player) {
        sender.sendMessage("§6========== 玩家信息 ==========");
        sender.sendMessage("§7玩家名称: §e" + player.getName());
        sender.sendMessage("§7UUID: §e" + player.getUniqueId());
        sender.sendMessage("§7等级: §e" + player.getLevel());
        sender.sendMessage("§7游戏模式: §e" + player.getGameMode());
        
        // 显示领地信息
        plugin.getResidenceService().getPlayerResidenceCount(player)
                .thenAccept(count -> {
                    sender.sendMessage("§7领地数量: §e" + count);
                    
                    plugin.getResidenceService().getPlayerAccessibleResidenceNames(player)
                            .thenAccept(residenceNames -> {
                                if (!residenceNames.isEmpty()) {
                                    sender.sendMessage("§7领地列表: " + String.join(", ", residenceNames));
                                }
                            });
                });
    }
    
    /**
     * 显示领地信息
     */
    private void showResidenceInfo(CommandSender sender, String residenceName) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        plugin.getDatabaseManager().getResidence(residenceName, serverId)
                .thenAccept(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(sender, "resIsNull");
                        return;
                    }
                    
                    sender.sendMessage("§6========== 领地信息 ==========");
                    sender.sendMessage("§7领地名称: §e" + residence.getName());
                    sender.sendMessage("§7服务器ID: §e" + residence.getServerId());
                    sender.sendMessage("§7世界: §e" + residence.getWorld());
                    sender.sendMessage("§7所有者UUID: §e" + residence.getOwnerUuid());
                    sender.sendMessage("§7区域: " + plugin.getResidenceService().formatResidenceArea(residence));
                    
                    plugin.getResidenceService().getPlayerName(residence.getOwnerUuidAsUUID())
                            .thenAccept(playerName -> {
                                if (playerName != null) {
                                    sender.sendMessage("§7所有者名称: §e" + playerName);
                                }
                            });
                });
    }
    
    /**
     * 检查权限
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            return ((Player) sender).hasPermission(permission);
        } else {
            return true; // 控制台默认有权限
        }
    }
}