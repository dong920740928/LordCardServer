package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by dong on 10/07/2017.
 */
public class Player {
    private String name;
    private String uuid;

    public BufferedReader input;
    public PrintStream output;

    private LinkedBlockingQueue<String> recvQueue;
    private LinkedBlockingQueue<String> sendQueue;

    public enum State {
        ONLINE, //在线,但不参与匹配
        WAITING, //正在队列中等待匹配
        PLAYING, //正在游戏
        DISCONNECTED,
    }

    public Socket socket;

    public State state;
    public int noResponse;

    public boolean dead;

    public Player(String name, String uuid, Socket socket) throws IOException {
        this.name = name;
        this.uuid = uuid;
        this.socket = socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintStream(socket.getOutputStream());
        this.noResponse = 0;
        this.state = State.ONLINE;
        this.recvQueue = new LinkedBlockingQueue<String>();
        this.sendQueue = new LinkedBlockingQueue<String>();
        this.dead = false;
    }

    synchronized public boolean changeState(State src, State dst) {
        if (state == src) {
            state = dst;
            return true;
        }

        return false;
    }

    public String getUuid() {
        return this.uuid;
    }

    //发送字符串
    public boolean send(String msg) {
        output.println(msg);

        return true;
    }

    //接收字符串
    public String recv() throws IOException {
        return input.readLine();
    }

    //断线
    public boolean release() {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println("player " + getUuid() + "release error: " + e.getMessage());
            return false;
        }

        return true;
    }

    //清空接收消息队列
    public void clearRecvBuf() {
        recvQueue.clear();
    }

    //清空发送消息队列
    public void clearSendBuf() {
        sendQueue.clear();
    }

    //发送隐式心跳包
    public boolean isAlive() {
        try {
            socket.sendUrgentData(0xff);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    //从发送队列获取推送消息
    public String pushMsg() {
        return sendQueue.poll();
    }

    //把指令放到发送队列
    public void sendMsg(String msg) {
        sendQueue.offer(msg);
    }

    //将收到的字符串放到接收队列
    public void pullMsg(String msg) {
        recvQueue.offer(msg);
    }

    //从接收队列拿取指令
    public String getMsg() {
        return recvQueue.poll();
    }
}
