package software.amazon.awssdk.services.jsonprotocoltests.transform;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
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

    private final AwsJsonProtocolFactory protocolFactory;

    public StreamingOutputOperationRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<StreamingOutputOperationRequest> marshall(StreamingOutputOperationRequest streamingOutputOperationRequest) {
        Validate.paramNotNull(streamingOutputOperationRequest, "streamingOutputOperationRequest");
        try {
            ProtocolRequestMarshaller<StreamingOutputOperationRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, streamingOutputOperationRequest);
            protocolMarshaller.startMarshalling();
            StreamingOutputOperationRequestModelMarshaller.getInstance().marshall(streamingOutputOperationRequest,
                                                                                  protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
