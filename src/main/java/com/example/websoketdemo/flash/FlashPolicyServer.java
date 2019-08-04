package com.example.websoketdemo.flash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

@Component
public class FlashPolicyServer {

    private boolean isStart = false;

    private ServerSocket serverSocket;

    private static Thread serverThread;

    @Value(value = "${flash.policy.server.port}")
    private int port;

    private static boolean listening = true;

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void start() {
        try {
            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.debug(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:run", "FlashPolicyServer: 启动，端口：" + port);
                        System.out.println("port:"+port);
                        serverSocket = new ServerSocket(port);

                        while (listening) {
                            final Socket socket = serverSocket.accept();
                            threadPoolTaskExecutor.execute(new FlashPolicyRunnable(socket));
                        }
                    } catch (IOException ex) {
                        LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "PolicyServerServlet", "启动FlashPolicyServer出错：" + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

            });
            serverThread.start();
            if (!this.getIsStart()) {
                this.setIsStart(true);
            }
        } catch (Exception ex) {
            LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "PolicyServerServlet",
                    "启动FlashPolicyServer出错：" + ex.getMessage());
            ex.printStackTrace();
        }

    }

    public void stop() {
        LOGGER.debug(MarkerFactory.getMarker("FlashPolicyServer:stop"), "[{}] - {}", "PolicyServerServlet",
                "正在关闭FlashPolicyServer-------------------：");
        if (listening) {
            listening = false;
        }
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ex) {
                LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:stop"), "[{}] - {}", "PolicyServerServlet",
                        "关闭FlashPolicyServer出错：" + ex.getMessage());
            }
        }

        if (this.getIsStart()) {
            this.setIsStart(false);
        }
    }

    public boolean getIsStart() {
        return isStart;
    }

    public void setIsStart(boolean start) {
        isStart = start;
    }


    class FlashPolicyRunnable implements Runnable {
        Socket socket = null;

        FlashPolicyRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", "FlashPolicyServer: 处理请求");
                }

                socket.setSoTimeout(30000);

                InputStream in = socket.getInputStream();

                byte[] buffer = new byte[23];

                if (in.read(buffer) != -1 && (new String(buffer, "ISO-8859-1")).startsWith("<policy-file-request/>")) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", "PolicyServerServlet: Serving Policy File...");
                    }
                    OutputStream out = socket.getOutputStream();

                    byte[] bytes = ("<?xml version=\"1.0\"?>\n" +
                            "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">\n" +
                            "<cross-domain-policy> \n" +
                            "   <site-control permitted-cross-domain-policies=\"master-only\"/>\n" +
                            "   <allow-access-from domain=\"*\" to-ports=\"*\" />\n" +
                            "</cross-domain-policy>").getBytes("ISO-8859-1");

                    out.write(bytes);
                    out.write(0x00);

                    out.flush();
                    out.close();
                } else {
                    LOGGER.warn(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", "FlashPolicyServer: Ignoring Invalid Request");
                    LOGGER.warn("  " + (new String(buffer)));
                }
            } catch (SocketException e) {
                LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", e.getMessage());
            } catch (IOException e) {
                LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (Exception ex2) {
                    LOGGER.error(MarkerFactory.getMarker("FlashPolicyServer:run"), "[{}] - {}", "FlashPolicyServer:listening", "socket关闭异常：" + ex2.getMessage());
                }
            }
        }
    }

}