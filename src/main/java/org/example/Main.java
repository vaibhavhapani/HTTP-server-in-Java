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

            while (true) {
                Socket clientConnection = serverSocket.accept(); // Wait for connection from client.
                System.out.println("accepted new connection");

                // Spawns a thread for every incoming connection
                Thread thread = new Thread(() -> {
                    try {
                        handleConnection(clientConnection);
                    } catch (IOException e) {
                        System.out.println("Thread error: " + e.getMessage());
                    }
                });
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static void handleConnection(Socket clientConnection) throws IOException {
        System.out.println("\nHandling on thread: " + Thread.currentThread().getName());

        try (
                InputStream inputStream = clientConnection.getInputStream(); // to get the byte-based input stream from the client socket
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream); // to wrap it with InputStreamReader to convert bytes to characters
                BufferedReader in = new BufferedReader(inputStreamReader); // to wrap the InputStreamReader with BufferedReader for efficient reading of lines

                OutputStream outputStream = clientConnection.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                BufferedWriter out = new BufferedWriter(outputStreamWriter);
        ) {

            // Read the request: e.g. "GET /path HTTP/1.1"
            String request = in.readLine();
            System.out.println("Incoming request: " + request);

            if (request == null || request.isEmpty()) return;

            String[] parts = request.split(" ");
            if (parts.length < 2) {
                System.out.println("Invalid request.");
                return;
            }

            String method = parts[0];
            String urlPath = parts[1];
            System.out.println("Method: " + method + ", URL Path: " + urlPath);

            String header = in.readLine();
            String userAgent = "";

            while (header != null && !header.isEmpty()) {
                System.out.println("Header: " + header);

                if (header.toLowerCase().startsWith("user-agent: ")) {
                    userAgent = header.split(":", 2)[1].trim();
                    System.out.println("userAgent: " + userAgent);
                }

                header = in.readLine();
            }

            if ("/user-agent".equals((urlPath))) {
                String responseBody = userAgent;
                String response = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        responseBody.length(), responseBody
                );
                out.write(response);
            } else if (urlPath.startsWith("/echo/")) {
                String responseBody = urlPath.substring("/echo/".length());
                System.out.println("Response body: " + responseBody);

                String response = String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        responseBody.length(), responseBody);

                out.write(response);
            } else if ("/".equals(urlPath)) {
                out.write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
            } else {
                String response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
                out.write(response);
            }

            out.flush();

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            clientConnection.close();
        }
    }
}