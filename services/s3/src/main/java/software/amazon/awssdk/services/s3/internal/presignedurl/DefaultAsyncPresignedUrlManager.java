/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal.presignedurl;

import static software.amazon.awssdk.core.client.config.SdkClientOption.SIGNER_OVERRIDDEN;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.PresignedUrlGetObjectRequestWrapper;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlManager;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Default implementation of {@link AsyncPresignedUrlManager} for executing S3 operations asynchronously using presigned URLs.
 */
@SdkInternalApi
public final class DefaultAsyncPresignedUrlManager implements AsyncPresignedUrlManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultAsyncPresignedUrlManager.class);

    private final AsyncClientHandler clientHandler;
    private final AwsS3ProtocolFactory protocolFactory;
    private final SdkClientConfiguration clientConfiguration;
    private final AwsProtocolMetadata protocolMetadata;
    
    public DefaultAsyncPresignedUrlManager(AsyncClientHandler clientHandler,
                                           AwsS3ProtocolFactory protocolFactory,
                                           SdkClientConfiguration clientConfiguration,
                                           AwsProtocolMetadata protocolMetadata) {
        this.clientHandler = clientHandler;
        this.protocolFactory = protocolFactory;
        this.clientConfiguration = clientConfiguration;
        this.protocolMetadata = protocolMetadata;
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
            PresignedUrlGetObjectRequest presignedUrlGetObjectRequest,
            AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer)
            throws NoSuchKeyException, InvalidObjectStateException,
                   AwsServiceException, SdkClientException, S3Exception {

        PresignedUrlGetObjectRequestWrapper internalRequest = PresignedUrlGetObjectRequestWrapper.builder()
                .url(presignedUrlGetObjectRequest.presignedUrl())
                .range(presignedUrlGetObjectRequest.range())
                .build();

        SdkClientConfiguration updatedClientConfiguration = updateSdkClientConfiguration(this.clientConfiguration);
        List<MetricPublisher> metricPublishers = Optional.ofNullable(
            updatedClientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS))
            .orElse(Collections.emptyList());
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ?
            NoOpMetricCollector.create() : MetricCollector.create("ApiCall");
        
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "S3");
            //TODO: Discuss if we need to change OPERATION_NAME as part of Surface API Review
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetObject");

            Pair<AsyncResponseTransformer<GetObjectResponse, ReturnT>, CompletableFuture<Void>> pair =
                    AsyncResponseTransformerUtils.wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();

            HttpResponseHandler<GetObjectResponse> responseHandler = protocolFactory.createResponseHandler(
                GetObjectResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<PresignedUrlGetObjectRequestWrapper, GetObjectResponse>()
                            .withOperationName("PresignedUrlGetObject")
                            .withProtocolMetadata(protocolMetadata)
                            .withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(updatedClientConfiguration)
                            .withInput(internalRequest)
                            .withMetricCollector(apiCallMetricCollector)
                            // TODO: Deprecate IS_DISCOVERED_ENDPOINT, use new SKIP_ENDPOINT_RESOLUTION for better semantics
                            .putExecutionAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true)
                            .withMarshaller(new PresignedUrlGetObjectRequestMarshaller(protocolFactory)),
                                                                                        asyncResponseTransformer);
            CompletableFuture<ReturnT> whenCompleteFuture = null;
            AsyncResponseTransformer<GetObjectResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                                   () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            AsyncResponseTransformer<GetObjectResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                           () -> finalAsyncResponseTransformer.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }
    
    private SdkClientConfiguration updateSdkClientConfiguration(SdkClientConfiguration clientConfiguration) {
        SdkClientConfiguration.Builder configBuilder = clientConfiguration.toBuilder();
        configBuilder.option(SdkAdvancedClientOption.SIGNER, new NoOpSigner());
        configBuilder.option(SIGNER_OVERRIDDEN, true);
        return configBuilder.build();
    }

}
