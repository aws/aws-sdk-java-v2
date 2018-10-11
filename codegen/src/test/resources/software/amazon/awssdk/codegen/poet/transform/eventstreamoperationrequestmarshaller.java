package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventStreamOperationRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventStreamOperationRequestMarshaller implements
                                                   Marshaller<Request<EventStreamOperationRequest>, EventStreamOperationRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
                                                                            .requestUri("/2016-03-11/eventStreamOperation").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(true)
                                                                            .hasPayloadMembers(true).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public EventStreamOperationRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<EventStreamOperationRequest> marshall(EventStreamOperationRequest eventStreamOperationRequest) {
        Validate.paramNotNull(eventStreamOperationRequest, "eventStreamOperationRequest");
        try {
            ProtocolMarshaller<Request<EventStreamOperationRequest>> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, eventStreamOperationRequest);
            return protocolMarshaller.marshall(eventStreamOperationRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}

