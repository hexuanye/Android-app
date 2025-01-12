package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
public class ChatActivity extends AppCompatActivity {
    String chat_username="请选择对象";
    private AckManager ackManager;
    private TextView tvChatUsername;  // 显示用户名
    private EditText etMessage;  // 用于输入消息
    private Button btnSend, btnUploadImage, btnSelectUser, btnExit; // 发送按钮和退出按钮
    private LinearLayout chatLayout;  // 用于动态添加消息的 LinearLayout

    // 用来标识接收线程是否应该继续运行
    private boolean isReceivingMessages = true;

    // 存储选择的图片的 URI
    private Uri selectedImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 初始化 AckManager
        ackManager = new AckManager();
        // 启动一个线程持续接收服务器消息
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receiveMessagesFromServer();  // 持续接收消息
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化控件
        tvChatUsername = findViewById(R.id.tvChatUsername);
        tvChatUsername.setText(chat_username);

        etMessage = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setBackgroundColor(Color.parseColor("#09801C"));
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadImage.setBackgroundColor(Color.parseColor("#09801C"));
        btnSelectUser = findViewById(R.id.btnSelect);

        btnExit = findViewById(R.id.btnExit);
        btnExit.setBackgroundColor(Color.parseColor("#FF0000"));
        chatLayout = findViewById(R.id.chatLayout);  // 存放消息的 LinearLayout

