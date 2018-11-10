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
import software.amazon.awssdk.services.jsonprotocoltests.model.OperationWithNoInputOrOutputRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link OperationWithNoInputOrOutputRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class OperationWithNoInputOrOutputRequestMarshaller implements
                                                           Marshaller<OperationWithNoInputOrOutputRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().requestUri("/")
                                                                            .httpMethod(SdkHttpMethod.POST).hasExplicitPayloadMember(false).hasPayloadMembers(false).build();

    private final BaseAwsJsonProtocolFactory protocolFactory;

    public OperationWithNoInputOrOutputRequestMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public SdkHttpFullRequest marshall(OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest) {
        Validate.paramNotNull(operationWithNoInputOrOutputRequest, "operationWithNoInputOrOutputRequest");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING);
            return protocolMarshaller.marshall(operationWithNoInputOrOutputRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}

