import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class ErrorRule extends Rule {
    private final Expr error;

    public ErrorRule(Builder builder, Expr error) {
        super(builder);
        this.error = error;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> v) {
        return v.visitErrorRule(error);
    }

    @Override
    public String toString() {
        return "ErrorRule{" +
               "error=" + error +
               ", conditions=" + conditions +
               ", documentation='" + documentation + '\'' +
               '}';
    }
}
