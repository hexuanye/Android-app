package App;
import App.DatabaseHandler;
import App.UserManager;
import App.ChatMessage;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.ArrayList;


public class Server {
    public static final int PORT = 3001;  // 服务器监听的端口
    private static final UserManager userManager = new UserManager();  // 管理在线用户//在线用户记录
    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            // 创建 ServerSocket 并绑定到指定端口
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started, waiting for connection...");

            while (true) {
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());

                // 每次有新的客户端连接时，创建一个新的线程来处理该客户端
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();  // 启动线程处理客户端请求
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 处理客户端连接的任务
    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // 使用 BufferedReader 和 PrintWriter 进行字符流处理
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  // 自动flush
                DataInputStream imageIn = new DataInputStream(socket.getInputStream());
                DataOutputStream imageOut= new DataOutputStream(socket.getOutputStream());
                String username=" ";
                
                String message = in.readLine(); 
                if (message.startsWith("#login") || message.startsWith("#register")) {
                        String[] parts = message.split("#");
                        // 提取操作、用户名和密码
                        username = parts[2];
                        String password = parts[3];
                        if(message.startsWith("#login")){
                            if(DatabaseHandler.login(username,password)){
                                if(userManager.addUser(username,out,imageOut)){System.out.println("首次上线");}//标记用户在线
                                out.println("Y");
                                System.out.println("用户 @"+username+" 上线");
                                // 获取用户 "user123" 的所有消息记录
                                List<ChatMessage> chat_records = DatabaseHandler.getMessagesByReceiver(username);
                                
                                // 输出所有消息
                                try {
                                    userManager.decrementPermit(username);//占用用户的输出流
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt(); // 重新中断当前线程
                                    System.out.println("Thread was interrupted while waiting.");
                                    // 可在这里做进一步的处理，例如返回，或者日志记录
                                }
                                for (ChatMessage record : chat_records) {
                                    if (record.isText()) {
                                        System.out.println("-->文本消息: " + record.getText());
                                        String sentence="#text#"+record.getSender()+"#"+record.getText();
                                        out.println(sentence);
                                    } else {
                                        System.out.println("-->图片消息: 图片数据大小 = " + record.getImage().length + " bytes");
                                        int imageSize=record.getImage().length;
                                        byte[] imageData=record.getImage();
                                        String sentence="#image#"+record.getSender()+"#"+String.valueOf(imageSize);
                                        
                                        out.println(sentence);
                                        System.out.println("发送了信息："+sentence);
                                        System.out.println("接收开始信号...");
                                        String ack=in.readLine();
                                        System.out.println("收到"+username+"准备信号，发送中...");
                                        // 通过二进制输出流发送图片数据
                                       // 逐块读取并发送图片数据，类似客户端的方式
                                        byte[] buffer = new byte[1024];  // 设置缓冲区为1024字节
                                        int bytesSent = 0;
                                        long totalBytesSent = 0;
                                
                                        try {
                                            while (totalBytesSent < imageSize) {
                                                // 每次最多发送1024字节数据
                                                int chunkSize = (int) Math.min(1024, imageSize - totalBytesSent);
                                                System.arraycopy(imageData, bytesSent, buffer, 0, chunkSize);  // 从图片数据中复制出一个块
                                
                                                // 发送数据块
                                                imageOut.write(buffer, 0, chunkSize);
                                                //可以添加ACK
                                                bytesSent += chunkSize;
                                                totalBytesSent += chunkSize;
                                                //System.out.println("totalBytesSent:"+totalBytesSent);
                                                // 如果传输完成，结束
                                                if (totalBytesSent >= imageSize) {
                                                    break;
                                                }
                                            }
                                            ack=in.readLine();
                                            
                                            System.out.println("图片已成功转发给 " + username);
                                            try {
                                                // 让当前线程休眠 100 毫秒
                                                Thread.sleep(500);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } catch (IOException e) {
                                            System.out.println("图片转发失败给 " + username);
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                if(DatabaseHandler.deleteMessagesByReceiver(username)){System.out.println("已经更新数据库");}
                                userManager.incrementPermit(username);//释放用户的输出流

                            }
                            else{out.println("N");}
                        }
                        else if(message.startsWith("#register")){
                            DatabaseHandler.register(username,password);
                            if(userManager.addUser(username,out,imageOut)){System.out.println("首次上线");}//标记用户在线
                            out.println("Y");
                            System.out.println("新用户 @"+username+" 完成注册上线");
                        }
                }else{
                    System.out.println("用户身份识别错误强制断开连接");
                    socket.close();
                    System.out.println("Connection closed with " + socket.getInetAddress());
                }

                // 在每次循环中，持续读取客户端消息
                while (true) {
                    message = in.readLine();  // 使用 readLine() 读取客户端发送的消息
                    if (message == null || message.equalsIgnoreCase("exit")) {
                        // 如果客户端发送了 "exit" 或者接收到的消息为空，关闭连接
                        out.println("Connection closed by client.");
                        break;  // 退出循环，准备关闭连接
                    }
                    if(message.equals("ACK")){
                        userManager.incrementAck(username);
                    }
                    if (!message.startsWith("#")){continue;}
                    System.out.println("==========开始当前信息处理==========");
                    System.out.println("服务器对象"+username+"Received message from " + socket.getInetAddress() + ": " + message);
                    // 如果消息包含 "#login" 或 "#register"，直接返回 "Y"
                    
                    // 如果是其他请求，查询用户名是否存在
                    String[] message_parts = message.split("#");
                    String chat_username = message_parts[1];
                    String type=message_parts[2];
                    //在此处增加判断用户是否在线
                    System.out.println("用户"+username+"请求向"+chat_username+"发送"+type);
                    boolean userExists = DatabaseHandler.checkUserExists(chat_username);
                    if (!userExists){System.out.println("用户"+chat_username+"不存在，无法完成指令");continue;}
                    if(type.equals("text")){
                        message=in.readLine();
                        if (userManager.isUserOnline(chat_username)){
                            System.out.println(chat_username+" 用户在线");
                            String sentence="#text#"+username+"#"+message;
                            
                            try {
                                userManager.decrementPermit(chat_username);//占用用户的输出流
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 重新中断当前线程
                                System.out.println("Thread was interrupted while waiting.");
                                // 可在这里做进一步的处理，例如返回，或者日志记录
                            }
                            
                            userManager.getUserTextOutputStream(chat_username).println(sentence);//获取目标用户的输入流对准直接输出
                            
                            userManager.incrementPermit(chat_username);//释放用户的输出流
                            
                            System.out.println("发送了信息:"+sentence);
                        }
                        else{
                            System.out.println(chat_username+" 用户不在线");
                            DatabaseHandler.insertTextMessage(username,chat_username,message);
                            System.out.println("文本内容暂存数据库");
                        }
                    }
                    else if(type.equals("image")){
                        long imageSize = Long.parseLong(message_parts[3]);
                        System.out.println("准备接收图片文件，大小为" + imageSize);
                        byte[] imageData = new byte[(int) imageSize];
                        int bytesRead = 0;
                        int totalBytesRead = 0;
                        try {
                                userManager.decrementPermit(username);//占用用户的输出流
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 重新中断当前线程
                                System.out.println("Thread was interrupted while waiting.");
                                // 可在这里做进一步的处理，例如返回，或者日志记录
                            }
                        out.println("ACK");
                        userManager.incrementPermit(username);//释放用户的输出流
                        System.out.println("接收中...");
                        while (totalBytesRead < imageSize) {
                            bytesRead = imageIn.read(imageData, totalBytesRead, (int) Math.min(1024, imageSize - totalBytesRead));  // 每次最多读取1024字节
                            if (bytesRead == -1) {
                                System.out.println("Error: End of stream reached before all data was read.");
                                break;
                            }
                            totalBytesRead += bytesRead;
                            //System.out.println(String.valueOf(bytesRead)+" "+String.valueOf(totalBytesRead));
                        }
                        System.out.println("接收完成"+String.valueOf(totalBytesRead));
                        if (userManager.isUserOnline(chat_username)){
                            System.out.println(chat_username+" 用户在线");
                            String sentence="#image#"+username+"#"+String.valueOf(imageSize);
                            
                             try {
                                userManager.decrementPermit(chat_username);//占用用户的输出流
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 重新中断当前线程
                                System.out.println("Thread was interrupted while waiting.");
                                // 可在这里做进一步的处理，例如返回，或者日志记录
                            }
                            
                            userManager.getUserTextOutputStream(chat_username).println(sentence);
                            System.out.println("发送了信息："+sentence);
                            DataOutputStream chat_image_out=userManager.getUserBinaryOutputStream(chat_username);
                            System.out.println("接收开始信号...");
    
                            try {
                                userManager.decrementAck(chat_username);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 重新中断当前线程
                                System.out.println("Thread was interrupted while waiting.");
                                // 可在这里做进一步的处理，例如返回，或者日志记录
                            }
                            System.out.println("收到"+chat_username+"准备信号，发送中...");
                            // 通过二进制输出流发送图片数据
                           // 逐块读取并发送图片数据，类似客户端的方式
                            byte[] buffer = new byte[1024];  // 设置缓冲区为1024字节
                            int bytesSent = 0;
                            long totalBytesSent = 0;
                    
                            try {
                                while (totalBytesSent < imageSize) {
                                    // 每次最多发送1024字节数据
                                    int chunkSize = (int) Math.min(1024, imageSize - totalBytesSent);
                                    System.arraycopy(imageData, bytesSent, buffer, 0, chunkSize);  // 从图片数据中复制出一个块
                    
                                    // 发送数据块
                                    chat_image_out.write(buffer, 0, chunkSize);
                                    //可以添加ACK
                                    bytesSent += chunkSize;
                                    totalBytesSent += chunkSize;
                                    //System.out.println("totalBytesSent:"+totalBytesSent);
                                    // 如果传输完成，结束
                                    if (totalBytesSent >= imageSize) {
                                        break;
                                    }
                                }
                                userManager.incrementPermit(chat_username);//释放用户的输出流
                                
                                System.out.println("图片已成功转发给 " + chat_username);
                            } catch (IOException e) {
                                System.out.println("图片转发失败给 " + chat_username);
                                e.printStackTrace();
                            }
                            try {
                                userManager.decrementAck(chat_username);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // 重新中断当前线程
                                System.out.println("Thread was interrupted while waiting.");
                                // 可在这里做进一步的处理，例如返回，或者日志记录
                            }
                            int number=userManager.getAckSize(chat_username);
                            System.out.println("ACK("+chat_username+")"+String.valueOf(number));
                        }
                        else{
                            System.out.println(chat_username+"用户不在线");
                            DatabaseHandler.insertImageMessage(username,chat_username,imageData);
                        }
                    }
                        
                    
                    System.out.println("==========完成当前信息处理==========");
                }

                // 关闭连接
                if(userManager.removeUser(username)){System.out.println("用户 @"+username+" 离线");}//从在线名单移除
                socket.close();
                System.out.println("Connection closed with " + socket.getInetAddress());
                

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
