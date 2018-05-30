/*
 * Copyright 2013-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.kinesis;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.config.AwsAsyncClientConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.core.client.AsyncClientHandler;
import software.amazon.awssdk.core.client.ClientExecutionParams;
import software.amazon.awssdk.core.flow.FlowResponseTransformer;
import software.amazon.awssdk.core.flow.UnmarshallingFlowAsyncResponseTransformer;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.services.kinesis.model.AddTagsToStreamRequest;
import software.amazon.awssdk.services.kinesis.model.AddTagsToStreamResponse;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodResponse;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DeregisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DeregisterStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
import software.amazon.awssdk.services.kinesis.model.DisableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.DisableEnhancedMonitoringResponse;
import software.amazon.awssdk.services.kinesis.model.EnableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.EnableEnhancedMonitoringResponse;
import software.amazon.awssdk.services.kinesis.model.ExpiredIteratorException;
import software.amazon.awssdk.services.kinesis.model.ExpiredNextTokenException;
import software.amazon.awssdk.services.kinesis.model.GetChildShardsRequest;
import software.amazon.awssdk.services.kinesis.model.GetChildShardsResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodResponse;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.KMSAccessDeniedException;
import software.amazon.awssdk.services.kinesis.model.KMSDisabledException;
import software.amazon.awssdk.services.kinesis.model.KMSInvalidStateException;
import software.amazon.awssdk.services.kinesis.model.KMSNotFoundException;
import software.amazon.awssdk.services.kinesis.model.KMSOptInRequiredException;
import software.amazon.awssdk.services.kinesis.model.KMSThrottlingException;
import software.amazon.awssdk.services.kinesis.model.LimitExceededException;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamConsumersRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamConsumersResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.ListTagsForStreamRequest;
import software.amazon.awssdk.services.kinesis.model.ListTagsForStreamResponse;
import software.amazon.awssdk.services.kinesis.model.MergeShardsRequest;
import software.amazon.awssdk.services.kinesis.model.MergeShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamRequest;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamResponse;
import software.amazon.awssdk.services.kinesis.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
import software.amazon.awssdk.services.kinesis.model.SplitShardResponse;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountResponse;
import software.amazon.awssdk.services.kinesis.transform.AddTagsToStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.AddTagsToStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.CreateStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.CreateStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DecreaseStreamRetentionPeriodRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DecreaseStreamRetentionPeriodResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DeleteStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DeleteStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DeregisterStreamConsumerRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DeregisterStreamConsumerResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeLimitsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeLimitsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamConsumerRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamConsumerResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamSummaryRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DescribeStreamSummaryResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.DisableEnhancedMonitoringRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.DisableEnhancedMonitoringResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.EnableEnhancedMonitoringRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.EnableEnhancedMonitoringResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetChildShardsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetChildShardsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetRecordsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetRecordsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetShardIteratorRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.GetShardIteratorResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.IncreaseStreamRetentionPeriodRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.IncreaseStreamRetentionPeriodResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListShardsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListShardsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListStreamConsumersRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListStreamConsumersResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListStreamsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListStreamsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListTagsForStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.ListTagsForStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.MergeShardsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.MergeShardsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.PutRecordRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.PutRecordResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.PutRecordsRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.PutRecordsResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.RegisterStreamConsumerRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.RegisterStreamConsumerResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.RemoveTagsFromStreamRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.RemoveTagsFromStreamResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.SplitShardRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.SplitShardResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.StartStreamEncryptionRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.StartStreamEncryptionResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.StopStreamEncryptionRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.StopStreamEncryptionResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.SubscribeToShardEventUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.SubscribeToShardRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.SubscribeToShardResponseUnmarshaller;
import software.amazon.awssdk.services.kinesis.transform.UpdateShardCountRequestMarshaller;
import software.amazon.awssdk.services.kinesis.transform.UpdateShardCountResponseUnmarshaller;

/**
 * Internal implementation of {@link KinesisAsyncClient}.
 *
 * @see KinesisAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultKinesisAsyncClient implements KinesisAsyncClient {
    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    protected DefaultKinesisAsyncClient(AwsAsyncClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration, null);
        this.protocolFactory = init();
    }

    /**
     * Invokes the SubscribeToShard operation asynchronously.
     *
     * @param subscribeToShardRequest
     * @return A Java Future containing the result of the SubscribeToShard operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>ProvisionedThroughputExceededException</li>
     * <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     * client). Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.SubscribeToShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SubscribeToShard" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<SubscribeToShardResponse> subscribeToShard(SubscribeToShardRequest subscribeToShardRequest) {

        HttpResponseHandler<SubscribeToShardResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new SubscribeToShardResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<SubscribeToShardRequest, SubscribeToShardResponse>()
                                         .withMarshaller(new SubscribeToShardRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(subscribeToShardRequest));
    }

    public <ReturnT> CompletableFuture<ReturnT> subscribeToShard(SubscribeToShardRequest subscribeToShardRequest,
                                                                 FlowResponseTransformer<SubscribeToShardResponse, SubscribeToShardEvent, ReturnT> flowResponseHandler) {

        HttpResponseHandler<SubscribeToShardResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata()
                .withPayloadJson(false)
                .withHasStreamingSuccessResponse(true),
            new SubscribeToShardResponseUnmarshaller());

        HttpResponseHandler<SubscribeToShardEvent> eventResponseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata()
                .withPayloadJson(true)
                .withHasStreamingSuccessResponse(false),
            new SubscribeToShardEventUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<SubscribeToShardRequest, SubscribeToShardResponse>()
                                         .withMarshaller(new SubscribeToShardRequestMarshaller(protocolFactory))
                                         .withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler)
                                         .withInput(subscribeToShardRequest),
                                     new UnmarshallingFlowAsyncResponseTransformer<>(flowResponseHandler, eventResponseHandler));
    }

    /**
     * Invokes the AddTagsToStream operation asynchronously.
     *
     * @param addTagsToStreamRequest
     * @return A Java Future containing the result of the AddTagsToStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>ResourceInUseException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.AddTagsToStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/AddTagsToStream" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<AddTagsToStreamResponse> addTagsToStream(AddTagsToStreamRequest addTagsToStreamRequest) {

        HttpResponseHandler<AddTagsToStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new AddTagsToStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<AddTagsToStreamRequest, AddTagsToStreamResponse>()
                                         .withMarshaller(new AddTagsToStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(addTagsToStreamRequest));
    }

    /**
     * Invokes the CreateStream operation asynchronously.
     *
     * @param createStreamRequest
     * @return A Java Future containing the result of the CreateStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceInUseException</li>
     * <li>LimitExceededException</li>
     * <li>InvalidArgumentException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.CreateStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/CreateStream" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<CreateStreamResponse> createStream(CreateStreamRequest createStreamRequest) {

        HttpResponseHandler<CreateStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new CreateStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<CreateStreamRequest, CreateStreamResponse>()
                                         .withMarshaller(new CreateStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(createStreamRequest));
    }

    /**
     * Invokes the DecreaseStreamRetentionPeriod operation asynchronously.
     *
     * @param decreaseStreamRetentionPeriodRequest
     * @return A Java Future containing the result of the DecreaseStreamRetentionPeriod operation returned by the
     * service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>LimitExceededException</li>
     * <li>InvalidArgumentException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DecreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DecreaseStreamRetentionPeriod"
     * target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
        DecreaseStreamRetentionPeriodRequest decreaseStreamRetentionPeriodRequest) {

        HttpResponseHandler<DecreaseStreamRetentionPeriodResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DecreaseStreamRetentionPeriodResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<DecreaseStreamRetentionPeriodRequest, DecreaseStreamRetentionPeriodResponse>()
                         .withMarshaller(new DecreaseStreamRetentionPeriodRequestMarshaller(protocolFactory))
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(decreaseStreamRetentionPeriodRequest));
    }

    /**
     * Invokes the DeleteStream operation asynchronously.
     *
     * @param deleteStreamRequest
     * @return A Java Future containing the result of the DeleteStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DeleteStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeleteStream" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteStreamResponse> deleteStream(DeleteStreamRequest deleteStreamRequest) {

        HttpResponseHandler<DeleteStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DeleteStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DeleteStreamRequest, DeleteStreamResponse>()
                                         .withMarshaller(new DeleteStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(deleteStreamRequest));
    }

    /**
     * Invokes the DeregisterStreamConsumer operation asynchronously.
     *
     * @param deregisterStreamConsumerRequest
     * @return A Java Future containing the result of the DeregisterStreamConsumer operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>LimitExceededException</li>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DeregisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeregisterStreamConsumer"
     * target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer(
        DeregisterStreamConsumerRequest deregisterStreamConsumerRequest) {

        HttpResponseHandler<DeregisterStreamConsumerResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DeregisterStreamConsumerResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<DeregisterStreamConsumerRequest, DeregisterStreamConsumerResponse>()
                         .withMarshaller(new DeregisterStreamConsumerRequestMarshaller(protocolFactory))
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(deregisterStreamConsumerRequest));
    }

    /**
     * Invokes the DescribeLimits operation asynchronously.
     *
     * @param describeLimitsRequest
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DescribeLimits
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeLimits" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {

        HttpResponseHandler<DescribeLimitsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DescribeLimitsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DescribeLimitsRequest, DescribeLimitsResponse>()
                                         .withMarshaller(new DescribeLimitsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(describeLimitsRequest));
    }

    /**
     * Invokes the DescribeStream operation asynchronously.
     *
     * @param describeStreamRequest
     * @return A Java Future containing the result of the DescribeStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DescribeStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStream" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<DescribeStreamResponse> describeStream(DescribeStreamRequest describeStreamRequest) {

        HttpResponseHandler<DescribeStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DescribeStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DescribeStreamRequest, DescribeStreamResponse>()
                                         .withMarshaller(new DescribeStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(describeStreamRequest));
    }

    /**
     * Invokes the DescribeStreamConsumer operation asynchronously.
     *
     * @param describeStreamConsumerRequest
     * @return A Java Future containing the result of the DescribeStreamConsumer operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>LimitExceededException</li>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DescribeStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamConsumer" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(
        DescribeStreamConsumerRequest describeStreamConsumerRequest) {

        HttpResponseHandler<DescribeStreamConsumerResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DescribeStreamConsumerResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DescribeStreamConsumerRequest, DescribeStreamConsumerResponse>()
                                         .withMarshaller(new DescribeStreamConsumerRequestMarshaller(protocolFactory))
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .withInput(describeStreamConsumerRequest));
    }

    /**
     * Invokes the DescribeStreamSummary operation asynchronously.
     *
     * @param describeStreamSummaryRequest
     * @return A Java Future containing the result of the DescribeStreamSummary operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DescribeStreamSummary
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamSummary" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<DescribeStreamSummaryResponse> describeStreamSummary(
        DescribeStreamSummaryRequest describeStreamSummaryRequest) {

        HttpResponseHandler<DescribeStreamSummaryResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DescribeStreamSummaryResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<DescribeStreamSummaryRequest, DescribeStreamSummaryResponse>()
                                         .withMarshaller(new DescribeStreamSummaryRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(describeStreamSummaryRequest));
    }

    /**
     * Invokes the DisableEnhancedMonitoring operation asynchronously.
     *
     * @param disableEnhancedMonitoringRequest
     * @return A Java Future containing the result of the DisableEnhancedMonitoring operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.DisableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DisableEnhancedMonitoring"
     * target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(
        DisableEnhancedMonitoringRequest disableEnhancedMonitoringRequest) {

        HttpResponseHandler<DisableEnhancedMonitoringResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new DisableEnhancedMonitoringResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<DisableEnhancedMonitoringRequest, DisableEnhancedMonitoringResponse>()
                         .withMarshaller(new DisableEnhancedMonitoringRequestMarshaller(protocolFactory))
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(disableEnhancedMonitoringRequest));
    }

    /**
     * Invokes the EnableEnhancedMonitoring operation asynchronously.
     *
     * @param enableEnhancedMonitoringRequest
     * @return A Java Future containing the result of the EnableEnhancedMonitoring operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.EnableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/EnableEnhancedMonitoring"
     * target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(
        EnableEnhancedMonitoringRequest enableEnhancedMonitoringRequest) {

        HttpResponseHandler<EnableEnhancedMonitoringResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new EnableEnhancedMonitoringResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<EnableEnhancedMonitoringRequest, EnableEnhancedMonitoringResponse>()
                         .withMarshaller(new EnableEnhancedMonitoringRequestMarshaller(protocolFactory))
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(enableEnhancedMonitoringRequest));
    }

    /**
     * Invokes the GetChildShards operation asynchronously.
     *
     * @param getChildShardsRequest
     * @return A Java Future containing the result of the GetChildShards operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.GetChildShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetChildShards" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<GetChildShardsResponse> getChildShards(GetChildShardsRequest getChildShardsRequest) {

        HttpResponseHandler<GetChildShardsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new GetChildShardsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<GetChildShardsRequest, GetChildShardsResponse>()
                                         .withMarshaller(new GetChildShardsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(getChildShardsRequest));
    }

    /**
     * Invokes the GetRecords operation asynchronously.
     *
     * @param getRecordsRequest
     * @return A Java Future containing the result of the GetRecords operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>ProvisionedThroughputExceededException</li>
     * <li>ExpiredIteratorException</li>
     * <li>KMSDisabledException</li>
     * <li>KMSInvalidStateException</li>
     * <li>KMSAccessDeniedException</li>
     * <li>KMSNotFoundException</li>
     * <li>KMSOptInRequiredException</li>
     * <li>KMSThrottlingException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.GetRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetRecords" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<GetRecordsResponse> getRecords(GetRecordsRequest getRecordsRequest) {

        HttpResponseHandler<GetRecordsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new GetRecordsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<GetRecordsRequest, GetRecordsResponse>()
                                         .withMarshaller(new GetRecordsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(getRecordsRequest));
    }

    /**
     * Invokes the GetShardIterator operation asynchronously.
     *
     * @param getShardIteratorRequest
     * @return A Java Future containing the result of the GetShardIterator operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>ProvisionedThroughputExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.GetShardIterator
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetShardIterator" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<GetShardIteratorResponse> getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {

        HttpResponseHandler<GetShardIteratorResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new GetShardIteratorResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<GetShardIteratorRequest, GetShardIteratorResponse>()
                                         .withMarshaller(new GetShardIteratorRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(getShardIteratorRequest));
    }

    /**
     * Invokes the IncreaseStreamRetentionPeriod operation asynchronously.
     *
     * @param increaseStreamRetentionPeriodRequest
     * @return A Java Future containing the result of the IncreaseStreamRetentionPeriod operation returned by the
     * service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>LimitExceededException</li>
     * <li>InvalidArgumentException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.IncreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/IncreaseStreamRetentionPeriod"
     * target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
        IncreaseStreamRetentionPeriodRequest increaseStreamRetentionPeriodRequest) {

        HttpResponseHandler<IncreaseStreamRetentionPeriodResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new IncreaseStreamRetentionPeriodResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<IncreaseStreamRetentionPeriodRequest, IncreaseStreamRetentionPeriodResponse>()
                         .withMarshaller(new IncreaseStreamRetentionPeriodRequestMarshaller(protocolFactory))
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(increaseStreamRetentionPeriodRequest));
    }

    /**
     * Invokes the ListShards operation asynchronously.
     *
     * @param listShardsRequest
     * @return A Java Future containing the result of the ListShards operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ExpiredNextTokenException</li>
     * <li>ResourceInUseException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.ListShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListShards" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<ListShardsResponse> listShards(ListShardsRequest listShardsRequest) {

        HttpResponseHandler<ListShardsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new ListShardsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListShardsRequest, ListShardsResponse>()
                                         .withMarshaller(new ListShardsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(listShardsRequest));
    }

    /**
     * Invokes the ListStreamConsumers operation asynchronously.
     *
     * @param listStreamConsumersRequest
     * @return A Java Future containing the result of the ListStreamConsumers operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ExpiredNextTokenException</li>
     * <li>ResourceInUseException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.ListStreamConsumers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreamConsumers" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<ListStreamConsumersResponse> listStreamConsumers(
        ListStreamConsumersRequest listStreamConsumersRequest) {

        HttpResponseHandler<ListStreamConsumersResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new ListStreamConsumersResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListStreamConsumersRequest, ListStreamConsumersResponse>()
                                         .withMarshaller(new ListStreamConsumersRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(listStreamConsumersRequest));
    }

    /**
     * Invokes the ListStreams operation asynchronously.
     *
     * @param listStreamsRequest
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.ListStreams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreams" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<ListStreamsResponse> listStreams(ListStreamsRequest listStreamsRequest) {

        HttpResponseHandler<ListStreamsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new ListStreamsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListStreamsRequest, ListStreamsResponse>()
                                         .withMarshaller(new ListStreamsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(listStreamsRequest));
    }

    /**
     * Invokes the ListTagsForStream operation asynchronously.
     *
     * @param listTagsForStreamRequest
     * @return A Java Future containing the result of the ListTagsForStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.ListTagsForStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListTagsForStream" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<ListTagsForStreamResponse> listTagsForStream(ListTagsForStreamRequest listTagsForStreamRequest) {

        HttpResponseHandler<ListTagsForStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new ListTagsForStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<ListTagsForStreamRequest, ListTagsForStreamResponse>()
                                         .withMarshaller(new ListTagsForStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(listTagsForStreamRequest));
    }

    /**
     * Invokes the MergeShards operation asynchronously.
     *
     * @param mergeShardsRequest
     * @return A Java Future containing the result of the MergeShards operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>ResourceInUseException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.MergeShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/MergeShards" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<MergeShardsResponse> mergeShards(MergeShardsRequest mergeShardsRequest) {

        HttpResponseHandler<MergeShardsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new MergeShardsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<MergeShardsRequest, MergeShardsResponse>()
                                         .withMarshaller(new MergeShardsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(mergeShardsRequest));
    }

    /**
     * Invokes the PutRecord operation asynchronously.
     *
     * @param putRecordRequest
     * @return A Java Future containing the result of the PutRecord operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>ProvisionedThroughputExceededException</li>
     * <li>KMSDisabledException</li>
     * <li>KMSInvalidStateException</li>
     * <li>KMSAccessDeniedException</li>
     * <li>KMSNotFoundException</li>
     * <li>KMSOptInRequiredException</li>
     * <li>KMSThrottlingException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.PutRecord
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecord" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest putRecordRequest) {

        HttpResponseHandler<PutRecordResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new PutRecordResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<PutRecordRequest, PutRecordResponse>()
                                         .withMarshaller(new PutRecordRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(putRecordRequest));
    }

    /**
     * Invokes the PutRecords operation asynchronously.
     *
     * @param putRecordsRequest
     * @return A Java Future containing the result of the PutRecords operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>InvalidArgumentException</li>
     * <li>ProvisionedThroughputExceededException</li>
     * <li>KMSDisabledException</li>
     * <li>KMSInvalidStateException</li>
     * <li>KMSAccessDeniedException</li>
     * <li>KMSNotFoundException</li>
     * <li>KMSOptInRequiredException</li>
     * <li>KMSThrottlingException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.PutRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecords" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<PutRecordsResponse> putRecords(PutRecordsRequest putRecordsRequest) {

        HttpResponseHandler<PutRecordsResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new PutRecordsResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<PutRecordsRequest, PutRecordsResponse>()
                                         .withMarshaller(new PutRecordsRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(putRecordsRequest));
    }

    /**
     * Invokes the RegisterStreamConsumer operation asynchronously.
     *
     * @param registerStreamConsumerRequest
     * @return A Java Future containing the result of the RegisterStreamConsumer operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.RegisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RegisterStreamConsumer" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<RegisterStreamConsumerResponse> registerStreamConsumer(
        RegisterStreamConsumerRequest registerStreamConsumerRequest) {

        HttpResponseHandler<RegisterStreamConsumerResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new RegisterStreamConsumerResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<RegisterStreamConsumerRequest, RegisterStreamConsumerResponse>()
                                         .withMarshaller(new RegisterStreamConsumerRequestMarshaller(protocolFactory))
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .withInput(registerStreamConsumerRequest));
    }

    /**
     * Invokes the RemoveTagsFromStream operation asynchronously.
     *
     * @param removeTagsFromStreamRequest
     * @return A Java Future containing the result of the RemoveTagsFromStream operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>ResourceInUseException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.RemoveTagsFromStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RemoveTagsFromStream" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<RemoveTagsFromStreamResponse> removeTagsFromStream(
        RemoveTagsFromStreamRequest removeTagsFromStreamRequest) {

        HttpResponseHandler<RemoveTagsFromStreamResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new RemoveTagsFromStreamResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<RemoveTagsFromStreamRequest, RemoveTagsFromStreamResponse>()
                                         .withMarshaller(new RemoveTagsFromStreamRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(removeTagsFromStreamRequest));
    }

    /**
     * Invokes the SplitShard operation asynchronously.
     *
     * @param splitShardRequest
     * @return A Java Future containing the result of the SplitShard operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>ResourceNotFoundException</li>
     * <li>ResourceInUseException</li>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.SplitShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SplitShard" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<SplitShardResponse> splitShard(SplitShardRequest splitShardRequest) {

        HttpResponseHandler<SplitShardResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new SplitShardResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<SplitShardRequest, SplitShardResponse>()
                                         .withMarshaller(new SplitShardRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(splitShardRequest));
    }

    /**
     * Invokes the StartStreamEncryption operation asynchronously.
     *
     * @param startStreamEncryptionRequest
     * @return A Java Future containing the result of the StartStreamEncryption operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>KMSDisabledException</li>
     * <li>KMSInvalidStateException</li>
     * <li>KMSAccessDeniedException</li>
     * <li>KMSNotFoundException</li>
     * <li>KMSOptInRequiredException</li>
     * <li>KMSThrottlingException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.StartStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StartStreamEncryption" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<StartStreamEncryptionResponse> startStreamEncryption(
        StartStreamEncryptionRequest startStreamEncryptionRequest) {

        HttpResponseHandler<StartStreamEncryptionResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new StartStreamEncryptionResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<StartStreamEncryptionRequest, StartStreamEncryptionResponse>()
                                         .withMarshaller(new StartStreamEncryptionRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(startStreamEncryptionRequest));
    }

    /**
     * Invokes the StopStreamEncryption operation asynchronously.
     *
     * @param stopStreamEncryptionRequest
     * @return A Java Future containing the result of the StopStreamEncryption operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.StopStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StopStreamEncryption" target="_top">AWS
     * API Documentation</a>
     */
    @Override
    public CompletableFuture<StopStreamEncryptionResponse> stopStreamEncryption(
        StopStreamEncryptionRequest stopStreamEncryptionRequest) {

        HttpResponseHandler<StopStreamEncryptionResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new StopStreamEncryptionResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<StopStreamEncryptionRequest, StopStreamEncryptionResponse>()
                                         .withMarshaller(new StopStreamEncryptionRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(stopStreamEncryptionRequest));
    }

    /**
     * Invokes the UpdateShardCount operation asynchronously.
     *
     * @param updateShardCountRequest
     * @return A Java Future containing the result of the UpdateShardCount operation returned by the service.<br/>
     * The CompletableFuture returned by this method can be completed exceptionally with the following
     * exceptions.
     * <ul>
     * <li>InvalidArgumentException</li>
     * <li>LimitExceededException</li>
     * <li>ResourceInUseException</li>
     * <li>ResourceNotFoundException</li>
     * <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     * Can be used for catch all scenarios.</li>
     * <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     * credentials, etc.</li>
     * <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     * instance of this type.</li>
     * </ul>
     * @sample KinesisAsyncClient.UpdateShardCount
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/UpdateShardCount" target="_top">AWS API
     * Documentation</a>
     */
    @Override
    public CompletableFuture<UpdateShardCountResponse> updateShardCount(UpdateShardCountRequest updateShardCountRequest) {

        HttpResponseHandler<UpdateShardCountResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new UpdateShardCountResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<UpdateShardCountRequest, UpdateShardCountResponse>()
                                         .withMarshaller(new UpdateShardCountRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler).withInput(updateShardCountRequest));
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    private AwsJsonProtocolFactory init() {
        return new AwsJsonProtocolFactory(new JsonClientMetadata()
                                              .withSupportsCbor(false)
                                              .withSupportsIon(false)
                                              .withBaseServiceExceptionClass(software.amazon.awssdk.services.kinesis.model.KinesisException.class)
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("InvalidArgumentException").withModeledClass(
                                                      InvalidArgumentException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("ResourceInUseException").withModeledClass(
                                                      ResourceInUseException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSOptInRequired").withModeledClass(
                                                      KMSOptInRequiredException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("ResourceNotFoundException").withModeledClass(
                                                      ResourceNotFoundException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("ExpiredIteratorException").withModeledClass(
                                                      ExpiredIteratorException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSAccessDeniedException").withModeledClass(
                                                      KMSAccessDeniedException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSInvalidStateException").withModeledClass(
                                                      KMSInvalidStateException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSDisabledException").withModeledClass(
                                                      KMSDisabledException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSNotFoundException").withModeledClass(
                                                      KMSNotFoundException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("ProvisionedThroughputExceededException").withModeledClass(
                                                      ProvisionedThroughputExceededException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("KMSThrottlingException").withModeledClass(
                                                      KMSThrottlingException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("ExpiredNextTokenException").withModeledClass(
                                                      ExpiredNextTokenException.class))
                                              .addErrorMetadata(
                                                  new JsonErrorShapeMetadata().withErrorCode("LimitExceededException").withModeledClass(
                                                      LimitExceededException.class)),
                                          AwsJsonProtocolMetadata.builder()
                                                                 .protocol(AwsJsonProtocol.AWS_JSON)
                                                                 .protocolVersion("1.1")
                                                                 .build());
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }
}

