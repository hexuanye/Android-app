package App;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import App.ChatMessage;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;


public class DatabaseHandler {

    // 数据库连接信息
    private static final String URL = "jdbc:mysql://localhost:3306/record";  // 请根据实际情况修改
    private static final String USER = "record";  // 数据库用户名
    private static final String PASSWORD = "123456";  // 数据库密码

    // 获取数据库连接
    private static Connection getConnection() throws SQLException {
        try {
            // 加载 MySQL 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new SQLException("MySQL Driver not found", e);
        }
    }

    public static List<ChatMessage> getMessagesByReceiver(String receiver) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT sender, receiver, type, text, image, time FROM Chat WHERE receiver = ? ORDER BY time ASC";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, receiver);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sender = rs.getString("sender");
                    String receiverResult = rs.getString("receiver");
                    boolean type = rs.getBoolean("type");
                    String text = type ? rs.getString("text") : null;  // 如果是文本消息，获取text，否则为null
                    byte[] image = type ? null : rs.getBytes("image"); // 如果是图片消息，获取image，否则为null
                    Timestamp time = rs.getTimestamp("time");
                    
                    // 创建相应的消息对象
                    ChatMessage message;
                    if (type) {
                        message = new ChatMessage(sender, receiverResult, text, time);  // 创建文本消息
                    } else {
                        message = new ChatMessage(sender, receiverResult, image, time);  // 创建图片消息
                    }
                    
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public static boolean insertTextMessage(String sender, String receiver, String text) {
        String sql = "INSERT INTO Chat (sender, receiver, type, text, time) VALUES (?, ?, true, ?, NOW())";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, text);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean insertImageMessage(String sender, String receiver, byte[] imageData) {
        String sql = "INSERT INTO Chat (sender, receiver, type, image, time) VALUES (?, ?, false, ?, NOW())";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setBytes(3, imageData);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static List<String> getChatMessages(String sender, String receiver) {
        List<String> chatMessages = new ArrayList<>();
        String sql = "SELECT time, sender, receiver, type, text, image FROM Chat WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY time";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, receiver);
            stmt.setString(4, sender);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String time = rs.getString("time");
                String senderName = rs.getString("sender");
                String receiverName = rs.getString("receiver");
                boolean type = rs.getBoolean("type");
                
                if (type) {  // 文本消息
                    String text = rs.getString("text");
                    chatMessages.add("[" + time + "] " + senderName + " to " + receiverName + ": " + text);
                } else {  // 图片消息
                    byte[] image = rs.getBytes("image");
                    chatMessages.add("[" + time + "] " + senderName + " sent an image (size: " + image.length + " bytes)");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return chatMessages;
    }
    
    public static boolean deleteChatMessage(int chatId) {
        String sql = "DELETE FROM Chat WHERE id = ?";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, chatId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }




    // 获取用户信息（示例）
    public static boolean checkUserExists(String username) {
        String sql = "SELECT * FROM User WHERE username = ?";
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            return rs.next();  // 如果存在该用户，返回 true
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    // 登录功能，检查用户名和密码是否匹配，并更新时间
    public static boolean login(String username, String password) {
        String checkSql = "SELECT * FROM User WHERE username = ?";
        String updateSql = "UPDATE User SET time = NOW() WHERE username = ? AND password = ?";
    
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
    
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
    
            if (!rs.next()) {
                // 用户名不存在
                System.out.println("Error: Username not found.");
                return false;
            } else {
                // 用户名存在，检查密码
                String storedPassword = rs.getString("password");
                if (!storedPassword.equals(password)) {
                    // 密码不匹配
                    System.out.println("Error: Incorrect password.");
                    return false;
                }
    
                // 密码匹配，更新用户的时间
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, username);
                    updateStmt.setString(2, password);
                    int rowsAffected = updateStmt.executeUpdate();
                    System.out.println("User time updated: " + rowsAffected + " row(s).");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 注册新用户功能，插入用户记录
    public static boolean register(String username, String password) {
        String checkSql = "SELECT * FROM User WHERE username = ?";
        String insertSql = "INSERT INTO User (username, password, time) VALUES (?, ?, NOW())";
    
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
    
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
    
            if (rs.next()) {
                // 用户名已存在
                System.out.println("Error: Username already exists.");
                return false;
            }
    
            // 用户名不存在，插入新用户
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                int rowsAffected = insertStmt.executeUpdate();
                System.out.println("New user inserted: " + rowsAffected + " row(s).");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteMessagesByReceiver(String receiver) {
        String sql = "DELETE FROM Chat WHERE receiver = ?";
        
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置删除条件：接收者为指定的 receiver
            stmt.setString(1, receiver);
            
            // 执行删除操作
            int rowsAffected = stmt.executeUpdate();
            
            // 如果删除了至少一行数据，返回 true
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;  // 如果出现异常，返回 false
    }

    
    // 关闭连接（如果需要）
    public static void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
