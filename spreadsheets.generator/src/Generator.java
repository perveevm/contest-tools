import password.MarkovChain;
import password.MarkovPasswordGenerator;
import password.PasswordGenerator;
import password.RandomPasswordGenerator;
import spreadsheet.SpreadsheetGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main runnable class.
 */
public class Generator {
    public static void main(String[] args) {
        if (args[0].equals("-h")) {
            System.out.println("Using;");
            System.out.println("\t-h prints this message");
            System.out.println("\t-i inputFile -o outputFile [-e ejudgeOutputFile] [-l loginTemplate] [-p {random|markov}] [-pl passwordLen] [-s markovSourceFile] [-n nameTemplate columnNumberList] [-c if file has caption] [-sn sheetName]");
            return;
        }

        if (args[0].equals("-rp")) {
            if (args.length < 2) {
                System.out.println("Too less arguments");
                return;
            }

            String markovFile = args[1];
            try (BufferedReader reader = new BufferedReader(new FileReader(markovFile))) {
                Stream<String> lines = reader.lines();
                String res = lines.collect(Collectors.joining());
                MarkovChain chain = new MarkovChain(res.chars());
                PasswordGenerator generator = new MarkovPasswordGenerator(chain);
                System.out.println(generator.nextPassword(9));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            return;
        }

        String inputFile = null, outputFile = null;
        String ejudgeOutputFile = null;
        String markovFile = null;
        boolean needMarkovGenerator = false;
        boolean hasCaption = false;
        String sheetName = null;

        SpreadsheetGenerator generator = new SpreadsheetGenerator();

        int pos = 0;
        while (pos < args.length) {
            if (args[pos].equals("-c")) {
                hasCaption = true;
                pos++;
                continue;
            }

            if (pos == args.length - 1) {
                System.out.println("Invalid arguments! Use -h for help.");
                return;
            }

            switch (args[pos]) {
                case "-i":
                    inputFile = args[pos + 1];
                    pos += 2;
                    break;
                case "-o":
                    outputFile = args[pos + 1];
                    pos += 2;
                    break;
                case "-e":
                    ejudgeOutputFile = args[pos + 1];
                    pos += 2;
                    break;
                case "-l":
                    generator.setLoginTemplate(args[pos + 1]);
                    pos += 2;
                    break;
                case "-p":
                    switch (args[pos + 1]) {
                        case "random":
                            generator.setPasswordGenerator(new RandomPasswordGenerator());
                            break;
                        case "markov":
                            needMarkovGenerator = true;
                            break;
                        default:
                            System.out.println("Invalid arguments! Use -h for help.");
                            return;
                    }
                    pos += 2;
                    break;
                case "-pl":
                    try {
                        generator.setPasswordLength(Integer.valueOf(args[pos + 1]));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid arguments! Use -h for help.");
                        return;
                    }
                    pos += 2;
                    break;
                case "-s":
                    markovFile = args[pos + 1];
                    pos += 2;
                    break;
                case "-n":
                    if (pos + 2 >= args.length) {
                        System.out.println("Invalid arguments! Use -h for help.");
                        return;
                    }

                    List<Integer> needed = new ArrayList<>();
                    for (String val : args[pos + 2].split(" ")) {
                        try {
                            needed.add(Integer.valueOf(val));
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid arguments! Use -h for help.");
                            return;
                        }
                    }

                    generator.setNameTemplate(needed, args[pos + 1]);
                    pos += 3;
                    break;
                case "-sn":
                    sheetName = args[pos + 1];
                    pos += 2;
                    break;
                default:
                    System.out.println("Invalid arguments! Use -h for help.");
                    return;
            }
        }

        if (needMarkovGenerator) {
            if (markovFile == null) {
                System.out.println("Invalid arguments! Use -h for help.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(markovFile))) {
                Stream<String> lines = reader.lines();
                String res = lines.collect(Collectors.joining());
                MarkovChain chain = new MarkovChain(res.chars());
                generator.setPasswordGenerator(new MarkovPasswordGenerator(chain));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            generator.generate(inputFile, outputFile, ejudgeOutputFile, hasCaption, sheetName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
