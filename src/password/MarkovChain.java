package password;

import java.util.Iterator;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Class that represents Markov chain (see more <a href="https://en.wikipedia.org/wiki/Markov_chain">here</a>).
 */
public class MarkovChain {
    private final int[][] transitions = new int[26][26];
    private final int[] totalTransitions = new int[26];

    private final Random random = new Random();

    private int currentState;

    /**
     * Constructor that builds Markov chain using given {@link IntStream} as data source.
     *
     * Chain has 26 states, each state represents some character. There are transitions between some pairs of states.
     * Probabilities are calculated using text given as {@link IntStream}.
     *
     * @param stream given {@link IntStream} of characters' ASCII codes.
     */
    public MarkovChain(IntStream stream) {
        IntStream filteredText = stream.filter(Character::isAlphabetic).map(Character::toLowerCase);
        Iterator<Integer> iterator = filteredText.iterator();

        if (!iterator.hasNext()) {
            return;
        }

        char from = (char)iterator.next().intValue();
        while (iterator.hasNext()) {
            char to = (char)iterator.next().intValue();

            transitions[from - 'a'][to - 'a']++;
            totalTransitions[from - 'a']++;

            from = to;
        }

        resetState();
    }

    /**
     * Chooses random state as a start state.
     */
    public void resetState() {
        currentState = random.nextInt(26);
    }

    /**
     * Chooses next state depending on current state and transition probabilities.
     *
     * @return character that is represented by chosen next state.
     */
    public char next() {
        char result = (char)(currentState + 'a');

        int sum = random.nextInt(totalTransitions[currentState] + 1);
        for (int i = 0; i < 26; i++) {
            if (transitions[currentState][i] >= sum) {
                currentState = i;
                break;
            }
            sum -= transitions[currentState][i];
        }

        return result;
    }
}
