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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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
import software.amazon.awssdk.services.s3.presignedurl.PresignedUrlManager;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

/**
 * Default implementation of {@link PresignedUrlManager} for executing S3 operations using presigned URLs.
 */
@SdkInternalApi
public final class DefaultPresignedUrlManager implements PresignedUrlManager {
    
    private final SyncClientHandler clientHandler;
    private final AwsS3ProtocolFactory protocolFactory;
    private final SdkClientConfiguration clientConfiguration;
    private final AwsProtocolMetadata protocolMetadata;
    
    public DefaultPresignedUrlManager(SyncClientHandler clientHandler, 
                                      AwsS3ProtocolFactory protocolFactory,
                                      SdkClientConfiguration clientConfiguration,
                                      AwsProtocolMetadata protocolMetadata) {
        this.clientHandler = clientHandler;
        this.protocolFactory = protocolFactory;
        this.clientConfiguration = clientConfiguration;
        this.protocolMetadata = protocolMetadata;
    }
    
    /**
     * Downloads an S3 object using a presigned URL.
     */
    @Override
    public <ReturnT> ReturnT getObject(PresignedUrlGetObjectRequest presignedUrlGetObjectRequest,
                                       ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) 
                                       throws NoSuchKeyException, InvalidObjectStateException, 
                                              AwsServiceException, SdkClientException, S3Exception {

        HttpResponseHandler<GetObjectResponse> responseHandler = protocolFactory.createResponseHandler(
                GetObjectResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        
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

            return clientHandler.execute(
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
                            .withMarshaller(new PresignedUrlGetObjectRequestMarshaller(protocolFactory)), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }
    
    private SdkClientConfiguration updateSdkClientConfiguration(SdkClientConfiguration clientConfiguration) {
        SdkClientConfiguration.Builder configBuilder = clientConfiguration.toBuilder();
        configBuilder.option(SdkAdvancedClientOption.SIGNER, new NoOpSigner());
        configBuilder.option(SIGNER_OVERRIDDEN, true);
        return configBuilder.build();
    }

}
