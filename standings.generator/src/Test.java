import contest.ContestType;
import standings.StandingsConfig;
import standings.StandingsGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) {
        StandingsConfig config = new StandingsConfig();
        StandingsGenerator generator = new StandingsGenerator();

        config.showZeros = true;
        config.standingsType = ContestType.IOI;

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("test.html"), StandardCharsets.UTF_8))) {
            try {
                writer.write(generator.generate(new File("test_ioi.xml"), config));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
