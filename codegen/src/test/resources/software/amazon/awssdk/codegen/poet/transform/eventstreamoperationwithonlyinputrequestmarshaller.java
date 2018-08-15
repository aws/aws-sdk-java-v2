package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationWithOnlyInputRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventStreamOperationWithOnlyInputRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventStreamOperationWithOnlyInputRequestMarshaller implements
                                                                Marshaller<Request<EventStreamOperationWithOnlyInputRequest>, EventStreamOperationWithOnlyInputRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
                                                                            .requestUri("/2016-03-11/EventStreamOperationWithOnlyInput").httpMethodName(HttpMethodName.POST)
                                                                            .hasExplicitPayloadMember(false).hasPayloadMembers(true).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public EventStreamOperationWithOnlyInputRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<EventStreamOperationWithOnlyInputRequest> marshall(
        EventStreamOperationWithOnlyInputRequest eventStreamOperationWithOnlyInputRequest) {
        Validate.paramNotNull(eventStreamOperationWithOnlyInputRequest, "eventStreamOperationWithOnlyInputRequest");
        try {
            ProtocolRequestMarshaller<EventStreamOperationWithOnlyInputRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, eventStreamOperationWithOnlyInputRequest);
            protocolMarshaller.startMarshalling();
            EventStreamOperationWithOnlyInputRequestModelMarshaller.getInstance().marshall(
                eventStreamOperationWithOnlyInputRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
