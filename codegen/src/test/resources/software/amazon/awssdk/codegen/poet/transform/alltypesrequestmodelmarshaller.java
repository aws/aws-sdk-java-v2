package software.amazon.awssdk.services.jsonprotocoltests.transform;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.model.AllTypesRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link AllTypesRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class AllTypesRequestModelMarshaller {
    private static final MarshallingInfo<String> STRINGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
                                                                                       .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StringMember").isBinary(false)
                                                                                       .defaultValueSupplier(software.amazon.awssdk.codegen.poet.transform.CustomDefaultValueSupplier.getInstance()).build();

    private static final MarshallingInfo<Integer> INTEGERMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.INTEGER)
                                                                                         .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("IntegerMember").isBinary(false).build();

    private static final MarshallingInfo<Boolean> BOOLEANMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.BOOLEAN)
                                                                                         .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("BooleanMember").isBinary(false).build();

    private static final MarshallingInfo<Float> FLOATMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.FLOAT)
                                                                                     .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("FloatMember").isBinary(false).build();

    private static final MarshallingInfo<Double> DOUBLEMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.DOUBLE)
                                                                                       .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("DoubleMember").isBinary(false).build();

    private static final MarshallingInfo<Long> LONGMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.LONG)
                                                                                   .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("LongMember").isBinary(false).build();

    private static final MarshallingInfo<List> SIMPLELIST_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                   .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("SimpleList").isBinary(false).build();

    private static final MarshallingInfo<List> LISTOFENUMS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                    .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfEnums").isBinary(false).build();

    private static final MarshallingInfo<List> LISTOFMAPS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                   .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfMaps").isBinary(false).build();

    private static final MarshallingInfo<List> LISTOFSTRUCTS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                      .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfStructs").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFSTRINGTOINTEGERLIST_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                                .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToIntegerList").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFSTRINGTOSTRING_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                           .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToString").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFSTRINGTOSIMPLESTRUCT_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                                 .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToSimpleStruct").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFENUMTOENUM_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                       .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfEnumToEnum").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFENUMTOSTRING_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                         .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfEnumToString").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFSTRINGTOENUM_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                         .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfStringToEnum").isBinary(false).build();

    private static final MarshallingInfo<Map> MAPOFENUMTOSIMPLESTRUCT_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                                               .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("MapOfEnumToSimpleStruct").isBinary(false).build();

    private static final MarshallingInfo<Instant> TIMESTAMPMEMBER_BINDING = MarshallingInfo.builder(MarshallingType.INSTANT)
                                                                                           .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("TimestampMember").isBinary(false).build();

    private static final MarshallingInfo<StructuredPojo> STRUCTWITHNESTEDTIMESTAMPMEMBER_BINDING = MarshallingInfo
            .builder(MarshallingType.STRUCTURED).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("StructWithNestedTimestampMember").isBinary(false).build();

    private static final MarshallingInfo<ByteBuffer> BLOBARG_BINDING = MarshallingInfo.builder(MarshallingType.BYTE_BUFFER)
                                                                                      .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("BlobArg").isBinary(false).build();

    private static final MarshallingInfo<StructuredPojo> STRUCTWITHNESTEDBLOB_BINDING = MarshallingInfo
            .builder(MarshallingType.STRUCTURED).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("StructWithNestedBlob").isBinary(false).build();

    private static final MarshallingInfo<Map> BLOBMAP_BINDING = MarshallingInfo.builder(MarshallingType.MAP)
                                                                               .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("BlobMap").isBinary(false).build();

    private static final MarshallingInfo<List> LISTOFBLOBS_BINDING = MarshallingInfo.builder(MarshallingType.LIST)
                                                                                    .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ListOfBlobs").isBinary(false).build();

    private static final MarshallingInfo<StructuredPojo> RECURSIVESTRUCT_BINDING = MarshallingInfo
            .builder(MarshallingType.STRUCTURED).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("RecursiveStruct").isBinary(false).build();

    private static final MarshallingInfo<StructuredPojo> POLYMORPHICTYPEWITHSUBTYPES_BINDING = MarshallingInfo
            .builder(MarshallingType.STRUCTURED).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("PolymorphicTypeWithSubTypes").isBinary(false).build();

    private static final MarshallingInfo<StructuredPojo> POLYMORPHICTYPEWITHOUTSUBTYPES_BINDING = MarshallingInfo
            .builder(MarshallingType.STRUCTURED).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("PolymorphicTypeWithoutSubTypes").isBinary(false).build();

    private static final MarshallingInfo<String> ENUMTYPE_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
                                                                                   .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("EnumType").isBinary(false).build();

    private static final AllTypesRequestModelMarshaller INSTANCE = new AllTypesRequestModelMarshaller();

    private AllTypesRequestModelMarshaller() {
    }

    public static AllTypesRequestModelMarshaller getInstance() {
        return INSTANCE;
    }

    /**
     * Marshall the given parameter object
     */
    public void marshall(AllTypesRequest allTypesRequest, ProtocolMarshaller protocolMarshaller) {
        Validate.paramNotNull(allTypesRequest, "allTypesRequest");
        Validate.paramNotNull(protocolMarshaller, "protocolMarshaller");
        try {
            protocolMarshaller.marshall(allTypesRequest.stringMember(), STRINGMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.integerMember(), INTEGERMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.booleanMember(), BOOLEANMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.floatMember(), FLOATMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.doubleMember(), DOUBLEMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.longMember(), LONGMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.simpleList(), SIMPLELIST_BINDING);
            protocolMarshaller.marshall(allTypesRequest.listOfEnumsAsStrings(), LISTOFENUMS_BINDING);
            protocolMarshaller.marshall(allTypesRequest.listOfMaps(), LISTOFMAPS_BINDING);
            protocolMarshaller.marshall(allTypesRequest.listOfStructs(), LISTOFSTRUCTS_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfStringToIntegerList(), MAPOFSTRINGTOINTEGERLIST_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfStringToString(), MAPOFSTRINGTOSTRING_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfStringToSimpleStruct(), MAPOFSTRINGTOSIMPLESTRUCT_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfEnumToEnumAsStrings(), MAPOFENUMTOENUM_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfEnumToStringAsStrings(), MAPOFENUMTOSTRING_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfStringToEnumAsStrings(), MAPOFSTRINGTOENUM_BINDING);
            protocolMarshaller.marshall(allTypesRequest.mapOfEnumToSimpleStructAsStrings(), MAPOFENUMTOSIMPLESTRUCT_BINDING);
            protocolMarshaller.marshall(allTypesRequest.timestampMember(), TIMESTAMPMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.structWithNestedTimestampMember(),
                                        STRUCTWITHNESTEDTIMESTAMPMEMBER_BINDING);
            protocolMarshaller.marshall(allTypesRequest.blobArg(), BLOBARG_BINDING);
            protocolMarshaller.marshall(allTypesRequest.structWithNestedBlob(), STRUCTWITHNESTEDBLOB_BINDING);
            protocolMarshaller.marshall(allTypesRequest.blobMap(), BLOBMAP_BINDING);
            protocolMarshaller.marshall(allTypesRequest.listOfBlobs(), LISTOFBLOBS_BINDING);
            protocolMarshaller.marshall(allTypesRequest.recursiveStruct(), RECURSIVESTRUCT_BINDING);
            protocolMarshaller.marshall(allTypesRequest.polymorphicTypeWithSubTypes(), POLYMORPHICTYPEWITHSUBTYPES_BINDING);
            protocolMarshaller.marshall(allTypesRequest.polymorphicTypeWithoutSubTypes(), POLYMORPHICTYPEWITHOUTSUBTYPES_BINDING);
            protocolMarshaller.marshall(allTypesRequest.enumTypeAsString(), ENUMTYPE_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