        // 发送消息按钮点击事件
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString();
                if (!message.isEmpty()) {
                    sendMessageToServer(message);  // 调用方法发送消息到服务器
                    etMessage.setText("");  // 清空输入框
                } else {
                    Toast.makeText(ChatActivity.this, "消息不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 上传图片按钮点击事件
        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个 Intent，打开图片选择器
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // 启动图片选择器，并处理结果
                pickImageLauncher.launch(intent);
            }
        });
        // 退出按钮点击事件
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止接收线程
                isReceivingMessages = false;
                closeSocketConnection();
                Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();  // 结束当前的 ChatActivity
            }
        });
        // 选择点击事件
        btnSelectUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString();
                if (!message.isEmpty()) {
                    if(message.equals(LoginActivity.username)){
                        Toast.makeText(ChatActivity.this, "对象不能是自己", Toast.LENGTH_SHORT).show();
                        etMessage.setText("");  // 清空输入框
                    }
                    else {
                        chat_username = message;
                        tvChatUsername.setText(chat_username);
                        etMessage.setText("");  // 清空输入框
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "对象不能为空", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    // 发送消息到服务器
    private void sendMessageToServer(String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!ackManager.decrementPermission(10000)){return;};
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 从 LoginActivity 获取静态变量
                PrintWriter out = LoginActivity.out;
                BufferedReader in = LoginActivity.in;

                // 向服务器发送消息
                String header="#"+chat_username+"#"+"text";
                out.println(header);
                out.println(message);
                ackManager.incrementPermission();


                // 显示发送的消息
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String messageToDisplay = "Me: " + message + "\n";

                        // 创建新的 TextView 来显示发送的消息
                        TextView messageTextView = new TextView(ChatActivity.this);
                        messageTextView.setText(messageToDisplay);  // 设置消息文本
                        messageTextView.setGravity(Gravity.END);    // 右对齐
                        messageTextView.setTextColor(Color.BLUE);   // 设置蓝色字体

                        // 获取 LinearLayout 来存放消息
                        chatLayout.addView(messageTextView); // 将消息添加到聊天布局
                    }
                });
            }
        }).start();
    }

    // 持续接收服务器消息的方法
    private void receiveMessagesFromServer() throws IOException, InterruptedException {
        DataInputStream imageIn = LoginActivity.imageIn;
        DataOutputStream imageOut=LoginActivity.imageOut;
        PrintWriter out=LoginActivity.out;
        BufferedReader in = LoginActivity.in;
        while (isReceivingMessages) {
            String response = in.readLine();  // 阻塞，等待接收到消息
            if(response.equals("ACK")) {
                ackManager.incrementAck();continue;
            }
            if (!response.startsWith("#text") && !response.startsWith("#image")) {
                continue;
            }

            String[] response_parts = response.split("#");
            if (response_parts[1].equals("text") || response_parts[1].equals("image")) {
                // 服务器返回的消息
                String res="test";
                if(response_parts[1].equals("text")){ res= "@"+response_parts[2] + ":" + response_parts[3];}
                else{
                    int byteNumber=Integer.parseInt(response_parts[3]);
                    double d_byte=byteNumber/1024.0/1024.0;
                    @SuppressLint("DefaultLocale") String s_byte_Mb = String.format("%.2f", d_byte);  // 保留2位小数
                    res="@"+response_parts[2]+":image("+s_byte_Mb+"MB),接收中...";
                }

                // 显示服务器的回应，假设“Server: response”
                TextView serverTextView = new TextView(ChatActivity.this);
                serverTextView.setText(res + "\n");
                serverTextView.setGravity(Gravity.START); // 左对齐
                serverTextView.setTextColor(Color.BLACK);  // 设置黑色字体

                // 使用 runOnUiThread 更新 UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 获取 LinearLayout 来存放消息
                        chatLayout.addView(serverTextView); // 将消息添加到聊天布局
                    }
                });


            }
            if (response_parts[1].equals("image")) {

                int imageSize = Integer.parseInt(response_parts[3]);

                // 用于保存图片的文件路径
                File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "received_image.jpg");

                FileOutputStream fileOut = null;
                fileOut = new FileOutputStream(imageFile);

                byte[] buffer = new byte[1024];  // 设置缓冲区为1024字节
                int bytesRead;
                int totalBytesReceived = 0;

                if(!ackManager.decrementPermission(10000)){return;};

                // 向服务器发送准备接收图片的消息
                out.println("ACK");
                out.flush();  // 确保确认字节立即发送

                try {
                    while (totalBytesReceived < imageSize && (bytesRead = imageIn.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);  // 写入文件
                        totalBytesReceived += bytesRead;

                        // 每接收到一个块数据后，发送一个确认字节
                        //out.println("ACK");  // 使用 PrintWriter 发送确认消息
                        //out.flush();  // 确保确认字节立即发送
                    }
                } catch (IOException e) {
                    // 这里仅记录错误信息，确保连接不被关闭
                    Log.e("ChatActivity", "图片接收过程中发生错误", e);
                    // 不关闭连接，继续等待下一个操作或通知用户
                } finally {
                    // 在出错时，不关闭文件输出流和连接
                    try {
                        fileOut.flush();  // 确保文件内容完全写入
                    } catch (IOException e) {
                        Log.e("ChatActivity", "文件刷新失败", e);
                    }
                }

               // 确保接收完成后发送结束消息
                out.println("ACK");
                out.flush();  // 确保结束消息发送
                ackManager.incrementPermission();



                // 图片接收完成后，更新 UI 显示接收到的图片
                runOnUiThread(() -> {
                    // 创建一个新的 ImageView 来显示收到的图片
                    ImageView receivedImageView = new ImageView(ChatActivity.this);
                    receivedImageView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    // 创建图片的 URI
                    Uri imageUri = Uri.fromFile(imageFile);  // 使用文件路径创建 URI
                    // 设置图片（这里假设 imageUri 是图片的 URI）
                    receivedImageView.setImageURI(imageUri); // 设置收到的图片 URI

                    // 获取屏幕宽度
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;

                    // 设置图片宽度为屏幕宽度的 60%，保持宽高比
                    int newWidth = (int) (screenWidth * 0.6);  // 宽度占屏幕的 60%
                    int newHeight = (int) (newWidth * 0.6);    // 高度按 60% 宽度比例设置

                    // 设置图片的宽高
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(newWidth, newHeight);

                    // 设置图片左对齐
                    params.gravity = Gravity.START; // 左对齐
                    receivedImageView.setLayoutParams(params);

                    // 将图片添加到聊天布局
                    LinearLayout chatLayout = findViewById(R.id.chatLayout);
                    chatLayout.addView(receivedImageView);

                    Toast.makeText(ChatActivity.this, "图片接收成功", Toast.LENGTH_SHORT).show();
                });


            }//end图片
        }//end循环
    }


    private ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(androidx.activity.result.ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 获取用户选择的图片 URI
                        selectedImageUri = result.getData().getData();

                        // 显示图片选择结果
                        Toast.makeText(ChatActivity.this, "图片选择成功: " + selectedImageUri.toString(), Toast.LENGTH_SHORT).show();
                        // 调用方法发送图片
                        sendImageToServer(selectedImageUri);
                    }
                }
            });
    // 发送图片到服务器
    private void sendImageToServer(Uri imageUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PrintWriter out = LoginActivity.out;
                    DataOutputStream imageOut = LoginActivity.imageOut;
                    InputStream imageInputStream = getContentResolver().openInputStream(imageUri);

                    if (imageInputStream == null) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "无法读取图片", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // 获取文件路径和文件大小
                    String filePath = getRealPathFromURI(imageUri);
                    File imageFile = new File(filePath);
                    long fileSize = imageFile.length();
                    String message = "#" + chat_username + "#" + "image" + "#" + String.valueOf(fileSize);

                    if(!ackManager.decrementPermission(10000)){return;};
                    out.println(message);  // 发送文件大小和类型信息到服务器
                    out.flush();

                    // 逐块读取和发送图片数据
                    byte[] buffer = new byte[1024];  // 设置缓冲区为1024字节
                    int bytesRead;
                    long totalBytesSent = 0;



                    if(!ackManager.decrementAck(10000)){
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "请求超时，自动放弃发送图片，请稍后重试", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "图片开始发送", Toast.LENGTH_SHORT).show());
                    while ((bytesRead = imageInputStream.read(buffer)) != -1) {
                        imageOut.write(buffer, 0, bytesRead);  // 发送数据块
                        //ack_message=LoginActivity.in.readLine();
                        totalBytesSent += bytesRead;
                        // 如果传输完成，结束
                        if (totalBytesSent >= fileSize) {
                            break;
                        }

                    }
                    ackManager.incrementPermission();


                    // 图片上传完成后，更新 UI 显示上传的图片
                    runOnUiThread(() -> {
                        // 显示图片
                        ImageView uploadedImageView = new ImageView(ChatActivity.this);
                        uploadedImageView.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        uploadedImageView.setImageURI(imageUri); // 设置上传的图片

                        // 获取屏幕宽度和高度
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;

                        // 设置图片宽度为屏幕宽度的 75%，同时保持宽高比例
                        int newWidth = (int) (screenWidth * 0.6);  // 宽度占屏幕的 75%
                        int newHeight = (int) (newWidth * 0.6); // 高度按 75% 宽度比例设置

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(newWidth, newHeight);


                        params.gravity = Gravity.END; // 右对齐
                        uploadedImageView.setLayoutParams(params);

                        // 将图片添加到聊天布局
                        LinearLayout chatLayout = findViewById(R.id.chatLayout);
                        chatLayout.addView(uploadedImageView);

                        Toast.makeText(ChatActivity.this, "图片发送成功", Toast.LENGTH_SHORT).show();
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "图片发送失败", Toast.LENGTH_SHORT).show());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    // 根据 URI 获取实际文件路径
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        return null;
    }

    //断开连接
    private void closeSocketConnection() {
        try {
            if (LoginActivity.socket != null && !LoginActivity.socket.isClosed()) {
                LoginActivity.socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
