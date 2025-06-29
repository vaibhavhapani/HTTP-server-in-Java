package org.example;

public class Main {
    public static void main(String[] args) {
        String directory = null;

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--directory")) {
                directory = args[i + 1];
                break;
            }
        }

        HttpServer server = new HttpServer(4221, directory);
        server.start();
    }
}
