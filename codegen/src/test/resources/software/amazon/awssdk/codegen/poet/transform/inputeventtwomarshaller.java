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
import software.amazon.awssdk.services.jsonprotocoltests.model.InputEventTwo;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link InputEventTwo} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class InputEventTwoMarshaller implements Marshaller<Request<InputEventTwo>, InputEventTwo> {
    private static final MarshallingInfo<SdkBytes> IMPLICITPAYLOADMEMBERONE_BINDING = MarshallingInfo
        .builder(MarshallingType.SDK_BYTES).marshallLocation(MarshallLocation.PAYLOAD)
        .marshallLocationName("ImplicitPayloadMemberOne").isBinary(false).build();

    private static final MarshallingInfo<String> IMPLICITPAYLOADMEMBERTWO_BINDING = MarshallingInfo
        .builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
        .marshallLocationName("ImplicitPayloadMemberTwo").isBinary(false).build();

    private static final MarshallingInfo<String> EVENTHEADERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
                                                                                            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("EventHeaderMember").isBinary(false).build();

    private static final InputEventTwoMarshaller INSTANCE = null;

    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().hasExplicitPayloadMember(false)
                                                                            .hasPayloadMembers(true).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public InputEventTwoMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public static InputEventTwoMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(InputEventTwo inputEventTwo, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(inputEventTwo, "inputEventTwo");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(inputEventTwo.implicitPayloadMemberOne(), IMPLICITPAYLOADMEMBERONE_BINDING);
            protocolMarshaller.marshall(inputEventTwo.implicitPayloadMemberTwo(), IMPLICITPAYLOADMEMBERTWO_BINDING);
            protocolMarshaller.marshall(inputEventTwo.eventHeaderMember(), EVENTHEADERMEMBER_BINDING);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }

    @Override
    public Request<InputEventTwo> marshall(InputEventTwo inputEventTwo) {
        Validate.paramNotNull(inputEventTwo, "inputEventTwo");
        try {
            ProtocolRequestMarshaller<?> protocolMarshaller = protocolFactory.createProtocolMarshaller(SDK_OPERATION_BINDING,
                                                                                                       null);
            protocolMarshaller.startMarshalling();
            marshall(inputEventTwo, protocolMarshaller);
            Request request = protocolMarshaller.finishMarshalling();
            request.addHeader(":message-type", "event");
            request.addHeader(":event-type", "InputEventTwo");
            request.addHeader(":content-type", "application/json");
            return request;
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
