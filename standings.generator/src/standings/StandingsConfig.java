package standings;

import contest.ContestType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class StandingsConfig {
    public String standingsTitle = "Standings";
    public ContestType standingsType = ContestType.ICPC;

    public boolean showZeros = false;

    public int contestID = 1;

    // TODO: use this parameters
    public List<Integer> contestsID = Collections.emptyList();
    public boolean mergeEqualNames = true;

    public int freezeTime = 0;


    public void readFromConfigFile(final Path path) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(path.toFile());

        document.getDocumentElement().normalize();

        NodeList titleNodes = document.getElementsByTagName("title");
        NodeList standingsTypeNodes = document.getElementsByTagName("type");
        NodeList zerosNodes = document.getElementsByTagName("zeros");
        NodeList contestIDNode = document.getElementsByTagName("contests");

        if (titleNodes.getLength() != 0) {
            standingsTitle = titleNodes.item(0).getTextContent();
        }
        if (standingsTypeNodes.getLength() != 0) {
            switch (standingsTypeNodes.item(0).getTextContent()) {
                case "ICPC":
                    standingsType = ContestType.ICPC;
                    break;
                case "IOI":
                    standingsType = ContestType.IOI;
                    break;
                default:
                    throw new IllegalArgumentException("Expected IOI/ICPC, but found: " + standingsTypeNodes.item(0).getTextContent());
            }
        }
        if (zerosNodes.getLength() != 0) {
            switch (zerosNodes.item(0).getTextContent()) {
                case "true":
                    showZeros = true;
                    break;
                case "false":
                    showZeros = false;
                    break;
                default:
                    throw new IllegalArgumentException("Expected true/false, but found: " + zerosNodes.item(0).getTextContent());
            }
        }
        if (contestIDNode.getLength() != 0) {
            try {
                contestID = Integer.parseInt(contestIDNode.item(0).getTextContent());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Can't parse contest ID");
            }
        }
    }
}
