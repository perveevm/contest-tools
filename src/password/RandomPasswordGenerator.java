package password;

import java.util.Random;

/**
 * Class that implements {@link PasswordGenerator} interface and generates random password using lowercase letters.
 */
public class RandomPasswordGenerator implements PasswordGenerator {
    private final Random random = new Random();

    /**
     * Generates random password consists of lowercase letters.
     *
     * @param passwordLength password length.
     *
     * @return generated password as a {@link String}.
     */
    @Override
    public String nextPassword(final int passwordLength) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < passwordLength; i++) {
            result.append((char)(random.nextInt(26) + 'a'));
        }
        return result.toString();
    }
}
