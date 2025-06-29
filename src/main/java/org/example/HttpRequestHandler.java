package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class HttpRequestHandler {
    private final Socket clientSocket;
    private final String directory;

    public HttpRequestHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    public void handle() throws IOException {
        System.out.println("Handling on thread: " + Thread.currentThread().getName());

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                HttpUtils.sendResponse(out, "400 Bad Request", null, "");
                return;
            }

            String method = parts[0];
            String path = parts[1];
            String version = parts[2];

            if (!version.equals("HTTP/1.1")) {
                HttpUtils.sendResponse(out, "400 Bad Request", null, "");
                return;
            }

            if (!method.equals("GET") && !method.equals("PUT")) {
                HttpUtils.sendResponse(out, "405 Method Not Allowed", null, "");
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

            if (path.equals("/user-agent")) {
                HttpUtils.sendResponse(out, "200 OK", "text/plain", userAgent);
            } else if (path.startsWith("/echo/")) {
                String echo = path.substring("/echo/".length());
                HttpUtils.sendResponse(out, "200 OK", "text/plain", echo);
            } else if (path.startsWith("/files/")) {
                handleFileRequest(out, in, path, method, contentLength);
            } else if (path.equals("/")) {
                HttpUtils.sendResponse(out, "200 OK", null, "");
            } else {
                HttpUtils.sendResponse(out, "404 Not Found", null, "");
            }

        } finally {
            clientSocket.close();
        }
    }

    private void handleFileRequest(BufferedWriter out, BufferedReader in, String path, String method, int contentLength) throws IOException {
        if (directory == null) {
            HttpUtils.sendResponse(out, "500 Internal Server Error", null, "");
            return;
        }

        String filename = path.substring("/files/".length());
        File file = new File(directory, filename);

        switch (method) {
            case "PUT": {
                char[] buffer = new char[contentLength];
                in.read(buffer, 0, contentLength);
                String body = new String(buffer);

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(body);
                }

                HttpUtils.sendResponse(out, "201 Created", null, "");
                break;
            }

            case "PATCH": {
                if (file.exists() && file.isFile()) {
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    String body = new String(buffer);

                    try (FileWriter writer = new FileWriter(file, true)) { // true → append mode
                        writer.write(body);
                    }

                    HttpUtils.sendResponse(out, "204 No Content", null, "");
                } else {
                    HttpUtils.sendResponse(out, "404 Not Found", null, "");
                }
                break;
            }

            case "DELETE": {
                if (file.exists() && file.isFile()) {
                    if (file.delete()) {
                        HttpUtils.sendResponse(out, "200 OK", null, "");
                    } else {
                        HttpUtils.sendResponse(out, "500 Internal Server Error", null, "");
                    }
                } else {
                    HttpUtils.sendResponse(out, "404 Not Found", null, "");
                }
                break;
            }

            case "GET": {
                if (file.exists() && file.isFile()) {
                    byte[] content = Files.readAllBytes(file.toPath());

                    out.write("HTTP/1.1 200 OK\r\n");
                    out.write("Content-Type: application/octet-stream\r\n");
                    out.write("Content-Length: " + content.length + "\r\n");
                    out.write("\r\n");
                    out.flush();

                    clientSocket.getOutputStream().write(content);
                } else {
                    HttpUtils.sendResponse(out, "404 Not Found", null, "");
                }
                break;
            }

            default: {
                HttpUtils.sendResponse(out, "405 Method Not Allowed", null, "");
                break;
            }
        }
    }
}
