package software.amazon.awssdk.services.query.endpoints;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.AttributeMap;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryClientContextParams<T> extends AttributeMap.Key<T> {
    /**
     * A boolean client context parameter
     */
    public static final QueryClientContextParams<Boolean> BOOLEAN_CONTEXT_PARAM = new QueryClientContextParams<>(Boolean.class);

    /**
     * a string client context parameter
     */
    public static final QueryClientContextParams<String> STRING_CONTEXT_PARAM = new QueryClientContextParams<>(String.class);

    private QueryClientContextParams(Class<T> valueClass) {
        super(valueClass);
    }
}
