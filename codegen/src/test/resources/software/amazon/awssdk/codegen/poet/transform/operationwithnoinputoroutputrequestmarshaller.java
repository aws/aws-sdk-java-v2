package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.OperationWithNoInputOrOutputRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link OperationWithNoInputOrOutputRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class OperationWithNoInputOrOutputRequestMarshaller implements
                                                           Marshaller<Request<OperationWithNoInputOrOutputRequest>, OperationWithNoInputOrOutputRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().requestUri("/")
                                                                            .httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false).hasPayloadMembers(false).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public OperationWithNoInputOrOutputRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<OperationWithNoInputOrOutputRequest> marshall(
        OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest) {
        Validate.paramNotNull(operationWithNoInputOrOutputRequest, "operationWithNoInputOrOutputRequest");
        try {
            ProtocolRequestMarshaller<OperationWithNoInputOrOutputRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING, operationWithNoInputOrOutputRequest);
            protocolMarshaller.startMarshalling();
            OperationWithNoInputOrOutputRequestModelMarshaller.getInstance().marshall(operationWithNoInputOrOutputRequest,
                                                                                      protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
