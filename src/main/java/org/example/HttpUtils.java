package org.example;

import java.io.BufferedWriter;
import java.io.IOException;

public class HttpUtils {
    public static void sendResponse(BufferedWriter out, String status, String contentType, String body) throws IOException {
        if (contentType == null) contentType = "text/plain";
        if (body == null) body = "";

        out.write("HTTP/1.1 " + status + "\r\n");
        out.write("Content-Type: " + contentType + "\r\n");
        out.write("Content-Length: " + body.getBytes().length + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
