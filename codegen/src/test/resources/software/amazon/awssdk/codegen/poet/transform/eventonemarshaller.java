package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventOne;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventOne} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventOneMarshaller {
    private static final MarshallingInfo<String> FOO_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
                                                                              .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("Foo").isBinary(false).build();

    private static final EventOneMarshaller INSTANCE = new EventOneMarshaller();

    private EventOneMarshaller() {
    }

    public static EventOneMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(EventOne eventOne, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(eventOne, "eventOne");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(eventOne.foo(), FOO_BINDING);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
