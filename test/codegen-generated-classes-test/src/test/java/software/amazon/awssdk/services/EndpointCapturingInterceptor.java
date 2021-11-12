package software.amazon.awssdk.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

public class EndpointCapturingInterceptor implements ExecutionInterceptor {
    private final List<String> endpoints = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        endpoints.add(context.httpRequest().getUri().toString());
        throw new CaptureCompletedException();
    }

    public List<String> endpoints() {
        return endpoints;
    }

    private void reset() {
        endpoints.clear();
    }

    public class CaptureCompletedException extends RuntimeException {
    }
}