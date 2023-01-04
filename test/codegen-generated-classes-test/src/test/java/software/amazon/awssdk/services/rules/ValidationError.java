package software.amazon.awssdk.services.rules;

import java.util.Objects;

public final class ValidationError {
    private final ValidationErrorType validationErrorType;
    private final String error;

    public ValidationError(ValidationErrorType validationErrorType, String error) {
        this.validationErrorType = validationErrorType;
        this.error = error;
    }

    public ValidationErrorType validationErrorType() {
        return validationErrorType;
    }

    public String error() {
        return error;
    }

    @Override
    public String toString() {
        return this.validationErrorType + ", " + this.error;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        ValidationError that = (ValidationError) obj;
        return Objects.equals(this.validationErrorType, that.validationErrorType) &&
                Objects.equals(this.error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationErrorType, error);
    }

}
