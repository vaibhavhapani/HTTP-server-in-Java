package org.example;

import java.io.*;
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

            handleConnection(clientConnection);

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static void handleConnection(Socket clientConnection) throws IOException {
        try {
            InputStream inputStream = clientConnection.getInputStream(); // to get the byte-based input stream from the client socket
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream); // to wrap it with InputStreamReader to convert bytes to characters
            BufferedReader in = new BufferedReader(inputStreamReader); // to wrap the InputStreamReader with BufferedReader for efficient reading of lines

            OutputStream outputStream = clientConnection.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter out = new BufferedWriter(outputStreamWriter);

            out.write("HTTP/1.1 200 OK\r\n\r\n");
            out.flush();
        } catch (IOException e){
            System.out.println("IOException: " + e.getMessage());
        }
    }
}