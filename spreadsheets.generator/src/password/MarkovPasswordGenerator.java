package password;

/**
 * Class that implements {@link PasswordGenerator} interface and generating passwords using Markov chain.
 */
public class MarkovPasswordGenerator implements PasswordGenerator {
    private final MarkovChain chain;

    /**
     * Constructor that builds generator using given {@link MarkovChain}.
     *
     * @param chain given Markov chain.
     */
    public MarkovPasswordGenerator(MarkovChain chain) {
        this.chain = chain;
    }

    /**
     * Generates password using Markov chain. Firstly it randomly chooses start state in chain and then generates
     * password of given length using {@link MarkovChain#next()} method.
     *
     * @param passwordLength password length.
     *
     * @return password as a {@link String}.
     */
    @Override
    public String nextPassword(final int passwordLength) {
        chain.resetState();

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < passwordLength; i++) {
            result.append(chain.next());
        }

        return result.toString();
    }
}
