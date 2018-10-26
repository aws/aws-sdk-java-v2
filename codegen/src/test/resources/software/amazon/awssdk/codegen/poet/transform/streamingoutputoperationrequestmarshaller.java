package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.services.jsonprotocoltests.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link StreamingOutputOperationRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class StreamingOutputOperationRequestMarshaller implements
                                                       Marshaller<Request<StreamingOutputOperationRequest>, StreamingOutputOperationRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
                                                                            .requestUri("/2016-03-11/streamingOutputOperation").httpMethodName(HttpMethodName.POST)
                                                                            .hasExplicitPayloadMember(false).hasPayloadMembers(false).build();

    private final BaseAwsJsonProtocolFactory protocolFactory;

    public StreamingOutputOperationRequestMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<StreamingOutputOperationRequest> marshall(StreamingOutputOperationRequest streamingOutputOperationRequest) {
        Validate.paramNotNull(streamingOutputOperationRequest, "streamingOutputOperationRequest");
        try {
            ProtocolMarshaller<Request<StreamingOutputOperationRequest>> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, streamingOutputOperationRequest);
            return protocolMarshaller.marshall(streamingOutputOperationRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}

