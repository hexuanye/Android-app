package com.example.myapplication;

import java.security.Permission;

public class AckManager {
    private int ackCount;
    private int permission;
    public AckManager() {

        this.ackCount = 0; // 初始化 ack 数量为 0
        this.permission=1;//初始化为1
    }

    // 增加 ack 数量，并在增加后唤醒所有等待线程
    public synchronized void incrementAck() {
        ackCount++;
        notifyAll();  // 在增加 ack 后唤醒所有等待的线程
    }
    public synchronized  void incrementPermission(){
        permission++;
        notifyAll();//在增加permission后唤醒所有等待的线程
    }

    // 减少 ack 数量，如果 ackCount <= 0，则阻塞
    // 修改 AckManager 类的 decrementAck 方法，加入超时机制
    public synchronized boolean decrementAck(long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();  // 获取当前时间
        while (ackCount <= 0) {
            long elapsedTime = System.currentTimeMillis() - startTime;  // 计算已等待时间
            long remainingTime = timeout - elapsedTime;  // 计算剩余等待时间
            if (remainingTime <= 0) {
                return false;  // 超过超时时间，直接返回失败
            }
            wait(remainingTime);  // 等待剩余时间
        }
        ackCount--;  // 执行扣减操作
        return true;  // 成功获取到 ACK
    }

    public synchronized boolean decrementPermission(long timeoutMillis) throws InterruptedException {
        // 如果 permission > 0，直接执行扣减
        if (permission > 0) {
            permission--;
            return true;
        }

        // 计算超时时间
        long startTime = System.currentTimeMillis();
        long remainingTime = timeoutMillis;

        // 等待直到 permission > 0 或者超时
        while (permission <= 0 && remainingTime > 0) {
            wait(remainingTime);  // 设置等待超时
            remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime);
        }

        // 如果 permission > 0，执行扣减操作并返回成功
        if (permission > 0) {
            permission--;
            return true;
        }

        // 如果超时仍未成功获取 permission，返回 false
        return false;
    }



    // 获取当前 ack 数量
    public synchronized int getAckCount() {
        return ackCount;
    }
}
