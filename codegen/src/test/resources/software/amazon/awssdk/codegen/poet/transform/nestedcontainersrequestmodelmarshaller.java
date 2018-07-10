package software.amazon.awssdk.services.jsonprotocoltests.transform;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.NestedContainersRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link NestedContainersRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class NestedContainersRequestModelMarshaller {
    private static final MarshallingInfo<List> LISTOFLISTOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListOfStrings").isBinary(false).build();

    private static final MarshallingInfo<List> LISTOFLISTOFLISTOFSTRINGS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                                  .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfListOfListOfStrings").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFSTRINGTOLISTOFLISTOFSTRINGS_BINDING = MarshallingInfo
        .builder(MarshallingType.MAP).marshallLocation(MarshallLocation.PAYLOAD)
        .marshallLocationName("MapOfStringToListOfListOfStrings").isBinary(false).build();

    private static final NestedContainersRequestModelMarshaller INSTANCE = new NestedContainersRequestModelMarshaller();

    private NestedContainersRequestModelMarshaller() {
    }

    public static NestedContainersRequestModelMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(NestedContainersRequest nestedContainersRequest, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(nestedContainersRequest, "nestedContainersRequest");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(nestedContainersRequest.listOfListOfStrings(), LISTOFLISTOFSTRINGS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.listOfListOfListOfStrings(), LISTOFLISTOFLISTOFSTRINGS_BINDING);
            protocolMarshaller.marshall(nestedContainersRequest.mapOfStringToListOfListOfStrings(),
                                        MAPOFSTRINGTOLISTOFLISTOFSTRINGS_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
