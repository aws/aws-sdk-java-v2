package software.amazon.awssdk.services.batchmanager;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.batchmanager.model.BatchManagerException;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;

/**
 * Internal implementation of {@link BatchManagerAsyncClient}.
 *
 * @see BatchManagerAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultBatchManagerAsyncClient implements BatchManagerAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultBatchManagerAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    protected DefaultBatchManagerAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(BatchManagerException::builder)
                      .protocol(AwsJsonProtocol.REST_JSON).protocolVersion("1.1");
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
                                                                 RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
                                                                                JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }

    @Override
    public SqsAsyncBatchManager batchManager() {
        return SqsAsyncBatchManager.builder().client(this).scheduledExecutor(executorService).build();
    }
}
