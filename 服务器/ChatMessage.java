package App;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;


public class ChatMessage {
    private String sender;
    private String receiver;
    private boolean type;  // true: 文本消息, false: 图片消息
    private String text;  // 只有当type为true时，text才有值
    private byte[] image; // 只有当type为false时，image才有值
    private Timestamp time;

    // 构造函数，用于创建文本消息
    public ChatMessage(String sender, String receiver, String text, Timestamp time) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = true;  // 文本消息
        this.text = text;
        this.image = null; // 图片数据为空
        this.time = time;
    }

    // 构造函数，用于创建图片消息
    public ChatMessage(String sender, String receiver, byte[] image, Timestamp time) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = false;  // 图片消息
        this.text = null;  // 文本为空
        this.image = image;
        this.time = time;
    }

    // Getter 和 Setter 方法
    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public boolean isText() {
        return type;
    }

    public String getText() {
        return text;
    }

    public byte[] getImage() {
        return image;
    }

    public Timestamp getTime() {
        return time;
    }
}
