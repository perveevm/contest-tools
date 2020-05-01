package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import standings.StandingsConfig;
import standings.StandingsGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class HttpStandingsHandler implements HttpHandler {
    private static final String CONFIG_PATH = "/etc/new-standings";
    private final String judgesDir;

    public HttpStandingsHandler(final String judgesDir) {
        this.judgesDir = judgesDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().toString().equals("config")) {
            exchange.sendResponseHeaders(403, 0);
            exchange.close();
            System.out.println("Access to config.xml denied");
            return;
        }

        if (exchange.getRequestURI().toString().equals("/")) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            System.out.println("There is no contest at root");
            return;
        }

        String requestStandingsName = exchange.getRequestURI().toString();
        Path path;

        try {
            path = Path.of(CONFIG_PATH + requestStandingsName + ".xml");
        } catch (InvalidPathException e) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            System.out.println("There is no config path: " + e.getMessage());
            return;
        }

        if (!Files.exists(path)) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            System.out.println("There is no config path");
            return;
        }

        StandingsConfig config = new StandingsConfig();

        try {
            config.readFromConfigFile(path);
        } catch (Exception e) {
            exchange.sendResponseHeaders(501, 0);
            exchange.close();
            System.out.println("Error reading config: " + e.getMessage());
            return;
        }

        Path submits;
        try {
            submits = Path.of(String.format("%s/%06d/var/status/dir/external.xml", judgesDir, config.contestID));
        } catch (InvalidPathException e) {
            exchange.sendResponseHeaders(501, 0);
            exchange.close();
            System.out.println("Error getting log: " + e.getMessage());
            return;
        }

        if (!Files.exists(submits)) {
            exchange.sendResponseHeaders(501, 0);
            exchange.close();
            System.out.println("Logs don't exist: " + submits.toString());
            return;
        }

        StandingsGenerator generator = new StandingsGenerator();

        try {
            String html = generator.generate(submits.toFile(), config);
            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(html.getBytes());
            stream.close();
        } catch (Exception e) {
            exchange.sendResponseHeaders(501, 0);
            exchange.close();
            System.out.println("Error generating standings: " + e.getMessage());
        }
    }
}
