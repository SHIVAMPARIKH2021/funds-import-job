package funds.exception;

public class InvalidParameterException extends Exception {

    public InvalidParameterException(String message) {
        super("Invalid Parameter Found: " + message);
    }
}
