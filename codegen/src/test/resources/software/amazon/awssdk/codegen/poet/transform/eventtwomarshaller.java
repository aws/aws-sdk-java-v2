package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventTwo;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventTwo} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventTwoMarshaller {
    private static final MarshallingInfo<String> BAR_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
                                                                              .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("Bar").isBinary(false).build();

    private static final EventTwoMarshaller INSTANCE = new EventTwoMarshaller();

    private EventTwoMarshaller() {
    }

    public static EventTwoMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(EventTwo eventTwo, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(eventTwo, "eventTwo");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(eventTwo.bar(), BAR_BINDING);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
