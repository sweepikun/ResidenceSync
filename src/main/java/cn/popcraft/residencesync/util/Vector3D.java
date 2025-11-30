package cn.popcraft.residencesync.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * 三维向量工具类
 * 
 * 提供三维空间坐标的处理功能
 * 
 * @author MiniMax Agent
 * @version 1.0.0
 */
public class Vector3D {
    
    private final double x;
    private final double y;
    private final double z;
    
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 从Bukkit Location创建Vector3D
     */
    public static Vector3D fromLocation(Location location) {
        return new Vector3D(location.getX(), location.getY(), location.getZ());
    }
    
    /**
     * 从Bukkit Vector创建Vector3D
     */
    public static Vector3D fromVector(Vector vector) {
        return new Vector3D(vector.getX(), vector.getY(), vector.getZ());
    }
    
    /**
     * 转换为Bukkit Location
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }
    
    /**
     * 转换为Bukkit Vector
     */
    public Vector toVector() {
        return new Vector(x, y, z);
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    /**
     * 计算与另一个向量的距离
     */
    public double distance(Vector3D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * 计算距离的平方（避免开方运算）
     */
    public double distanceSquared(Vector3D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * 计算与另一个向量的欧几里得距离
     */
    public double euclideanDistance(Vector3D other) {
        return distance(other);
    }
    
    /**
     * 计算曼哈顿距离
     */
    public double manhattanDistance(Vector3D other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }
    
    /**
     * 计算切比雪夫距离
     */
    public double chebyshevDistance(Vector3D other) {
        return Math.max(Math.abs(x - other.x), 
               Math.max(Math.abs(y - other.y), Math.abs(z - other.z)));
    }
    
    /**
     * 标准化向量
     */
    public Vector3D normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) {
            return new Vector3D(0, 0, 0);
        }
        return new Vector3D(x / length, y / length, z / length);
    }
    
    /**
     * 向量加法
     */
    public Vector3D add(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }
    
    /**
     * 向量减法
     */
    public Vector3D subtract(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }
    
    /**
     * 向量乘法（标量乘法）
     */
    public Vector3D multiply(double scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }
    
    /**
     * 向量点积
     */
    public double dot(Vector3D other) {
        return x * other.x + y * other.y + z * other.z;
    }
    
    /**
     * 向量叉积
     */
    public Vector3D cross(Vector3D other) {
        return new Vector3D(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }
    
    /**
     * 计算向量长度
     */
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    /**
     * 计算向量长度的平方
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }
    
    /**
     * 检查向量是否为零向量
     */
    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }
    
    /**
     * 创建随机向量
     */
    public static Vector3D random() {
        return new Vector3D(
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
                Math.random() * 2 - 1
        );
    }
    
    /**
     * 创建单位向量
     */
    public static Vector3D up() {
        return new Vector3D(0, 1, 0);
    }
    
    public static Vector3D down() {
        return new Vector3D(0, -1, 0);
    }
    
    public static Vector3D north() {
        return new Vector3D(0, 0, -1);
    }
    
    public static Vector3D south() {
        return new Vector3D(0, 0, 1);
    }
    
    public static Vector3D east() {
        return new Vector3D(1, 0, 0);
    }
    
    public static Vector3D west() {
        return new Vector3D(-1, 0, 0);
    }
    
    /**
     * 线性插值
     */
    public Vector3D lerp(Vector3D other, double t) {
        t = Math.max(0, Math.min(1, t)); // 限制在 [0, 1] 范围内
        return new Vector3D(
                x + (other.x - x) * t,
                y + (other.y - y) * t,
                z + (other.z - z) * t
        );
    }
    
    /**
     * 转换为字符串
     */
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
    
    /**
     * 检查两个向量是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Vector3D vector3D = (Vector3D) obj;
        return Double.compare(vector3D.x, x) == 0 &&
               Double.compare(vector3D.y, y) == 0 &&
               Double.compare(vector3D.z, z) == 0;
    }
    
    /**
     * 生成hashCode
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }
}