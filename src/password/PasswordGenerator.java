package password;

/**
 * Interface for password generator.
 */
public interface PasswordGenerator {
    /**
     * The only method generates password of given length.
     *
     * @param passwordLength password length.
     *
     * @return generated password as a {@link String}.
     */
    String nextPassword(final int passwordLength);
}
