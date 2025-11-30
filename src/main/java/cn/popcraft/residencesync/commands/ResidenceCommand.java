package cn.popcraft.residencesync.commands;

import cn.popcraft.residencesync.database.ResidenceData;
import cn.popcraft.residencesync.service.ResidenceService;
import cn.popcraft.residencesync.service.TeleportService;
import cn.popcraft.residencesync.util.LoggerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 领地命令类
 * 
 * 拦截并处理 /res 相关命令，添加跨服功能
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class ResidenceCommand implements CommandExecutor {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    
    public ResidenceCommand(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageConfig().sendMessage(sender, "consoleReject");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // 显示帮助信息
            showResidenceHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "tp":
                return handleTeleport(player, args);
            case "list":
                return handleList(player);
            case "rename":
                return handleRename(player, args);
            case "give":
                return handleGive(player, args);
            case "remove":
            case "delete":
                return handleRemove(player, args);
            default:
                // 其他命令传递给原始的Residence插件处理
                return false;
        }
    }
    
    /**
     * 处理传送命令
     */
    private boolean handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageConfig().sendMessage(player, "renameUsage");
            return true;
        }
        
        String residenceName = args[1];
        
        // 检查权限
        if (!player.hasPermission("residence.command.tp") && !player.hasPermission("residencesync.tp")) {
            plugin.getMessageConfig().sendMessage(player, "tpPermissionCancel");
            return true;
        }
        
        // 开始跨服传送流程
        startCrossServerTeleport(player, residenceName);
        
        return true;
    }
    
    /**
     * 处理领地列表命令
     */
    private boolean handleList(Player player) {
        plugin.getResidenceService().getPlayerAccessibleResidenceNames(player)
                .thenAccept(residenceNames -> {
                    if (residenceNames.isEmpty()) {
                        plugin.getMessageConfig().sendMessage(player, "listResTableIsNull", 
                                Map.of("player", player.getName()));
                    } else {
                        showResidenceList(player, residenceNames);
                    }
                });
        
        return true;
    }
    
    /**
     * 处理重命名命令
     */
    private boolean handleRename(Player player, String[] args) {
        if (args.length < 3) {
            plugin.getMessageConfig().sendMessage(player, "renameUsage");
            return true;
        }
        
        String oldName = args[1];
        String newName = args[2];
        
        // 检查权限
        if (!player.hasPermission("residence.command.rename")) {
            plugin.getMessageConfig().sendMessage(player, "noIsOp");
            return true;
        }
        
        // 验证领地所有权
        plugin.getResidenceService().playerOwnsResidence(player, oldName)
                .thenCompose(owns -> {
                    if (!owns) {
                        plugin.getMessageConfig().sendMessage(player, "noIsOwner");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 检查新名称是否有效
                    if (!plugin.getResidenceService().isValidResidenceName(newName)) {
                        plugin.getMessageConfig().sendMessage(player, "createFailNoEmpty", 
                                Map.of("resName", "无效的领地名称"));
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 执行重命名
                    return performRename(player, oldName, newName);
                });
        
        return true;
    }
    
    /**
     * 处理给予命令
     */
    private boolean handleGive(Player player, String[] args) {
        if (args.length < 3) {
            plugin.getMessageConfig().sendMessage(player, "giveUsage");
            return true;
        }
        
        String residenceName = args[1];
        String targetPlayerName = args[2];
        
        // 检查权限
        if (!player.hasPermission("residence.command.give")) {
            plugin.getMessageConfig().sendMessage(player, "givePermissionCancel");
            return true;
        }
        
        // 验证领地所有权
        plugin.getResidenceService().playerOwnsResidence(player, residenceName)
                .thenCompose(owns -> {
                    if (!owns) {
                        plugin.getMessageConfig().sendMessage(player, "noIsOwner");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 检查目标玩家是否在线
                    org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
                    if (targetPlayer == null) {
                        plugin.getMessageConfig().sendMessage(player, "playerIsOffline", 
                                Map.of("targetPlayer", targetPlayerName));
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 执行给予操作
                    return performGive(player, residenceName, targetPlayer);
                });
        
        return true;
    }
    
    /**
     * 处理删除命令
     */
    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /res remove [领地名称]");
            return true;
        }
        
        String residenceName = args[1];
        
        // 检查权限
        if (!player.hasPermission("residence.command.remove")) {
            plugin.getMessageConfig().sendMessage(player, "noIsOp");
            return true;
        }
        
        // 验证领地所有权
        plugin.getResidenceService().playerOwnsResidence(player, residenceName)
                .thenCompose(owns -> {
                    if (!owns) {
                        plugin.getMessageConfig().sendMessage(player, "noIsOwner");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 执行删除操作
                    return performRemove(player, residenceName);
                });
        
        return true;
    }
    
    /**
     * 开始跨服传送流程
     */
    private void startCrossServerTeleport(Player player, String residenceName) {
        plugin.getMessageConfig().sendMessage(player, "tryTpRes", 
                Map.of("resName", residenceName));
        
        // 查找领地
        plugin.getResidenceService().findResidenceCrossServer(residenceName)
                .thenAccept(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(player, "resIsNull");
                        return;
                    }
                    
                    // 执行传送
                    plugin.getTeleportService().initiateCrossServerTeleport(player, residence);
                });
    }
    
    /**
     * 显示领地列表
     */
    private void showResidenceList(Player player, java.util.List<String> residenceNames) {
        // 显示标题
        plugin.getMessageConfig().sendMessageList(player, "ListResTable");
        
        // 显示每个领地
        for (int i = 0; i < residenceNames.size(); i++) {
            String residenceName = residenceNames.get(i);
            String message = "&#ccffbb - " + residenceName;
            player.sendMessage(plugin.getMessageConfig().getMessage(message));
        }
    }
    
    /**
     * 执行重命名操作
     */
    private CompletableFuture<Boolean> performRename(Player player, String oldName, String newName) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        return plugin.getDatabaseManager().getResidence(oldName, serverId)
                .thenCompose(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(player, "resIsNull");
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // 检查新名称是否已存在
                    return plugin.getDatabaseManager().getResidence(newName, serverId)
                            .thenCompose(existingResidence -> {
                                if (existingResidence != null) {
                                    plugin.getMessageConfig().sendMessage(player, "renameCancel");
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
                                                plugin.getMessageConfig().sendMessage(player, "createFailNoMoney", 
                                                        Map.of("resName", "删除原领地"));
                                                return CompletableFuture.completedFuture(false);
                                            }
                                            
                                            return plugin.getDatabaseManager().addOrUpdateResidence(updatedResidence)
                                                    .thenCompose(addSuccess -> {
                                                        if (addSuccess) {
                                                            plugin.getMessageConfig().sendMessage(player, "renameSuccess", 
                                                                    Map.of("resName", oldName, "newResName", newName));
                                                        } else {
                                                            plugin.getMessageConfig().sendMessage(player, "createFailNoMoney", 
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
    private CompletableFuture<Boolean> performGive(Player player, String residenceName, org.bukkit.entity.Player targetPlayer) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        return plugin.getDatabaseManager().getResidence(residenceName, serverId)
                .thenCompose(residence -> {
                    if (residence == null) {
                        plugin.getMessageConfig().sendMessage(player, "resIsNull");
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
                                    plugin.getMessageConfig().sendMessage(player, "giveSuccess", 
                                            Map.of("resName", residenceName, "targetPlayer", targetPlayer.getName()));
                                    
                                    // 通知目标玩家
                                    plugin.getMessageConfig().sendMessage(targetPlayer, "giveSuccess", 
                                            Map.of("resName", residenceName, "targetPlayer", targetPlayer.getName()));
                                } else {
                                    plugin.getMessageConfig().sendMessage(player, "createFailNoMoney", 
                                            Map.of("resName", "给予领地"));
                                }
                                return CompletableFuture.completedFuture(success);
                            });
                });
    }
    
    /**
     * 执行删除操作
     */
    private CompletableFuture<Boolean> performRemove(Player player, String residenceName) {
        String serverId = plugin.getPluginConfig().getServerId();
        
        return plugin.getDatabaseManager().deleteResidence(residenceName, serverId)
                .thenCompose(success -> {
                    if (success) {
                        player.sendMessage("§a领地 " + residenceName + " 已删除");
                    } else {
                        plugin.getMessageConfig().sendMessage(player, "createFailNoMoney", 
                                Map.of("resName", "删除领地"));
                    }
                    return CompletableFuture.completedFuture(success);
                });
    }
    
    /**
     * 显示帮助信息
     */
    private void showResidenceHelp(Player player) {
        player.sendMessage("§6========== 领地跨服帮助 ==========");
        
        if (player.hasPermission("residence.command.tp")) {
            player.sendMessage("§e/res tp <领地名称> §7- 传送到指定领地（支持跨服）");
        }
        
        if (player.hasPermission("residence.command.list")) {
            player.sendMessage("§e/res list §7- 查看您的所有领地");
        }
        
        if (player.hasPermission("residence.command.rename")) {
            player.sendMessage("§e/res rename <旧名称> <新名称> §7- 重命名领地");
        }
        
        if (player.hasPermission("residence.command.give")) {
            player.sendMessage("§e/res give <领地名称> <玩家> §7- 将领地给予其他玩家");
        }
        
        if (player.hasPermission("residence.command.remove")) {
            player.sendMessage("§e/res remove <领地名称> §7- 删除领地");
        }
        
        player.sendMessage("§7使用 §e/res help §7查看更多帮助");
    }
}