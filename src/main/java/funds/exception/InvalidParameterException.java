package funds.exception;

public class InvalidParameterException extends Exception {

    public InvalidParameterException(String message) {
        super();
        throw new RuntimeException("Invalid Parameter Found: " + message);
    }
}
