package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventThree;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventThree} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventThreeMarshaller implements Marshaller<EventThree> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().hasExplicitPayloadMember(false)
                                                                            .hasPayloadMembers(true).httpMethod(SdkHttpMethod.GET).hasEvent(true).build();
    private final BaseAwsJsonProtocolFactory protocolFactory;

    public EventThreeMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public SdkHttpFullRequest marshall(EventThree eventThree) {
        Validate.paramNotNull(eventThree, "eventThree");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                    .createProtocolMarshaller(SDK_OPERATION_BINDING);
            return protocolMarshaller.marshall(eventThree).toBuilder().putHeader(":message-type", "event")
                    .putHeader(":event-type", "EventThree").putHeader(":content-type", "application/json").build();
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}

