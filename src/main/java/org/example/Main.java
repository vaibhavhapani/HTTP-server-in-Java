package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try {
            // Create a server socket that listens on port 4221
            // This is the entry point for any client trying to connect to the server.
            ServerSocket serverSocket = new ServerSocket(4221);

            // This tells the OS to allow reusing the port quickly after the server is stopped and restarted.
            // To avoid "Address already in use" error.
            serverSocket.setReuseAddress(true);

            Socket clientConnection = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}