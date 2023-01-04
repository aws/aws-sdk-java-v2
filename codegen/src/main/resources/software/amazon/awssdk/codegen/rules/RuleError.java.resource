import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;

@SdkInternalApi
public class RuleError extends SdkException {

    protected RuleError(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkException.Builder {
        @Override
        RuleError build();
    }

    public static <T> T ctx(String message, Supplier<T> f) {
        try {
            return f.get();
        } catch (Exception e) {
            throw builder().message(message).cause(e).build();
        }
    }

    public static <T> T ctx(String message, Runnable f) {
        return ctx(message, () -> {
            f.run();
            return null;
        });
    }

    private static class BuilderImpl extends SdkException.BuilderImpl implements Builder {
        @Override
        public RuleError build() {
            return new RuleError(this);
        }
    }
}
