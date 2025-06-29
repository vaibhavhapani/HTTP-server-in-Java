package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class Main {
    private static String servingDirectory;

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        // Extract --directory argument
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--directory")) {
                servingDirectory = args[i + 1];
                break;
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientConnection = serverSocket.accept();
                System.out.println("Accepted new connection");

                // Handle each request on a separate thread
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
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket client) throws IOException {
        System.out.println("\nHandling on thread: " + Thread.currentThread().getName());

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Incoming request: " + requestLine);
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                sendResponse(out, "400 Bad Request", "text/plain", "");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String httpVersion = requestParts[2];

            if (!httpVersion.equals("HTTP/1.1")) {
                sendResponse(out, "400 Bad Request", "text/plain", "");
                return;
            }

            if (!method.equals("GET") && !method.equals("PUT")) {
                sendResponse(out, "405 Method Not Allowed", "text/plain", "");
                return;
            }

            String headerLine;
            String userAgent = "";
            int contentLength = 0;

            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("user-agent:")) {
                    userAgent = headerLine.split(":", 2)[1].trim();
                } else if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":", 2)[1].trim());
                }
            }

            // Route handling
            if (path.equals("/user-agent")) {
                sendResponse(out, "200 OK", "text/plain", userAgent);

            } else if (path.startsWith("/echo/")) {
                String responseBody = path.substring("/echo/".length());
                sendResponse(out, "200 OK", "text/plain", responseBody);

            } else if (path.startsWith("/files/")) {
                handleFileRequest(method, path, in, out, client, contentLength);

            } else if (path.equals("/")) {
                sendResponse(out, "200 OK", "text/plain", "");

            } else {
                sendResponse(out, "404 Not Found", "text/plain", "");
            }

        } finally {
            client.close();
        }
    }

    private static void handleFileRequest(String method, String path, BufferedReader in,
                                          BufferedWriter out, Socket client, int contentLength) throws IOException {
        if (servingDirectory == null) {
            System.out.println("Missing --directory argument");
            sendResponse(out, "500 Internal Server Error", "text/plain", "");
            return;
        }

        String filename = path.substring("/files/".length());
        File file = new File(servingDirectory, filename);

        if (method.equals("PUT")) {
            char[] buffer = new char[contentLength];
            in.read(buffer, 0, contentLength);
            String requestBody = new String(buffer);

            try (FileWriter fw = new FileWriter(file)) {
                fw.write(requestBody);
            }

            sendResponse(out, "201 Created", "text/plain", "");
        } else if (method.equals("GET")) {
            if (file.exists() && file.isFile()) {
                byte[] content = Files.readAllBytes(file.toPath());

                // Write headers
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: application/octet-stream\r\n");
                out.write("Content-Length: " + content.length + "\r\n");
                out.write("\r\n");
                out.flush();

                // Write body directly to socket
                client.getOutputStream().write(content);
            } else {
                sendResponse(out, "404 Not Found", "text/plain", "");
            }
        }
    }

    private static void sendResponse(BufferedWriter out, String status, String contentType, String body) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        out.write("Content-Type: " + contentType + "\r\n");
        out.write("Content-Length: " + body.getBytes().length + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
