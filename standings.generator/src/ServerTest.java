import server.StandingsServer;

public class ServerTest {
    public static void main(String[] args) {
        try {
            StandingsServer server = new StandingsServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
