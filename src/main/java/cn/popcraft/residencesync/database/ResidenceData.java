package cn.popcraft.residencesync.database;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 领地数据类
 * 
 * 表示跨服同步的领地信息
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class ResidenceData {
    
    private final String name;
    private final String ownerUuid;
    private final String serverId;
    private final String world;
    private final double x1, y1, z1;
    private final double x2, y2, z2;
    private final Timestamp creationTime;
    private final Timestamp lastModified;
    
    public ResidenceData(String name, String ownerUuid, String serverId, String world,
                        double x1, double y1, double z1, double x2, double y2, double z2,
                        Timestamp creationTime, Timestamp lastModified) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.serverId = serverId;
        this.world = world;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getOwnerUuid() {
        return ownerUuid;
    }
    
    public UUID getOwnerUuidAsUUID() {
        return UUID.fromString(ownerUuid);
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public String getWorld() {
        return world;
    }
    
    public double getX1() {
        return x1;
    }
    
    public double getY1() {
        return y1;
    }
    
    public double getZ1() {
        return z1;
    }
    
    public double getX2() {
        return x2;
    }
    
    public double getY2() {
        return y2;
    }
    
    public double getZ2() {
        return z2;
    }
    
    public Timestamp getCreationTime() {
        return creationTime;
    }
    
    public Timestamp getLastModified() {
        return lastModified;
    }
    
    /**
     * 获取领地的中心点坐标
     */
    public cn.popcraft.residencesync.util.Vector3D getCenter() {
        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;
        double centerZ = (z1 + z2) / 2.0;
        return new cn.popcraft.residencesync.util.Vector3D(centerX, centerY, centerZ);
    }
    
    /**
     * 检查给定的坐标是否在领地范围内
     */
    public boolean contains(double x, double y, double z) {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);
        
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * 获取领地的体积（立方体）
     */
    public double getVolume() {
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);
        double length = Math.abs(z2 - z1);
        return width * height * length;
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        return String.format("ResidenceData{name='%s', owner='%s', server='%s', world='%s', area=[%s,%s,%s] to [%s,%s,%s]}",
                name, ownerUuid, serverId, world, x1, y1, z1, x2, y2, z2);
    }
    
    /**
     * 检查两个领地数据是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ResidenceData that = (ResidenceData) obj;
        return name.equals(that.name) && serverId.equals(that.serverId);
    }
    
    /**
     * 生成hashCode
     */
    @Override
    public int hashCode() {
        return name.hashCode() * 31 + serverId.hashCode();
    }
}