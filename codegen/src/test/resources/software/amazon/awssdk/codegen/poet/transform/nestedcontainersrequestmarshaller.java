package software.amazon.awssdk.services.jsonprotocoltests.transform;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.services.jsonprotocoltests.model.NestedContainersRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link NestedContainersRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class NestedContainersRequestMarshaller implements Marshaller<Request<NestedContainersRequest>, NestedContainersRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder().requestUri("/")
                                                                            .httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false).hasPayloadMembers(true).build();

    private final AwsJsonProtocolFactory protocolFactory;

    public NestedContainersRequestMarshaller(AwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public Request<NestedContainersRequest> marshall(NestedContainersRequest nestedContainersRequest) {
        Validate.paramNotNull(nestedContainersRequest, "nestedContainersRequest");
        try {
            ProtocolRequestMarshaller<NestedContainersRequest> protocolMarshaller = protocolFactory.createProtocolMarshaller(
                SDK_OPERATION_BINDING, nestedContainersRequest);
            protocolMarshaller.startMarshalling();
            NestedContainersRequestModelMarshaller.getInstance().marshall(nestedContainersRequest, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
