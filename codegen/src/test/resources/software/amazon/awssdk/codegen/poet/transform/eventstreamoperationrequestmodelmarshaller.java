package software.amazon.awssdk.services.jsonprotocoltests.transform;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationRequest;

/**
 * {@link EventStreamOperationRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventStreamOperationRequestModelMarshaller {
    private static final EventStreamOperationRequestModelMarshaller INSTANCE = new EventStreamOperationRequestModelMarshaller();

    private EventStreamOperationRequestModelMarshaller() {
    }

    public static EventStreamOperationRequestModelMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(EventStreamOperationRequest eventStreamOperationRequest, ProtocolMarshaller protocolMarshaller) {
    }
}

