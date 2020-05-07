package standings;

import contest.ContestType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import contest.Problem;
import contest.ParticipantProblemInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StandingsGenerator {
    private String headData =
            "<meta http-equiv=\"Content-type\" content='text/html; charset=utf-8' />" +
            "<link href=\"../standings.css\" rel=\"stylesheet\" type=\"text/css\" />" +
            "<link rel=\"stylesheet\" href=\"../ejudge/unpriv.css\" type=\"text/css\"/>\n" +
            "<link rel=\"stylesheet\" href=\"../ejudge/unpriv3.css\" type=\"text/css\"/>\n" +
            "<link rel=\"stylesheet\" href=\"../ejudge/priv.css\" type=\"text/css\"/>\n" +
            "<link rel=\"stylesheet\" href=\"../ejudge/ejudge3_ss.css\" type=\"text/css\"/>\n" +
            "<script type=\"text/javascript\" charset=\"UTF-8\" src=\"../ejudge/priv.js\"></script>" +
            "<script src=\"../ejudge/jquery.min.js\"></script>\n" +
            "    <script>\n" +
            "        $(function () {\n" +
            "            $(\"#menuContestSelector\").load(\"../menu.html\");\n" +
            "            $(\"#footerCopyright\").load(\"../footer.html\");\n" +
            "        });\n" +
            "    </script>";

    private String bodyUpData =
            "<div id=\"main-cont\"><div id=\"container\">\n" +
            "<div id=\"menuContestSelector\"></div>" +
            "<div align=\"center\"><h3>%s</h3></div>";
    private String bodyDownData = "<div id=\"footerCopyright\"></div></div></div>";

    private int calculateScore(final List<ParticipantProblemInfo> problems, final StandingsConfig config) {
        int score = 0;

        for (final ParticipantProblemInfo info : problems) {
            if (config.standingsType == ContestType.ICPC) {
                if (info.isSolved) {
                    score++;
                }
            } else {
                score += info.score;
            }
        }

        return score;
    }

    private int calculatePenalty(final List<ParticipantProblemInfo> problems, final StandingsConfig config) {
        if (config.standingsType == ContestType.IOI) {
            return 0;
        }

        int penalty = 0;
        for (final ParticipantProblemInfo info : problems) {
            if (info.isSolved) {
                penalty += (info.lastRunTime) / 60 + 20 * info.runsCount;
            }
        }

        return penalty;
    }

    public String generate(final File inputXMLFile, final StandingsConfig config) throws ParserConfigurationException, SAXException, IOException {
        StringBuilder html = new StringBuilder();

        html.append("<html>\n");
        html.append("<head>\n");
        html.append(String.format("<title>%s</title>", config.standingsTitle)).append("\n");
        html.append(headData).append("\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(String.format(bodyUpData, config.standingsTitle)).append("\n");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document document = dBuilder.parse(inputXMLFile);

        document.getDocumentElement().normalize();

        NodeList userNodes = ((Element) document.getElementsByTagName("users").item(0)).getElementsByTagName("user");
        NodeList problemNodes = ((Element) document.getElementsByTagName("problems").item(0)).getElementsByTagName("problem");
        NodeList runNodes = ((Element) document.getElementsByTagName("runs").item(0)).getElementsByTagName("run");

        Map<Integer, String> users = new HashMap<>();
        Map<Integer, Problem> problems = new HashMap<>();
        Map<Integer, Integer> userVirtualStart = new HashMap<>();


        for (int i = 0; i < userNodes.getLength(); i++) {
            Element user = (Element) userNodes.item(i);
            users.put(Integer.parseInt(user.getAttribute("id")), user.getAttribute("name"));
        }

        for (int i = 0; i < problemNodes.getLength(); i++) {
            Element problem = (Element) problemNodes.item(i);
            problems.put(Integer.parseInt(problem.getAttribute("id")), new Problem(
                    problem.getAttribute("short_name"),
                    problem.getAttribute("long_name")
            ));
        }

        Map<Integer, List<ParticipantProblemInfo>> standings = new HashMap<>();
        for (final Integer userID : users.keySet()) {
            standings.put(userID, new ArrayList<>());

            for (int i = 0; i < problems.size(); i++) {
                standings.get(userID).add(new ParticipantProblemInfo());
            }
        }

        Set<Integer> solved = new HashSet<>();
        List<Integer> firstAC = new ArrayList<>(problems.size());

        for (int i = 0; i < problems.size(); i++) {
            firstAC.set(i, (int)1e9);
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

                    boolean needLastAC = config.needLastAC.contains(problems.get(problemID + 1).shortName);

                    if (!standings.get(userID).get(problemID).isSolved || needLastAC) {
                        standings.get(userID).get(problemID).isSolved = true;
                        standings.get(userID).get(problemID).lastRunTime = time;

                        if (!solved.contains(problemID)) {
                            solved.add(problemID);

                            if (firstAC.get(problemID) > time) {
                                firstAC.set(problemID, time);
                            }
//                            standings.get(userID).get(problemID).isFirstAC = true;
                        }

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

                    boolean needLastAC = config.needLastAC.contains(problems.get(problemID + 1).shortName);

                    if (standings.get(userID).get(problemID).score <= score || needLastAC) {
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

                    boolean needLastAC = config.needLastAC.contains(problems.get(problemID + 1).shortName);

                    if (!standings.get(userID).get(problemID).isSolved || needLastAC) {
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

        for (int i = 0; i < runNodes.getLength(); i++) {
            Element run = (Element) runNodes.item(i);

            Integer time = Integer.parseInt(run.getAttribute("time"));
            String status = run.getAttribute("status");
            Integer userID = Integer.parseInt(run.getAttribute("user_id"));

            if (status.equals("OK")) {
                int problemID = Integer.parseInt(run.getAttribute("prob_id")) - 1;

                if (firstAC.get(problemID).equals(time)) {
                    standings.get(userID).get(problemID).isFirstAC = true;
                }
            }
        }

        html.append("<table class=\"new-standings\">\n");
        html.append("<tbody>\n");

        // Make title
        html.append("<tr>\n");
        html.append("<th><div class=\"new-standings-cell\" valign=\"middle\">Место</div></th>\n");
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

        Comparator<List<ParticipantProblemInfo>> comparator = (list1, list2) -> {
            if (config.standingsType == ContestType.IOI) {
                int score1 = 0, score2 = 0;

                for (final ParticipantProblemInfo info : list1) {
                    score1 += info.score;
                }
                for (final ParticipantProblemInfo info : list2) {
                    score2 += info.score;
                }

                return -Integer.compare(score1, score2);
            } else {
                int solved1 = 0, solved2 = 0;
                int penalty1 = 0, penalty2 = 0;

                for (final ParticipantProblemInfo info : list1) {
                    if (!info.isSolved) {
                        continue;
                    }

                    solved1++;
                    penalty1 += (info.lastRunTime) / 60 + 20 * info.runsCount;
                }
                for (final ParticipantProblemInfo info : list2) {
                    if (!info.isSolved) {
                        continue;
                    }

                    solved2++;
                    penalty2 += (info.lastRunTime) / 60 + 20 * info.runsCount;
                }

                if (solved1 == solved2) {
                    return Integer.compare(penalty1, penalty2);
                } else {
                    return -Integer.compare(solved1, solved2);
                }
            }
        };

        Comparator<Map.Entry<Integer, List<ParticipantProblemInfo>>> fullComparator = (entry1, entry2) -> {
            if (comparator.compare(entry1.getValue(), entry2.getValue()) == 0) {
                return users.get(entry1.getKey()).compareTo(users.get(entry2.getKey()));
            } else {
                return comparator.compare(entry1.getValue(), entry2.getValue());
            }
        };

        List<Map.Entry<Integer, List<ParticipantProblemInfo>>> sortedStandings = standings.entrySet().stream()
                .sorted(fullComparator)
                .collect(Collectors.toList());

        Map<Integer, Integer> lowerPlace = new HashMap<>();
        Map<Integer, Integer> upperPlace = new HashMap<>();
        Map<Integer, Integer> idPlace = new HashMap<>();

        int l = 0;
        int curId = 0;
        lowerPlace.put(sortedStandings.get(0).getKey(), 1);

        int oldScore = calculateScore(sortedStandings.get(0).getValue(), config);
        int oldPenalty = calculatePenalty(sortedStandings.get(0).getValue(), config);

        for (int r = 1; r < sortedStandings.size(); r++) {
            int score = calculateScore(sortedStandings.get(r).getValue(), config);
            int penalty = calculatePenalty(sortedStandings.get(r).getValue(), config);

            if (score != oldScore || penalty != oldPenalty) {
                int lower = l + 1;
                int upper = r;

                for (int i = l; i < r; i++) {
                    lowerPlace.put(sortedStandings.get(i).getKey(), lower);
                    upperPlace.put(sortedStandings.get(i).getKey(), upper);
                }

                l = r;
                oldScore = score;
                oldPenalty = penalty;
            }
        }

        for (int i = l; i < sortedStandings.size(); i++) {
            lowerPlace.put(sortedStandings.get(i).getKey(), l + 1);
            upperPlace.put(sortedStandings.get(i).getKey(), sortedStandings.size());
        }

        oldScore = calculateScore(sortedStandings.get(0).getValue(), config);
        idPlace.put(sortedStandings.get(0).getKey(), curId);

        for (int i = 1; i < sortedStandings.size(); i++) {
            int score = calculateScore(sortedStandings.get(i).getValue(), config);

            if (score != oldScore) {
                curId++;
                oldScore = score;
            }

            idPlace.put(sortedStandings.get(i).getKey(), curId);
        }

        int parity = 0;
        int prevId = idPlace.get(sortedStandings.get(0).getKey());
        for (Map.Entry<Integer, List<ParticipantProblemInfo>> participant : sortedStandings) {
            StringBuilder cur = new StringBuilder();

            int id = idPlace.get(participant.getKey());

            if (id != prevId) {
                parity = 0;
            }

            if (id % 2 == 0) {
                if (parity % 2 == 0) {
                    cur.append("<tr class=\"r00\">\n");
                } else {
                    cur.append("<tr class=\"r01\">\n");
                }
            } else {
                if (parity % 2 == 0) {
                    cur.append("<tr class=\"r10\">\n");
                } else {
                    cur.append("<tr class=\"r11\">\n");
                }
            }

            prevId = id;

            int lower = lowerPlace.get(participant.getKey());
            int upper = upperPlace.get(participant.getKey());

            if (lower != upper) {
                cur.append(String.format("<td class=\"stat\" valign=\"middle\"><div class=\"new-standings-cell\">%d-%d</div></td>\n", lowerPlace.get(participant.getKey()), upperPlace.get(participant.getKey())));
            } else {
                cur.append(String.format("<td class=\"stat\" valign=\"middle\"><div class=\"new-standings-cell\">%d</div></td>\n", lower));
            }
            cur.append(String.format("<td>%s</td>", users.get(participant.getKey()))).append("\n");

            int score = 0;
            int penalty = 0;

            for (ParticipantProblemInfo participantProblemInfo : participant.getValue()) {
                if (config.standingsType == ContestType.IOI) {
                    if (participantProblemInfo.isSolved || participantProblemInfo.runsCount != 0) {
                        if (participantProblemInfo.isSolved) {
                            cur.append("<td class=\"ok\">");
                        } else {
                            cur.append("<td class=\"rj\">");
                        }
                        cur.append(participantProblemInfo.score);
                        score += participantProblemInfo.score;
                    } else {
                        cur.append("<td>");
                    }
                } else {
                    if (participantProblemInfo.isSolved) {
                        if (participantProblemInfo.isFirstAC) {
                            cur.append("<td class=\"firstAC\">");
                        } else {
                            cur.append("<td class=\"ok\">");
                        }

                        cur.append("+");

                        if (participantProblemInfo.runsCount != 0) {
                            cur.append(participantProblemInfo.runsCount);
                        }

                        score++;
                        int curPenalty = (participantProblemInfo.lastRunTime) / 60;
                        penalty += curPenalty + participantProblemInfo.runsCount * 20;

                        int h = curPenalty / 60;
                        int m = curPenalty % 60;

                        cur.append(String.format("<div>%d:%02d</div>", h, m));
                    } else if (participantProblemInfo.runsCount != 0) {
                        cur.append("<td class=\"rj\">");
                        cur.append("-").append(participantProblemInfo.runsCount);

                        int curPenalty = (participantProblemInfo.lastRunTime) / 60;
                        int h = curPenalty / 60;
                        int m = curPenalty % 60;

                        cur.append(String.format("<div>%d:%02d</div>", h, m));
                    } else {
                        cur.append("<td>");
                    }
                }

                cur.append("</td>\n");
            }

            cur.append("<td class=\"stat\">").append(score).append("</td>\n");
            if (config.standingsType == ContestType.ICPC) {
                cur.append("<td class=\"stat\">").append(penalty).append("</td>\n");
            }

            cur.append("</tr>\n");

            if (score != 0 || config.showZeros) {
                html.append(cur);
            }

            parity ^= 1;
        }

        html.append("</tbody>\n");
        html.append("</table>\n");

        html.append(bodyDownData).append("\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
