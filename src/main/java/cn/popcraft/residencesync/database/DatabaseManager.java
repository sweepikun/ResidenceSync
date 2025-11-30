package cn.popcraft.residencesync.database;

import cn.popcraft.residencesync.util.LoggerUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库管理器
 * 
 * 负责管理MySQL数据库连接、初始化表结构和数据库操作
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class DatabaseManager {
    
    private final cn.popcraft.residencesync.ResidenceSyncPlugin plugin;
    private HikariDataSource dataSource;
    
    // 表名常量
    private static final String RESIDENCES_TABLE = "residencesync_residences";
    private static final String PLAYERS_TABLE = "residencesync_players";
    
    public DatabaseManager(cn.popcraft.residencesync.ResidenceSyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化数据库连接池
     */
    public boolean initialize() {
        try {
            HikariConfig config = new HikariConfig();
            
            // 数据库连接配置
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8",
                    plugin.getPluginConfig().getDatabaseHost(),
                    plugin.getPluginConfig().getDatabasePort(),
                    plugin.getPluginConfig().getDatabaseName()));
            
            config.setUsername(plugin.getPluginConfig().getDatabaseUsername());
            config.setPassword(plugin.getPluginConfig().getDatabasePassword());
            
            // 连接池配置
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // 数据库驱动
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            dataSource = new HikariDataSource(config);
            
            // 初始化表结构
            if (!initializeTables()) {
                return false;
            }
            
            LoggerUtil.info("数据库连接池初始化完成");
            return true;
            
        } catch (Exception e) {
            LoggerUtil.severe("数据库初始化失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 初始化数据库表
     */
    private boolean initializeTables() {
        try (Connection conn = getConnection()) {
            // 创建领地表
            String createResidencesTable = """
                CREATE TABLE IF NOT EXISTS `""" + RESIDENCES_TABLE + """` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `residence_name` varchar(255) NOT NULL,
                  `owner_uuid` varchar(36) NOT NULL,
                  `server_id` varchar(100) NOT NULL,
                  `world` varchar(255) NOT NULL,
                  `x1` double NOT NULL,
                  `y1` double NOT NULL,
                  `z1` double NOT NULL,
                  `x2` double NOT NULL,
                  `y2` double NOT NULL,
                  `z2` double NOT NULL,
                  `creation_time` timestamp DEFAULT CURRENT_TIMESTAMP,
                  `last_modified` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `residence_server_unique` (`residence_name`, `server_id`),
                  KEY `owner_uuid_idx` (`owner_uuid`),
                  KEY `server_id_idx` (`server_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            // 创建玩家表
            String createPlayersTable = """
                CREATE TABLE IF NOT EXISTS `""" + PLAYERS_TABLE + """` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `uuid` varchar(36) NOT NULL,
                  `player_name` varchar(16) NOT NULL,
                  `last_seen` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uuid_unique` (`uuid`),
                  KEY `player_name_idx` (`player_name`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            Statement stmt = conn.createStatement();
            stmt.execute(createResidencesTable);
            stmt.execute(createPlayersTable);
            
            LoggerUtil.info("数据库表初始化完成");
            return true;
            
        } catch (SQLException e) {
            LoggerUtil.severe("创建数据库表失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * 关闭数据库连接池
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LoggerUtil.info("数据库连接池已关闭");
        }
    }
    
    /**
     * 添加或更新领地信息
     */
    public CompletableFuture<Boolean> addOrUpdateResidence(ResidenceData residence) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = """
                    INSERT INTO `""" + RESIDENCES_TABLE + """` 
                    (`residence_name`, `owner_uuid`, `server_id`, `world`, `x1`, `y1`, `z1`, `x2`, `y2`, `z2`)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    `owner_uuid` = VALUES(`owner_uuid`),
                    `world` = VALUES(`world`),
                    `x1` = VALUES(`x1`),
                    `y1` = VALUES(`y1`),
                    `z1` = VALUES(`z1`),
                    `x2` = VALUES(`x2`),
                    `y2` = VALUES(`y2`),
                    `z2` = VALUES(`z2`),
                    `last_modified` = CURRENT_TIMESTAMP
                    """;
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, residence.getName());
                stmt.setString(2, residence.getOwnerUuid());
                stmt.setString(3, residence.getServerId());
                stmt.setString(4, residence.getWorld());
                stmt.setDouble(5, residence.getX1());
                stmt.setDouble(6, residence.getY1());
                stmt.setDouble(7, residence.getZ1());
                stmt.setDouble(8, residence.getX2());
                stmt.setDouble(9, residence.getY2());
                stmt.setDouble(10, residence.getZ2());
                
                int result = stmt.executeUpdate();
                stmt.close();
                
                return result > 0;
                
            } catch (SQLException e) {
                LoggerUtil.severe("添加或更新领地信息失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 删除领地信息
     */
    public CompletableFuture<Boolean> deleteResidence(String residenceName, String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM `" + RESIDENCES_TABLE + "` WHERE `residence_name` = ? AND `server_id` = ?";
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, residenceName);
                stmt.setString(2, serverId);
                
                int result = stmt.executeUpdate();
                stmt.close();
                
                return result > 0;
                
            } catch (SQLException e) {
                LoggerUtil.severe("删除领地信息失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 获取玩家的所有领地
     */
    public CompletableFuture<List<ResidenceData>> getPlayerResidences(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ResidenceData> residences = new ArrayList<>();
            
            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM `" + RESIDENCES_TABLE + "` WHERE `owner_uuid` = ? ORDER BY `creation_time` DESC";
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, playerUuid.toString());
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    ResidenceData residence = new ResidenceData(
                        rs.getString("residence_name"),
                        rs.getString("owner_uuid"),
                        rs.getString("server_id"),
                        rs.getString("world"),
                        rs.getDouble("x1"),
                        rs.getDouble("y1"),
                        rs.getDouble("z1"),
                        rs.getDouble("x2"),
                        rs.getDouble("y2"),
                        rs.getDouble("z2"),
                        rs.getTimestamp("creation_time"),
                        rs.getTimestamp("last_modified")
                    );
                    residences.add(residence);
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                LoggerUtil.severe("获取玩家领地信息失败: " + e.getMessage(), e);
            }
            
            return residences;
        });
    }
    
    /**
     * 根据名称和服务器ID获取领地
     */
    public CompletableFuture<ResidenceData> getResidence(String residenceName, String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM `" + RESIDENCES_TABLE + "` WHERE `residence_name` = ? AND `server_id` = ?";
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, residenceName);
                stmt.setString(2, serverId);
                
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    ResidenceData residence = new ResidenceData(
                        rs.getString("residence_name"),
                        rs.getString("owner_uuid"),
                        rs.getString("server_id"),
                        rs.getString("world"),
                        rs.getDouble("x1"),
                        rs.getDouble("y1"),
                        rs.getDouble("z1"),
                        rs.getDouble("x2"),
                        rs.getDouble("y2"),
                        rs.getDouble("z2"),
                        rs.getTimestamp("creation_time"),
                        rs.getTimestamp("last_modified")
                    );
                    
                    rs.close();
                    stmt.close();
                    return residence;
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                LoggerUtil.severe("获取领地信息失败: " + e.getMessage(), e);
            }
            
            return null;
        });
    }
    
    /**
     * 更新玩家信息
     */
    public CompletableFuture<Boolean> updatePlayer(UUID uuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = """
                    INSERT INTO `""" + PLAYERS_TABLE + """` (`uuid`, `player_name`)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE 
                    `player_name` = VALUES(`player_name`),
                    `last_seen` = CURRENT_TIMESTAMP
                    """;
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                
                int result = stmt.executeUpdate();
                stmt.close();
                
                return result > 0;
                
            } catch (SQLException e) {
                LoggerUtil.severe("更新玩家信息失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 获取玩家姓名
     */
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "SELECT `player_name` FROM `" + PLAYERS_TABLE + "` WHERE `uuid` = ?";
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, uuid.toString());
                
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String playerName = rs.getString("player_name");
                    rs.close();
                    stmt.close();
                    return playerName;
                }
                
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                LoggerUtil.severe("获取玩家姓名失败: " + e.getMessage(), e);
            }
            
            return null;
        });
    }
    
    /**
     * 同步本地Residence插件的领地数据
     */
    public CompletableFuture<Boolean> syncResidenceData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 这里需要集成Residence插件的API来获取本地领地数据
                // 暂时返回true，实际实现时需要调用Residence插件的方法
                LoggerUtil.info("开始同步Residence插件数据...");
                return true;
                
            } catch (Exception e) {
                LoggerUtil.severe("同步Residence数据失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
}