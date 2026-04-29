package software.amazon.awssdk.services.s3control.internal.functionaltests.arns;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Interceptor that captures the resolved endpoint and reroutes requests to WireMock.
 */
public class S3ControlWireMockRerouteInterceptor implements ExecutionInterceptor {

    private final URI rerouteEndpoint;
    private final List<SdkHttpRequest> recordedRequests = new ArrayList<>();
    private final List<URI> recordedEndpoints = new ArrayList<URI>();

    S3ControlWireMockRerouteInterceptor(URI rerouteEndpoint) {
        this.rerouteEndpoint = rerouteEndpoint;
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {

        SdkHttpRequest request = context.httpRequest();
        recordedRequests.add(request);

        return request.toBuilder().uri(rerouteEndpoint).build();
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        Endpoint resolvedEndpoint = executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        if (resolvedEndpoint != null) {
            recordedEndpoints.add(resolvedEndpoint.url());
        } else {
            recordedEndpoints.add(context.httpRequest().getUri());
        }
    }

    public List<SdkHttpRequest> getRecordedRequests() {
        return recordedRequests;
    }

    public List<URI> getRecordedEndpoints() {
        return recordedEndpoints;
    }
}
