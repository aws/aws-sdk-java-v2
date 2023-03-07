package software.amazon.awssdk.services.query.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class QueryOperations {
    public final String A_POST_OPERATION = "APostOperation";

    public final String A_POST_OPERATION_WITH_OUTPUT = "APostOperationWithOutput";

    public final String BEARER_AUTH_OPERATION = "BearerAuthOperation";

    public final String GET_OPERATION_WITH_CHECKSUM = "GetOperationWithChecksum";

    public final String OPERATION_WITH_CHECKSUM_REQUIRED = "OperationWithChecksumRequired";

    public final String OPERATION_WITH_CONTEXT_PARAM = "OperationWithContextParam";

    public final String OPERATION_WITH_NONE_AUTH_TYPE = "OperationWithNoneAuthType";

    public final String OPERATION_WITH_STATIC_CONTEXT_PARAMS = "OperationWithStaticContextParams";

    public final String PUT_OPERATION_WITH_CHECKSUM = "PutOperationWithChecksum";

    public final String STREAMING_INPUT_OPERATION = "StreamingInputOperation";

    public final String STREAMING_OUTPUT_OPERATION = "StreamingOutputOperation";

    public String requestObjectToName(QueryRequest req) {
        if (req instanceof APostOperationRequest) {
            return "APostOperation";
        }

        ...

        throw SdkClientException.create("Unknown request object!");
    }
}
