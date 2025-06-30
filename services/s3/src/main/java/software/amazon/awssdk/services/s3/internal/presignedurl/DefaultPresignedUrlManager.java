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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.InternalPresignedUrlGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.PresignedUrlManager;

@SdkInternalApi
public final class DefaultPresignedUrlManager implements PresignedUrlManager {
    
    private final SyncClientHandler clientHandler;
    private final AwsS3ProtocolFactory protocolFactory;
    private final SdkClientConfiguration clientConfiguration;
    private static final AwsProtocolMetadata PROTOCOL_METADATA = AwsProtocolMetadata.builder()
            .serviceProtocol(AwsServiceProtocol.REST_XML).build();
    
    public DefaultPresignedUrlManager(SyncClientHandler clientHandler, 
                                      AwsS3ProtocolFactory protocolFactory,
                                      SdkClientConfiguration clientConfiguration,
                                      AwsProtocolMetadata protocolMetadata) {
        this.clientHandler = clientHandler;
        this.protocolFactory = protocolFactory;
        this.clientConfiguration = clientConfiguration;

    }
    
    @Override
    public <ReturnT> ReturnT getObject(PresignedUrlGetObjectRequest presignedUrlGetObjectRequest,
                                       ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) 
                                       throws NoSuchKeyException, InvalidObjectStateException, 
                                              AwsServiceException, SdkClientException, S3Exception {
        
        InternalPresignedUrlGetObjectRequest internalRequest = convertToInternalRequest(presignedUrlGetObjectRequest);
        
        HttpResponseHandler<GetObjectResponse> responseHandler = protocolFactory.createResponseHandler(
            GetObjectResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));
        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, 
            internalRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? 
            NoOpMetricCollector.create() : MetricCollector.create("ApiCall");
        
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "S3");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetObject");

            SdkClientConfiguration updatedClientConfiguration = updateSdkClientConfiguration(internalRequest, 
                this.clientConfiguration);
            
            ClientExecutionParams<InternalPresignedUrlGetObjectRequest, GetObjectResponse> params = 
                new ClientExecutionParams<InternalPresignedUrlGetObjectRequest, GetObjectResponse>()
                    .withOperationName("PresignedUrlGetObject")
                    .withProtocolMetadata(PROTOCOL_METADATA)
                    .withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler)
                    .withInput(internalRequest)
                    .withRequestConfiguration(updatedClientConfiguration)
                    .withMetricCollector(apiCallMetricCollector)
                    .putExecutionAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION, true)
                    .putExecutionAttribute(
                        SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                        HttpChecksum.builder()
                            .requestChecksumRequired(false)
                            .isRequestStreaming(false)
                            .responseAlgorithmsV2(DefaultChecksumAlgorithm.CRC32C,
                                                  DefaultChecksumAlgorithm.CRC32, 
                                                  DefaultChecksumAlgorithm.CRC64NVME,
                                                  DefaultChecksumAlgorithm.SHA1, 
                                                  DefaultChecksumAlgorithm.SHA256).build())
                    .withMarshaller(new PresignedUrlGetObjectRequestMarshaller());
            
            return clientHandler.execute(params, responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }
    
    // @Override
    // public ResponseBytes<GetObjectResponse> getObjectAsBytes(PresignedUrlGetObjectRequest request) {
    //     return getObject(request, ResponseTransformer.toBytes());
    // }
    //
    // @Override
    // public ResponseInputStream<GetObjectResponse> getObjectAsStream(PresignedUrlGetObjectRequest request) {
    //     return getObject(request, ResponseTransformer.toInputStream());
    // }
    
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

    private SdkClientConfiguration updateSdkClientConfiguration(InternalPresignedUrlGetObjectRequest request,
                                                                SdkClientConfiguration clientConfiguration) {
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        configuration.option(SdkAdvancedClientOption.SIGNER, new NoOpSigner());
        configuration.option(SIGNER_OVERRIDDEN, true);
        return configuration.build();
    }

    private InternalPresignedUrlGetObjectRequest convertToInternalRequest(PresignedUrlGetObjectRequest request) {
        return InternalPresignedUrlGetObjectRequest.builder()
                .url(request.presignedUrl())
                .range(request.range())
                .buildInternal();
    }
}