package funds.exception;

/**
 * Thrown when an incoming message violates the schema contract.
 * Should NOT be retried.
 */
public class NonRetryableContractException extends RuntimeException {
    public NonRetryableContractException(String message) {
        super(message);
    }

    public NonRetryableContractException(String message, Throwable cause) {
        super(message, cause);
    }
}