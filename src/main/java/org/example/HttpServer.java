package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private final int port;
    private final String servingDirectory;

    public HttpServer(int port, String servingDirectory) {
        this.port = port;
        this.servingDirectory = servingDirectory;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread thread = new Thread(() -> {
                    try {
                        new HttpRequestHandler(clientSocket, servingDirectory).handle();
                    } catch (IOException e) {
                        System.err.println("Thread error: " + e.getMessage());
                    }
                });
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
