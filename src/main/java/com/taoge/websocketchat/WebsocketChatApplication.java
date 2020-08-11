package com.taoge.websocketchat;

import com.taoge.websocketchat.netty.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class WebsocketChatApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(WebsocketChatApplication.class, args);
        Environment env = application.getEnvironment();
        String host = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");

        System.out.println("[----------------------------------------------------------]");
        System.out.println("聊天室启动成功！点击进入:\t http://" + host + ":" + port);
        System.out.println("[----------------------------------------------------------");

        WebSocketServer.inst.start();
    }

}
