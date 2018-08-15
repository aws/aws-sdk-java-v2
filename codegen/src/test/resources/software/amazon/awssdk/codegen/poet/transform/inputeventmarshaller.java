package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.InputEvent;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link InputEvent} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class InputEventMarshaller implements Marshaller<Request<InputEvent>, InputEvent> {
    private static final MarshallingInfo<SdkBytes> EXPLICITPAYLOADMEMBER_BINDING = MarshallingInfo
        .builder(MarshallingType.SDK_BYTES).marshallLocation(MarshallLocation.PAYLOAD).isExplicitPayloadMember(true)
        .isBinary(true).build();

    private static final InputEventMarshaller INSTANCE = null;

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().hasExplicitPayloadMember(true)
                                                                            .hasPayloadMembers(true).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public InputEventMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public static InputEventMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(InputEvent inputEvent, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(inputEvent, "inputEvent");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(inputEvent.explicitPayloadMember(), EXPLICITPAYLOADMEMBER_BINDING);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }

    @Override
    public Request<InputEvent> marshall(InputEvent inputEvent) {
        Validate.paramNotNull(inputEvent, "inputEvent");
        try {
            ProtocolRequestMarshaller<?> protocolMarshaller = protocolFactory.createProtocolMarshaller(SDK_OPERATION_BINDING,
                                                                                                       null);
            protocolMarshaller.startMarshalling();
            marshall(inputEvent, protocolMarshaller);
            Request request = protocolMarshaller.finishMarshalling();
            request.addHeader(":message-type", "event");
            request.addHeader(":event-type", "InputEvent");
            request.addHeader(":content-type", "application/octet-stream");
            return request;
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
