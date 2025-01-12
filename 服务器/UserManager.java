package App;

import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    // 使用 HashMap 来存储在线用户的 PrintWriter（用于文本数据）
    private final Map<String, PrintWriter> onlineUsersText = new HashMap<>();
    
    // 使用 HashMap 来存储在线用户的 DataOutputStream（用于二进制数据，如图片）
    private final Map<String, DataOutputStream> onlineUsersBinary = new HashMap<>();
    
    // 新增 HashMap 来存储每个用户的 ack_number
    private final Map<String, Integer> userAckNumbers = new HashMap<>();
    
    // 新增 HashMap 来存储每个用户的 permit
    private final Map<String, Integer> userPermits = new HashMap<>();

    // 用于同步的方法，确保线程安全
    // 添加用户的文本输出流、二进制输出流
    public synchronized boolean addUser(String username, PrintWriter out, DataOutputStream imageOut) {
        if (onlineUsersText.containsKey(username) || onlineUsersBinary.containsKey(username)) {
            return false; // 用户已在线
        }
        onlineUsersText.put(username, out);
        onlineUsersBinary.put(username, imageOut);
        userAckNumbers.put(username, 0);  // 初始化 ack_number 为 0
        userPermits.put(username, 1); // 初始化 permit 为 0
        return true;
    }

    // 移除用户
    public synchronized boolean removeUser(String username) {
        if (onlineUsersText.containsKey(username)) {
            onlineUsersText.remove(username);
            onlineUsersBinary.remove(username);  // 同时移除对应的 DataOutputStream
            userAckNumbers.remove(username);
            userPermits.remove(username); // 同时移除 permit
            return true;
        }
        return false; // 用户不在线
    }

    // 获取某个用户的文本输出流
    public synchronized PrintWriter getUserTextOutputStream(String username) {
        return onlineUsersText.get(username);
    }

    // 获取某个用户的二进制数据输出流
    public synchronized DataOutputStream getUserBinaryOutputStream(String username) {
        return onlineUsersBinary.get(username);
    }

    // 检查用户是否在线
    public synchronized boolean isUserOnline(String username) {
        return onlineUsersText.containsKey(username) || onlineUsersBinary.containsKey(username);
    }
    
    // 获取所有在线用户（文本流）
    public synchronized Map<String, PrintWriter> getAllOnlineUsersText() {
        return new HashMap<>(onlineUsersText); // 返回副本以避免外部修改
    }

    // 获取所有在线用户（图片输出流）
    public synchronized Map<String, DataOutputStream> getAllOnlineUsersBinary() {
        return new HashMap<>(onlineUsersBinary); // 返回副本以避免外部修改
    }

    public synchronized void incrementAck(String username) {
        int currentAck = userAckNumbers.getOrDefault(username, 0);
        userAckNumbers.put(username, currentAck + 1);
    
        // 唤醒等待该用户 ack 的线程
        notifyAll();  // 通知所有等待的线程
    }

    public synchronized int getAckSize(String username) {
        return userAckNumbers.getOrDefault(username, 0);
    }

    public synchronized void decrementAck(String username) throws InterruptedException {
        // 如果 ack_number <= 0，阻塞直到 ack_number > 0
        while (userAckNumbers.getOrDefault(username, 0) <= 0) {
            wait();  // 当前线程将被阻塞，直到有其它线程调用 notify()
        }

        // 执行扣减操作
        int currentAck = userAckNumbers.getOrDefault(username, 0);
        if (currentAck > 0) {
            userAckNumbers.put(username, currentAck - 1);
        }
    }
    
     // 增加用户的 permit
    public synchronized void incrementPermit(String username) {
        int currentPermit = userPermits.getOrDefault(username, 0);
        userPermits.put(username, currentPermit + 1);
    
        // 唤醒等待该用户 permit 的线程
        notifyAll();  // 通知所有等待的线程
    }

    // 获取某个用户的 permit 数量
    public synchronized int getPermitSize(String username) {
        return userPermits.getOrDefault(username, 0);
    }

    // 减少用户的 permit，并实现等待和唤醒
    public synchronized void decrementPermit(String username) throws InterruptedException {
        // 如果 permit <= 0，阻塞直到 permit > 0
        while (userPermits.getOrDefault(username, 0) <= 0) {
            wait();  // 当前线程将被阻塞，直到有其它线程调用 notify()
        }

        // 执行扣减操作
        int currentPermit = userPermits.getOrDefault(username, 0);
        if (currentPermit > 0) {
            userPermits.put(username, currentPermit - 1);
        }
    }

}
