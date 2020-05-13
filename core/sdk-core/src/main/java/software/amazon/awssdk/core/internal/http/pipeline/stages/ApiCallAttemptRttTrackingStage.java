package software.amazon.awssdk.core.internal.http.pipeline.stages;

import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;

public class ApiCallAttemptRttTrackingStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final HttpClientDependencies dependencies;
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public ApiCallAttemptRttTrackingStage(HttpClientDependencies dependencies,
                                              RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.dependencies = dependencies;
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        try (MetricCollector apiCallAttemptMetricCollector = context
                .executionContext()
                .metricCollector()
                .createChild("ApiCallAttempt")) {
            context.apiCallAttemptMetricCollector(apiCallAttemptMetricCollector);
            return wrapped.execute(input, context);
        }
    }
}
