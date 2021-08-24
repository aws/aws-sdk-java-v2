package software.amazon.awssdk.services.batchmanagertest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.BatchManagerTestException;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link BatchManagerTestClient}.
 *
 * @see BatchManagerTestClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultBatchManagerTestClient implements BatchManagerTestClient {
    private static final Logger log = Logger.loggerFor(DefaultBatchManagerTestClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    protected DefaultBatchManagerTestClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                      .defaultServiceExceptionSupplier(BatchManagerTestException::builder).protocol(AwsJsonProtocol.REST_JSON)
                      .protocolVersion("1.1");
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    @Override
    public BatchManagerTestBatchManager batchManager() {
        return BatchManagerTestBatchManager.builder().client(this).scheduledExecutor(executorService).build();
    }
}
