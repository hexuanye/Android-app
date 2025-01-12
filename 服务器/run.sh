#!/bin/bash

# 定义目录路径
SRC_DIR=/root/server          # 存放源代码的文件夹
LIB_DIR=lib                   # 存放库文件的文件夹
OUTPUT_DIR=/root/server/App   # 编译输出目录
JAR_PATH=$LIB_DIR/mysql-connector-java-8.0.30.jar  # MySQL JDBC 驱动包
PORT=3001                     # 需要清理的端口

# 检查并清理占用端口的进程
echo "Checking for any process occupying port $PORT..."
PID=$(lsof -t -i:$PORT)

if [ -n "$PID" ]; then
    echo "Process occupying port $PORT found with PID: $PID"
    echo "Killing process with PID $PID..."
    kill -9 $PID
    echo "Process killed."
else
    echo "No process found occupying port $PORT."
fi

# 清理输出目录中所有 .class 文件，以确保覆盖
echo "Cleaning up old .class files in $OUTPUT_DIR"
rm -f $OUTPUT_DIR/*.class

# 编译 Java 文件并输出到 /root/server/App 文件夹
echo "Compiling Java files..."
javac -cp ".:$JAR_PATH" -d $OUTPUT_DIR $SRC_DIR/*.java

# 检查编译是否成功
if [ $? -eq 0 ]; then
    # 编译成功后运行服务器
    echo "Compilation successful, starting the server..."
    java -cp ".:$JAR_PATH:$OUTPUT_DIR" App.Server
else
    # 编译失败，输出错误
    echo "Compilation failed, please check your Java code."
fi
