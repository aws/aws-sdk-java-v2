package software.amazon.awssdk;

/**
 * Base class for all AWS Service requests.
 */
public abstract class AwsRequest<B extends AwsRequest.Builder<B, R, C>,
        R extends AwsRequest<B, R, C>,
        C extends AwsRequestOverrideConfig> extends SdkRequest<B, R, C> {

    protected AwsRequest(B builder) {
        super(builder);
    }

    public interface Builder<B extends AwsRequest.Builder<B, R, C>,
            R extends AwsRequest<B, R, C>,
            C extends AwsRequestOverrideConfig> extends SdkRequest.Builder<B, R, C> {
    }

    protected static abstract class BuilderImpl<B extends AwsRequest.Builder<B, R, C>,
            R extends AwsRequest<B, R, C>,
            C extends AwsRequestOverrideConfig> extends SdkRequest.BuilderImpl<B, R, C> implements Builder<B, R, C>  {

        protected BuilderImpl(Class<B> concrete) {
            super(concrete);
        }

        protected BuilderImpl(Class<B> concrete, R request) {
            super(concrete, request);
        }
    }
}
