import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class InnerParseError extends RuntimeException {
    private static final long serialVersionUID = -7808901449079805477L;

    public InnerParseError(String message) {
        super(message);
    }
}
