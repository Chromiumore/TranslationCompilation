package me.chromiumore.mtnc.preprocessor;

public class ValidationResult {
    public static final String OK_MESSAGE = "Ошибок не выявлено";
    private String message;
    private boolean isSuccessful;

    public static ValidationResult ok() {
        return new ValidationResult(OK_MESSAGE, true);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(message, false);
    }

    private ValidationResult(String message, boolean isSuccessful) {
        this.message = message;
        this.isSuccessful = isSuccessful;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}
