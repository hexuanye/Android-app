package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;

    // 定义 PrintWriter 和 BufferedReader 为 public 变量
    static public PrintWriter out;
    static public BufferedReader in;
    static DataOutputStream imageOut;
    static DataInputStream imageIn;
    static public Socket socket;
    static String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化控件
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // 登录按钮点击事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 调用公共的处理方法
                sendMessageToServer("login", username, password);
            }
        });

        // 注册按钮点击事件
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 调用公共的处理方法
                sendMessageToServer("register", username, password);
            }
        });
    }

    // 公共的发送消息到服务器的方法
    private void sendMessageToServer(String action, String username, String password) {
        // 启动新线程进行 Socket 连接和消息发送
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 连接到服务器（修改为你的服务器 IP 和端口）
                    socket = new Socket("8.134.153.106", 3001);  // 服务器的 IP 和端口
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    imageOut= new DataOutputStream(socket.getOutputStream());
                    imageIn = new DataInputStream(socket.getInputStream());
                    // 根据动作（登录或注册）构造消息
                    String message = "#"+action + "#" + username + "#" + password;
                    out.println(message);  // 发送消息到服务器

                    runOnUiThread(new Runnable() {//避免主线程被阻塞引起的页面冻结
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "消息已发送: " + action, Toast.LENGTH_SHORT).show();
                        }
                    });

                    // 接收服务器的响应
                    String response = in.readLine();  // 读取服务器返回的响应

                    // 处理服务器的响应
                    handleServerResponse(response);
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "连接错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // 处理服务器返回的响应
    private void handleServerResponse(String response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ("Y".equals(response)) {
                    // 如果服务器返回 "Y"，跳转到 ChatActivity
                    Toast.makeText(LoginActivity.this,  response+"收到同意", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this,ChatActivity.class);
                    startActivity(intent);

                    //finish();  // 结束当前的 LoginActivity
                } else if ("N".equals(response)) {
                    // 如果服务器返回 "N"，提示登录或注册失败
                    Toast.makeText(LoginActivity.this, "登录或注册失败，请检查账号或密码", Toast.LENGTH_SHORT).show();
                } else {
                    // 如果返回的不是 Y 或 N，处理其他情况
                    Toast.makeText(LoginActivity.this, "未知的服务器响应: " + response, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在退出时关闭 socket 连接，避免内存泄漏
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
