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
                                                                            .requestUri("/2016-03-11/eventStreamOperation").httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false)
                                                                            .hasPayloadMembers(false).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public EventStreamOperationRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<EventStreamOperationRequest> marshall(EventStreamOperationRequest eventStreamOperationRequest) {
        Validate.paramNotNull(eventStreamOperationRequest, "eventStreamOperationRequest");
        try {
            ProtocolRequestMarshaller<EventStreamOperationRequest> protocolMarshaller = protocolFactory.createProtocolMarshaller(
                SDK_OPERATION_BINDING, eventStreamOperationRequest);
            protocolMarshaller.startMarshalling();
            EventStreamOperationRequestModelMarshaller.getInstance().marshall(eventStreamOperationRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}

