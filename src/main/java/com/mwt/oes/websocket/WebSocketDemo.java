package com.mwt.oes.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * @ServerEndpoint 可以把当前类变成websocket服务类
 */
@CrossOrigin
@ServerEndpoint("/api/websocket/{userno}")
@Component
public class WebSocketDemo {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static ConcurrentHashMap<String, WebSocketDemo> webSocketSet = new ConcurrentHashMap<String, WebSocketDemo>();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    //当前发消息的人员编号
    private String userno = "";

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userno") String param, Session session, EndpointConfig config) {
        System.out.println(param);
        userno = param;//接收到发送消息的人员编号
        this.session = session;
        webSocketSet.put(param, this);//加入map中
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (!userno.equals("")) {
            webSocketSet.remove(userno);  //从set中删除
            subOnlineCount();           //在线数减1
            System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        //可以自己约定字符串内容，比如 内容|0 表示信息群发，内容|X 表示信息发给id为X的用户
//        String sendMessage = message.split("[|]")[0];
        String sendUserno = message.split("[|]")[1];
        //群发消息
        if (sendUserno.equals("0")) {
            sendAll(message);
        } else {
            //给指定的人发消息
            sendToUser(message);
        }

    }

    /**
     * 给指定的人发送消息
     * @param message
     */
    public void sendToUser(String message) {
        String sendUserno = message.split("[|]")[1];
        String sendMessage = message.split("[|]")[0];
        String now = getNowTime();
        try {
            if (webSocketSet.get(sendUserno) != null) {
//                webSocketSet.get(sendUserno).sendMessage(now + "用户" + userno + "发来消息：" + " <br/> " + sendMessage);
                webSocketSet.get(sendUserno).sendMessage(sendMessage);
//                webSocketSet.get(userno).sendMessage("发送成功，用户已接收");
                // 更新其他管理员的未读条数
                for (String key : webSocketSet.keySet()) {
                    if (key.length() == 6) {
                        webSocketSet.get(key).sendMessage(sendMessage);
                    }
                }
            } else {
                System.out.println("当前用户不在线");
                webSocketSet.get(userno).sendMessage(sendMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给所有人发消息
     * @param message
     */
    public void sendAll(String message) {
        String now = getNowTime();
        String sendMessage = message.split("[|]")[0];
        //遍历HashMap
        for (String key : webSocketSet.keySet()) {
            try {
                //判断接收用户是否是当前发消息的用户
                if (!userno.equals(key)) {
//                    webSocketSet.get(key).sendMessage(now + "用户" + userno + "发来消息：" + " <br/> " + sendMessage);
                    webSocketSet.get(key).sendMessage(sendMessage);
                    System.out.println("key = " + key);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取当前时间
     *
     * @return
     */
    private String getNowTime() {
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(date);
        return time;
    }
    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketDemo.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketDemo.onlineCount--;
    }

    public static ConcurrentHashMap<String, WebSocketDemo> getWebSocketDemo() {
        return webSocketSet;
    }

}
