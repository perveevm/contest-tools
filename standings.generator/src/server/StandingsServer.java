package server;

import com.sun.net.httpserver.HttpServer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StandingsServer implements AutoCloseable {
    private final HttpServer server;

    private static final String CONFIG_PATH = "/etc/new-standings/config.xml";

    public StandingsServer() throws IOException, ParserConfigurationException, SAXException {
        Path configPath = Paths.get(CONFIG_PATH);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(configPath.toFile());

        document.getDocumentElement().normalize();

        NodeList judgesDirNode = document.getElementsByTagName("judges");
        NodeList portNode = document.getElementsByTagName("port");
        NodeList hostNode = document.getElementsByTagName("host");

        String judgesDir = "/home/judges";
        int port = 8080;
        String host = "127.0.0.1";

        if (judgesDirNode.getLength() != 0) {
            judgesDir = judgesDirNode.item(0).getTextContent();
        }
        if (portNode.getLength() != 0) {
            port = Integer.parseInt(portNode.item(0).getTextContent());
        }
        if (hostNode.getLength() != 0) {
            host = hostNode.item(0).getTextContent();
        }

        server = HttpServer.create();
        server.bind(new InetSocketAddress(host, port), 1000);
        server.createContext("/", new HttpStandingsHandler(judgesDir));
        server.start();

        System.out.println("Started server at host: " + host + ", port: " + port);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
