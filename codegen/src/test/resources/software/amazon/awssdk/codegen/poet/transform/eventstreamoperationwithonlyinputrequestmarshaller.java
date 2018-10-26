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

    private final BaseAwsJsonProtocolFactory protocolFactory;

    public EventStreamOperationWithOnlyInputRequestMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<EventStreamOperationWithOnlyInputRequest> marshall(
        EventStreamOperationWithOnlyInputRequest eventStreamOperationWithOnlyInputRequest) {
        Validate.paramNotNull(eventStreamOperationWithOnlyInputRequest, "eventStreamOperationWithOnlyInputRequest");
        try {
            ProtocolMarshaller<Request<EventStreamOperationWithOnlyInputRequest>> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, eventStreamOperationWithOnlyInputRequest);
            return protocolMarshaller.marshall(eventStreamOperationWithOnlyInputRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}

