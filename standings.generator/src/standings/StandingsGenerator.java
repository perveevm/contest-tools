package standings;

import contest.ContestType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import contest.Problem;
import contest.ProblemInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StandingsGenerator {
    private String headData = "<meta http-equiv=\"Content-type\" content='text/html; charset=utf-8' /><style>table, th, td { border: 1px solid black; }</style>";

    private String bodyUpData = "";
    private String bodyDownData = "";

    public String generate(final File inputXMLFile, final StandingsConfig config) throws ParserConfigurationException, SAXException, IOException {
        StringBuilder html = new StringBuilder();

        html.append("<html>\n");
        html.append("<head>\n");
        html.append(String.format("<title>%s</title>", config.standingsTitle)).append("\n");
        html.append(headData).append("\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(bodyUpData).append("\n");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(inputXMLFile);

        System.out.println("Parsing file: " + inputXMLFile.getAbsolutePath());

        document.getDocumentElement().normalize();

        NodeList userNodes = ((Element) document.getElementsByTagName("users").item(0)).getElementsByTagName("user");
        NodeList problemNodes = ((Element) document.getElementsByTagName("problems").item(0)).getElementsByTagName("contest");
        NodeList runNodes = ((Element) document.getElementsByTagName("runs").item(0)).getElementsByTagName("run");

        Map<Integer, String> users = new HashMap<>();
        Map<Integer, Problem> problems = new HashMap<>();
        Map<Integer, Integer> userVirtualStart = new HashMap<>();


        for (int i = 0; i < userNodes.getLength(); i++) {
            Element user = (Element) userNodes.item(i);
            users.put(Integer.parseInt(user.getAttribute("id")), user.getAttribute("name"));
            System.out.println(user.getAttribute("name"));
        }

        for (int i = 0; i < problemNodes.getLength(); i++) {
            Element problem = (Element) problemNodes.item(i);
            problems.put(Integer.parseInt(problem.getAttribute("id")), new Problem(
                    problem.getAttribute("short_name"),
                    problem.getAttribute("long_name")
            ));
        }

        Map<Integer, List<ProblemInfo>> standings = new HashMap<>();
        for (final Integer userID : users.keySet()) {
            standings.put(userID, new ArrayList<>());

            for (int i = 0; i < problems.size(); i++) {
                standings.get(userID).add(new ProblemInfo());
            }
        }

        for (int i = 0; i < runNodes.getLength(); i++) {
            Element run = (Element) runNodes.item(i);

            Integer time = Integer.parseInt(run.getAttribute("time"));
            String status = run.getAttribute("status");
            Integer userID = Integer.parseInt(run.getAttribute("user_id"));

            switch (status) {
                case "VS":
                    userVirtualStart.put(userID, time);
                    break;
                case "OK": {
                    int problemID = Integer.parseInt(run.getAttribute("prob_id")) - 1;

                    if (!standings.get(userID).get(problemID).isSolved) {
                        standings.get(userID).get(problemID).isSolved = true;
                        standings.get(userID).get(problemID).lastRunTime = time;

                        if (userVirtualStart.containsKey(userID)) {
                            standings.get(userID).get(problemID).lastRunTime -= userVirtualStart.get(userID);
                        }

                        if (!run.getAttribute("score").isEmpty()) {
                            standings.get(userID).get(problemID).score = Integer.parseInt(run.getAttribute("score"));
                        }
                    }
                    break;
                }
                case "PT": {
                    int problemID = Integer.parseInt(run.getAttribute("prob_id")) - 1;
                    int score = Integer.parseInt(run.getAttribute("score"));

                    if (standings.get(userID).get(problemID).score < score) {
                        standings.get(userID).get(problemID).score = score;
                        standings.get(userID).get(problemID).lastRunTime = time;

                        if (userVirtualStart.containsKey(userID)) {
                            standings.get(userID).get(problemID).lastRunTime -= userVirtualStart.get(userID);
                        }

                        standings.get(userID).get(problemID).runsCount++;
                    }
                    break;
                }
                case "WA":
                case "RT":
                case "TL":
                case "PE":
                case "ML":
                case "SE":
                case "WT": {
                    int problemID = Integer.parseInt(run.getAttribute("prob_id")) - 1;

                    if (!standings.get(userID).get(problemID).isSolved) {
                        standings.get(userID).get(problemID).runsCount++;
                        standings.get(userID).get(problemID).lastRunTime = time;

                        if (userVirtualStart.containsKey(userID)) {
                            standings.get(userID).get(problemID).lastRunTime -= userVirtualStart.get(userID);
                        }
                    }
                    break;
                }
            }
        }

        html.append("<table width=\"100%\">\n");
        html.append("<tbody>\n");

        // Make title
        html.append("<tr>\n");
        html.append("<th>Место</th>\n");
        html.append("<th>Участник</th>\n");

        for (final Map.Entry<Integer, Problem> problemEntry : problems.entrySet()) {
            html.append(String.format("<th>%s</th>\n", problemEntry.getValue().shortName));
        }

        if (config.standingsType == ContestType.IOI) {
            html.append("<th>Набранный балл</th>\n");
        } else {
            html.append("<th>Решено задач</th>\n");
            html.append("<th>Штраф</th>\n");
        }

        html.append("</tr>\n");

        Comparator<List<ProblemInfo>> comparator = (list1, list2) -> {
            if (config.standingsType == ContestType.IOI) {
                int score1 = 0, score2 = 0;

                for (final ProblemInfo info : list1) {
                    score1 += info.score;
                }
                for (final ProblemInfo info : list2) {
                    score2 += info.score;
                }

                return -Integer.compare(score1, score2);
            } else {
                int solved1 = 0, solved2 = 0;
                int penalty1 = 0, penalty2 = 0;

                for (final ProblemInfo info : list1) {
                    if (!info.isSolved) {
                        continue;
                    }

                    solved1++;
                    penalty1 += (info.lastRunTime + 59) / 60 + 20 * info.runsCount;
                }
                for (final ProblemInfo info : list2) {
                    if (!info.isSolved) {
                        continue;
                    }

                    solved2++;
                    penalty2 += (info.lastRunTime + 59) / 60 + 20 * info.runsCount;
                }

                if (solved1 == solved2) {
                    return Integer.compare(penalty1, penalty2);
                } else {
                    return -Integer.compare(solved1, solved2);
                }
            }
        };

        Comparator<Map.Entry<Integer, List<ProblemInfo>>> fullComparator = (entry1, entry2) -> {
            if (comparator.compare(entry1.getValue(), entry2.getValue()) == 0) {
                return users.get(entry1.getKey()).compareTo(users.get(entry2.getKey()));
            } else {
                return comparator.compare(entry1.getValue(), entry2.getValue());
            }
        };

        List<Map.Entry<Integer, List<ProblemInfo>>> sortedStandings = standings.entrySet().stream()
                .sorted(fullComparator)
                .collect(Collectors.toList());

        for (Map.Entry<Integer, List<ProblemInfo>> participant : sortedStandings) {
            StringBuilder cur = new StringBuilder();

            cur.append("<tr>\n");

            cur.append("<td>???</td>\n");
            cur.append(String.format("<td>%s</td>", users.get(participant.getKey()))).append("\n");

            int score = 0;
            int penalty = 0;

            for (ProblemInfo problemInfo : participant.getValue()) {
                cur.append("<td>");

                if (config.standingsType == ContestType.IOI) {
                    if (problemInfo.isSolved || problemInfo.runsCount != 0) {
                        cur.append(problemInfo.score);
                        score += problemInfo.score;
                    }
                } else {
                    if (problemInfo.isSolved) {
                        cur.append("+");

                        if (problemInfo.runsCount != 0) {
                            cur.append(problemInfo.runsCount);
                        }

                        score++;
                        penalty += (problemInfo.lastRunTime + 59) / 60 + 20 * problemInfo.runsCount;
                    } else if (problemInfo.runsCount != 0) {
                        cur.append("-").append(problemInfo.runsCount);
                    }
                }

                cur.append("</td>\n");
            }

            cur.append("<td>").append(score).append("</td>\n");
            if (config.standingsType == ContestType.ICPC) {
                cur.append("<td>").append(penalty).append("</td>\n");
            }

            cur.append("</tr>\n");

            if (score != 0 || config.showZeros) {
                html.append(cur);
            }
        }

        html.append("</tbody>\n");
        html.append("</table>\n");

        html.append(bodyDownData).append("\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
