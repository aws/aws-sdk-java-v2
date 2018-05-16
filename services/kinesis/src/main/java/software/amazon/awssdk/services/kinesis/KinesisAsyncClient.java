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

import java.util.function.Consumer;
import software.amazon.awssdk.core.flow.ResponseIterator;
import software.amazon.awssdk.core.flow.FlowResponseTransformer;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Generated;
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
import software.amazon.awssdk.services.kinesis.model.GetChildShardsRequest;
import software.amazon.awssdk.services.kinesis.model.GetChildShardsResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodResponse;
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
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamRequest;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamResponse;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
import software.amazon.awssdk.services.kinesis.model.SplitShardResponse;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Service client for accessing Kinesis asynchronously. This can be created using the static {@link #builder()} method.
 *
 * null
 */
@Generated("software.amazon.awssdk:codegen")
public interface KinesisAsyncClient extends SdkAutoCloseable {

    String SERVICE_NAME = "kinesis";

    /**
     * Invokes the SubscribeToShard operation asynchronously.
     *
     * @param subscribeToShardRequest
     * @return A Java Future containing the result of the SubscribeToShard operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.SubscribeToShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SubscribeToShard" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<SubscribeToShardResponse> subscribeToShard(SubscribeToShardRequest subscribeToShardRequest) {
        throw new UnsupportedOperationException();
    }

    default  <ReturnT> CompletableFuture<ReturnT> subscribeToShard(SubscribeToShardRequest subscribeToShardRequest,
                                                                   FlowResponseTransformer<SubscribeToShardResponse, SubscribeToShardEvent, ReturnT> flowResponseHandler) {
        throw new UnsupportedOperationException();
    }

    default ResponseIterator<SubscribeToShardResponse, SubscribeToShardEvent> subscribeToShardBlocking(SubscribeToShardRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link KinesisAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.core.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from
     * the {@link software.amazon.awssdk.core.auth.DefaultCredentialsProvider}.
     */
    static KinesisAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link KinesisAsyncClient}.
     */
    static KinesisAsyncClientBuilder builder() {
        return new DefaultKinesisAsyncClientBuilder();
    }

    /**
     * Invokes the AddTagsToStream operation asynchronously.
     *
     * @param addTagsToStreamRequest
     * @return A Java Future containing the result of the AddTagsToStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.AddTagsToStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/AddTagsToStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<AddTagsToStreamResponse> addTagsToStream(AddTagsToStreamRequest addTagsToStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the AddTagsToStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link AddTagsToStreamRequest.Builder} avoiding the need
     * to create one manually via {@link AddTagsToStreamRequest#builder()}
     * </p>
     *
     * @param addTagsToStreamRequest
     *        A {@link Consumer} that will call methods on {@link AddTagsToStreamInput.Builder} to create a request.
     * @return A Java Future containing the result of the AddTagsToStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.AddTagsToStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/AddTagsToStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<AddTagsToStreamResponse> addTagsToStream(
        Consumer<AddTagsToStreamRequest.Builder> addTagsToStreamRequest) {
        return addTagsToStream(AddTagsToStreamRequest.builder().apply(addTagsToStreamRequest).build());
    }

    /**
     * Invokes the CreateStream operation asynchronously.
     *
     * @param createStreamRequest
     * @return A Java Future containing the result of the CreateStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.CreateStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/CreateStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<CreateStreamResponse> createStream(CreateStreamRequest createStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the CreateStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateStreamRequest.Builder} avoiding the need to
     * create one manually via {@link CreateStreamRequest#builder()}
     * </p>
     *
     * @param createStreamRequest
     *        A {@link Consumer} that will call methods on {@link CreateStreamInput.Builder} to create a request.
     * @return A Java Future containing the result of the CreateStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.CreateStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/CreateStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<CreateStreamResponse> createStream(Consumer<CreateStreamRequest.Builder> createStreamRequest) {
        return createStream(CreateStreamRequest.builder().apply(createStreamRequest).build());
    }

    /**
     * Invokes the DecreaseStreamRetentionPeriod operation asynchronously.
     *
     * @param decreaseStreamRetentionPeriodRequest
     * @return A Java Future containing the result of the DecreaseStreamRetentionPeriod operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DecreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DecreaseStreamRetentionPeriod"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
        DecreaseStreamRetentionPeriodRequest decreaseStreamRetentionPeriodRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DecreaseStreamRetentionPeriod operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DecreaseStreamRetentionPeriodRequest.Builder}
     * avoiding the need to create one manually via {@link DecreaseStreamRetentionPeriodRequest#builder()}
     * </p>
     *
     * @param decreaseStreamRetentionPeriodRequest
     *        A {@link Consumer} that will call methods on {@link DecreaseStreamRetentionPeriodInput.Builder} to create
     *        a request.
     * @return A Java Future containing the result of the DecreaseStreamRetentionPeriod operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DecreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DecreaseStreamRetentionPeriod"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
        Consumer<DecreaseStreamRetentionPeriodRequest.Builder> decreaseStreamRetentionPeriodRequest) {
        return decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder()
                                                                                 .apply(decreaseStreamRetentionPeriodRequest).build());
    }

    /**
     * Invokes the DeleteStream operation asynchronously.
     *
     * @param deleteStreamRequest
     * @return A Java Future containing the result of the DeleteStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DeleteStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeleteStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DeleteStreamResponse> deleteStream(DeleteStreamRequest deleteStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeleteStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteStreamRequest.Builder} avoiding the need to
     * create one manually via {@link DeleteStreamRequest#builder()}
     * </p>
     *
     * @param deleteStreamRequest
     *        A {@link Consumer} that will call methods on {@link DeleteStreamInput.Builder} to create a request.
     * @return A Java Future containing the result of the DeleteStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DeleteStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeleteStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DeleteStreamResponse> deleteStream(Consumer<DeleteStreamRequest.Builder> deleteStreamRequest) {
        return deleteStream(DeleteStreamRequest.builder().apply(deleteStreamRequest).build());
    }

    /**
     * Invokes the DeregisterStreamConsumer operation asynchronously.
     *
     * @param deregisterStreamConsumerRequest
     * @return A Java Future containing the result of the DeregisterStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DeregisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeregisterStreamConsumer"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer(
        DeregisterStreamConsumerRequest deregisterStreamConsumerRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeregisterStreamConsumer operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeregisterStreamConsumerRequest.Builder} avoiding
     * the need to create one manually via {@link DeregisterStreamConsumerRequest#builder()}
     * </p>
     *
     * @param deregisterStreamConsumerRequest
     *        A {@link Consumer} that will call methods on {@link DeregisterStreamConsumerInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DeregisterStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DeregisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeregisterStreamConsumer"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer(
        Consumer<DeregisterStreamConsumerRequest.Builder> deregisterStreamConsumerRequest) {
        return deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().apply(deregisterStreamConsumerRequest).build());
    }

    /**
     * Invokes the DeregisterStreamConsumer operation asynchronously.
     *
     * @return A Java Future containing the result of the DeregisterStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DeregisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DeregisterStreamConsumer"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer() {
        return deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().build());
    }

    /**
     * Invokes the DescribeLimits operation asynchronously.
     *
     * @param describeLimitsRequest
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeLimits
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeLimits" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DescribeLimits operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DescribeLimitsRequest.Builder} avoiding the need to
     * create one manually via {@link DescribeLimitsRequest#builder()}
     * </p>
     *
     * @param describeLimitsRequest
     *        A {@link Consumer} that will call methods on {@link DescribeLimitsInput.Builder} to create a request.
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeLimits
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeLimits" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeLimitsResponse> describeLimits(Consumer<DescribeLimitsRequest.Builder> describeLimitsRequest) {
        return describeLimits(DescribeLimitsRequest.builder().apply(describeLimitsRequest).build());
    }

    /**
     * Invokes the DescribeLimits operation asynchronously.
     *
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeLimits
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeLimits" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeLimitsResponse> describeLimits() {
        return describeLimits(DescribeLimitsRequest.builder().build());
    }

    /**
     * Invokes the DescribeStream operation asynchronously.
     *
     * @param describeStreamRequest
     * @return A Java Future containing the result of the DescribeStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeStreamResponse> describeStream(DescribeStreamRequest describeStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DescribeStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DescribeStreamRequest.Builder} avoiding the need to
     * create one manually via {@link DescribeStreamRequest#builder()}
     * </p>
     *
     * @param describeStreamRequest
     *        A {@link Consumer} that will call methods on {@link DescribeStreamInput.Builder} to create a request.
     * @return A Java Future containing the result of the DescribeStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeStreamResponse> describeStream(Consumer<DescribeStreamRequest.Builder> describeStreamRequest) {
        return describeStream(DescribeStreamRequest.builder().apply(describeStreamRequest).build());
    }

    /**
     * Invokes the DescribeStreamConsumer operation asynchronously.
     *
     * @param describeStreamConsumerRequest
     * @return A Java Future containing the result of the DescribeStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamConsumer" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(
        DescribeStreamConsumerRequest describeStreamConsumerRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DescribeStreamConsumer operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DescribeStreamConsumerRequest.Builder} avoiding the
     * need to create one manually via {@link DescribeStreamConsumerRequest#builder()}
     * </p>
     *
     * @param describeStreamConsumerRequest
     *        A {@link Consumer} that will call methods on {@link DescribeStreamConsumerInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DescribeStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamConsumer" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(
        Consumer<DescribeStreamConsumerRequest.Builder> describeStreamConsumerRequest) {
        return describeStreamConsumer(DescribeStreamConsumerRequest.builder().apply(describeStreamConsumerRequest).build());
    }

    /**
     * Invokes the DescribeStreamConsumer operation asynchronously.
     *
     * @return A Java Future containing the result of the DescribeStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamConsumer" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer() {
        return describeStreamConsumer(DescribeStreamConsumerRequest.builder().build());
    }

    /**
     * Invokes the DescribeStreamSummary operation asynchronously.
     *
     * @param describeStreamSummaryRequest
     * @return A Java Future containing the result of the DescribeStreamSummary operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStreamSummary
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamSummary" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<DescribeStreamSummaryResponse> describeStreamSummary(
        DescribeStreamSummaryRequest describeStreamSummaryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DescribeStreamSummary operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DescribeStreamSummaryRequest.Builder} avoiding the
     * need to create one manually via {@link DescribeStreamSummaryRequest#builder()}
     * </p>
     *
     * @param describeStreamSummaryRequest
     *        A {@link Consumer} that will call methods on {@link DescribeStreamSummaryInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DescribeStreamSummary operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DescribeStreamSummary
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DescribeStreamSummary" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<DescribeStreamSummaryResponse> describeStreamSummary(
        Consumer<DescribeStreamSummaryRequest.Builder> describeStreamSummaryRequest) {
        return describeStreamSummary(DescribeStreamSummaryRequest.builder().apply(describeStreamSummaryRequest).build());
    }

    /**
     * Invokes the DisableEnhancedMonitoring operation asynchronously.
     *
     * @param disableEnhancedMonitoringRequest
     * @return A Java Future containing the result of the DisableEnhancedMonitoring operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DisableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DisableEnhancedMonitoring"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(
        DisableEnhancedMonitoringRequest disableEnhancedMonitoringRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DisableEnhancedMonitoring operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DisableEnhancedMonitoringRequest.Builder} avoiding
     * the need to create one manually via {@link DisableEnhancedMonitoringRequest#builder()}
     * </p>
     *
     * @param disableEnhancedMonitoringRequest
     *        A {@link Consumer} that will call methods on {@link DisableEnhancedMonitoringInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DisableEnhancedMonitoring operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.DisableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/DisableEnhancedMonitoring"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(
        Consumer<DisableEnhancedMonitoringRequest.Builder> disableEnhancedMonitoringRequest) {
        return disableEnhancedMonitoring(DisableEnhancedMonitoringRequest.builder().apply(disableEnhancedMonitoringRequest)
                                                                         .build());
    }

    /**
     * Invokes the EnableEnhancedMonitoring operation asynchronously.
     *
     * @param enableEnhancedMonitoringRequest
     * @return A Java Future containing the result of the EnableEnhancedMonitoring operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.EnableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/EnableEnhancedMonitoring"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(
        EnableEnhancedMonitoringRequest enableEnhancedMonitoringRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the EnableEnhancedMonitoring operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link EnableEnhancedMonitoringRequest.Builder} avoiding
     * the need to create one manually via {@link EnableEnhancedMonitoringRequest#builder()}
     * </p>
     *
     * @param enableEnhancedMonitoringRequest
     *        A {@link Consumer} that will call methods on {@link EnableEnhancedMonitoringInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the EnableEnhancedMonitoring operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.EnableEnhancedMonitoring
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/EnableEnhancedMonitoring"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(
        Consumer<EnableEnhancedMonitoringRequest.Builder> enableEnhancedMonitoringRequest) {
        return enableEnhancedMonitoring(EnableEnhancedMonitoringRequest.builder().apply(enableEnhancedMonitoringRequest).build());
    }

    /**
     * Invokes the GetChildShards operation asynchronously.
     *
     * @param getChildShardsRequest
     * @return A Java Future containing the result of the GetChildShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetChildShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetChildShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetChildShardsResponse> getChildShards(GetChildShardsRequest getChildShardsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the GetChildShards operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetChildShardsRequest.Builder} avoiding the need to
     * create one manually via {@link GetChildShardsRequest#builder()}
     * </p>
     *
     * @param getChildShardsRequest
     *        A {@link Consumer} that will call methods on {@link GetChildShardsInput.Builder} to create a request.
     * @return A Java Future containing the result of the GetChildShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetChildShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetChildShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetChildShardsResponse> getChildShards(Consumer<GetChildShardsRequest.Builder> getChildShardsRequest) {
        return getChildShards(GetChildShardsRequest.builder().apply(getChildShardsRequest).build());
    }

    /**
     * Invokes the GetRecords operation asynchronously.
     *
     * @param getRecordsRequest
     * @return A Java Future containing the result of the GetRecords operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>ExpiredIteratorException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetRecords" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetRecordsResponse> getRecords(GetRecordsRequest getRecordsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the GetRecords operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetRecordsRequest.Builder} avoiding the need to
     * create one manually via {@link GetRecordsRequest#builder()}
     * </p>
     *
     * @param getRecordsRequest
     *        A {@link Consumer} that will call methods on {@link GetRecordsInput.Builder} to create a request.
     * @return A Java Future containing the result of the GetRecords operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>ExpiredIteratorException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetRecords" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetRecordsResponse> getRecords(Consumer<GetRecordsRequest.Builder> getRecordsRequest) {
        return getRecords(GetRecordsRequest.builder().apply(getRecordsRequest).build());
    }

    /**
     * Invokes the GetShardIterator operation asynchronously.
     *
     * @param getShardIteratorRequest
     * @return A Java Future containing the result of the GetShardIterator operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetShardIterator
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetShardIterator" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetShardIteratorResponse> getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the GetShardIterator operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetShardIteratorRequest.Builder} avoiding the need
     * to create one manually via {@link GetShardIteratorRequest#builder()}
     * </p>
     *
     * @param getShardIteratorRequest
     *        A {@link Consumer} that will call methods on {@link GetShardIteratorInput.Builder} to create a request.
     * @return A Java Future containing the result of the GetShardIterator operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.GetShardIterator
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/GetShardIterator" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetShardIteratorResponse> getShardIterator(
        Consumer<GetShardIteratorRequest.Builder> getShardIteratorRequest) {
        return getShardIterator(GetShardIteratorRequest.builder().apply(getShardIteratorRequest).build());
    }

    /**
     * Invokes the IncreaseStreamRetentionPeriod operation asynchronously.
     *
     * @param increaseStreamRetentionPeriodRequest
     * @return A Java Future containing the result of the IncreaseStreamRetentionPeriod operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.IncreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/IncreaseStreamRetentionPeriod"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
        IncreaseStreamRetentionPeriodRequest increaseStreamRetentionPeriodRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the IncreaseStreamRetentionPeriod operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link IncreaseStreamRetentionPeriodRequest.Builder}
     * avoiding the need to create one manually via {@link IncreaseStreamRetentionPeriodRequest#builder()}
     * </p>
     *
     * @param increaseStreamRetentionPeriodRequest
     *        A {@link Consumer} that will call methods on {@link IncreaseStreamRetentionPeriodInput.Builder} to create
     *        a request.
     * @return A Java Future containing the result of the IncreaseStreamRetentionPeriod operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.IncreaseStreamRetentionPeriod
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/IncreaseStreamRetentionPeriod"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
        Consumer<IncreaseStreamRetentionPeriodRequest.Builder> increaseStreamRetentionPeriodRequest) {
        return increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest.builder()
                                                                                 .apply(increaseStreamRetentionPeriodRequest).build());
    }

    /**
     * Invokes the ListShards operation asynchronously.
     *
     * @param listShardsRequest
     * @return A Java Future containing the result of the ListShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ExpiredNextTokenException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListShardsResponse> listShards(ListShardsRequest listShardsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the ListShards operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListShardsRequest.Builder} avoiding the need to
     * create one manually via {@link ListShardsRequest#builder()}
     * </p>
     *
     * @param listShardsRequest
     *        A {@link Consumer} that will call methods on {@link ListShardsInput.Builder} to create a request.
     * @return A Java Future containing the result of the ListShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ExpiredNextTokenException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListShardsResponse> listShards(Consumer<ListShardsRequest.Builder> listShardsRequest) {
        return listShards(ListShardsRequest.builder().apply(listShardsRequest).build());
    }

    /**
     * Invokes the ListShards operation asynchronously.
     *
     * @return A Java Future containing the result of the ListShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ExpiredNextTokenException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListShardsResponse> listShards() {
        return listShards(ListShardsRequest.builder().build());
    }

    /**
     * Invokes the ListStreamConsumers operation asynchronously.
     *
     * @param listStreamConsumersRequest
     * @return A Java Future containing the result of the ListStreamConsumers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ExpiredNextTokenException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListStreamConsumers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreamConsumers" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<ListStreamConsumersResponse> listStreamConsumers(
        ListStreamConsumersRequest listStreamConsumersRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the ListStreamConsumers operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListStreamConsumersRequest.Builder} avoiding the
     * need to create one manually via {@link ListStreamConsumersRequest#builder()}
     * </p>
     *
     * @param listStreamConsumersRequest
     *        A {@link Consumer} that will call methods on {@link ListStreamConsumersInput.Builder} to create a request.
     * @return A Java Future containing the result of the ListStreamConsumers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ExpiredNextTokenException</li>
     *         <li>ResourceInUseException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListStreamConsumers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreamConsumers" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<ListStreamConsumersResponse> listStreamConsumers(
        Consumer<ListStreamConsumersRequest.Builder> listStreamConsumersRequest) {
        return listStreamConsumers(ListStreamConsumersRequest.builder().apply(listStreamConsumersRequest).build());
    }

    /**
     * Invokes the ListStreams operation asynchronously.
     *
     * @param listStreamsRequest
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListStreams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreams" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListStreamsResponse> listStreams(ListStreamsRequest listStreamsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the ListStreams operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListStreamsRequest.Builder} avoiding the need to
     * create one manually via {@link ListStreamsRequest#builder()}
     * </p>
     *
     * @param listStreamsRequest
     *        A {@link Consumer} that will call methods on {@link ListStreamsInput.Builder} to create a request.
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListStreams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreams" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListStreamsResponse> listStreams(Consumer<ListStreamsRequest.Builder> listStreamsRequest) {
        return listStreams(ListStreamsRequest.builder().apply(listStreamsRequest).build());
    }

    /**
     * Invokes the ListStreams operation asynchronously.
     *
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListStreams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListStreams" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListStreamsResponse> listStreams() {
        return listStreams(ListStreamsRequest.builder().build());
    }

    /**
     * Invokes the ListTagsForStream operation asynchronously.
     *
     * @param listTagsForStreamRequest
     * @return A Java Future containing the result of the ListTagsForStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListTagsForStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListTagsForStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListTagsForStreamResponse> listTagsForStream(ListTagsForStreamRequest listTagsForStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the ListTagsForStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListTagsForStreamRequest.Builder} avoiding the need
     * to create one manually via {@link ListTagsForStreamRequest#builder()}
     * </p>
     *
     * @param listTagsForStreamRequest
     *        A {@link Consumer} that will call methods on {@link ListTagsForStreamInput.Builder} to create a request.
     * @return A Java Future containing the result of the ListTagsForStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.ListTagsForStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/ListTagsForStream" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListTagsForStreamResponse> listTagsForStream(
        Consumer<ListTagsForStreamRequest.Builder> listTagsForStreamRequest) {
        return listTagsForStream(ListTagsForStreamRequest.builder().apply(listTagsForStreamRequest).build());
    }

    /**
     * Invokes the MergeShards operation asynchronously.
     *
     * @param mergeShardsRequest
     * @return A Java Future containing the result of the MergeShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.MergeShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/MergeShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<MergeShardsResponse> mergeShards(MergeShardsRequest mergeShardsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the MergeShards operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link MergeShardsRequest.Builder} avoiding the need to
     * create one manually via {@link MergeShardsRequest#builder()}
     * </p>
     *
     * @param mergeShardsRequest
     *        A {@link Consumer} that will call methods on {@link MergeShardsInput.Builder} to create a request.
     * @return A Java Future containing the result of the MergeShards operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.MergeShards
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/MergeShards" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<MergeShardsResponse> mergeShards(Consumer<MergeShardsRequest.Builder> mergeShardsRequest) {
        return mergeShards(MergeShardsRequest.builder().apply(mergeShardsRequest).build());
    }

    /**
     * Invokes the PutRecord operation asynchronously.
     *
     * @param putRecordRequest
     * @return A Java Future containing the result of the PutRecord operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.PutRecord
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecord" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest putRecordRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PutRecord operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutRecordRequest.Builder} avoiding the need to
     * create one manually via {@link PutRecordRequest#builder()}
     * </p>
     *
     * @param putRecordRequest
     *        A {@link Consumer} that will call methods on {@link PutRecordInput.Builder} to create a request.
     * @return A Java Future containing the result of the PutRecord operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.PutRecord
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecord" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<PutRecordResponse> putRecord(Consumer<PutRecordRequest.Builder> putRecordRequest) {
        return putRecord(PutRecordRequest.builder().apply(putRecordRequest).build());
    }

    /**
     * Invokes the PutRecords operation asynchronously.
     *
     * @param putRecordsRequest
     * @return A Java Future containing the result of the PutRecords operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.PutRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecords" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<PutRecordsResponse> putRecords(PutRecordsRequest putRecordsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PutRecords operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutRecordsRequest.Builder} avoiding the need to
     * create one manually via {@link PutRecordsRequest#builder()}
     * </p>
     *
     * @param putRecordsRequest
     *        A {@link Consumer} that will call methods on {@link PutRecordsInput.Builder} to create a request.
     * @return A Java Future containing the result of the PutRecords operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.PutRecords
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/PutRecords" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<PutRecordsResponse> putRecords(Consumer<PutRecordsRequest.Builder> putRecordsRequest) {
        return putRecords(PutRecordsRequest.builder().apply(putRecordsRequest).build());
    }

    /**
     * Invokes the RegisterStreamConsumer operation asynchronously.
     *
     * @param registerStreamConsumerRequest
     * @return A Java Future containing the result of the RegisterStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.RegisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RegisterStreamConsumer" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RegisterStreamConsumerResponse> registerStreamConsumer(
        RegisterStreamConsumerRequest registerStreamConsumerRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the RegisterStreamConsumer operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RegisterStreamConsumerRequest.Builder} avoiding the
     * need to create one manually via {@link RegisterStreamConsumerRequest#builder()}
     * </p>
     *
     * @param registerStreamConsumerRequest
     *        A {@link Consumer} that will call methods on {@link RegisterStreamConsumerInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the RegisterStreamConsumer operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.RegisterStreamConsumer
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RegisterStreamConsumer" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RegisterStreamConsumerResponse> registerStreamConsumer(
        Consumer<RegisterStreamConsumerRequest.Builder> registerStreamConsumerRequest) {
        return registerStreamConsumer(RegisterStreamConsumerRequest.builder().apply(registerStreamConsumerRequest).build());
    }

    /**
     * Invokes the RemoveTagsFromStream operation asynchronously.
     *
     * @param removeTagsFromStreamRequest
     * @return A Java Future containing the result of the RemoveTagsFromStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.RemoveTagsFromStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RemoveTagsFromStream" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RemoveTagsFromStreamResponse> removeTagsFromStream(
        RemoveTagsFromStreamRequest removeTagsFromStreamRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the RemoveTagsFromStream operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RemoveTagsFromStreamRequest.Builder} avoiding the
     * need to create one manually via {@link RemoveTagsFromStreamRequest#builder()}
     * </p>
     *
     * @param removeTagsFromStreamRequest
     *        A {@link Consumer} that will call methods on {@link RemoveTagsFromStreamInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the RemoveTagsFromStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.RemoveTagsFromStream
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/RemoveTagsFromStream" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RemoveTagsFromStreamResponse> removeTagsFromStream(
        Consumer<RemoveTagsFromStreamRequest.Builder> removeTagsFromStreamRequest) {
        return removeTagsFromStream(RemoveTagsFromStreamRequest.builder().apply(removeTagsFromStreamRequest).build());
    }

    /**
     * Invokes the SplitShard operation asynchronously.
     *
     * @param splitShardRequest
     * @return A Java Future containing the result of the SplitShard operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.SplitShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SplitShard" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<SplitShardResponse> splitShard(SplitShardRequest splitShardRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the SplitShard operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SplitShardRequest.Builder} avoiding the need to
     * create one manually via {@link SplitShardRequest#builder()}
     * </p>
     *
     * @param splitShardRequest
     *        A {@link Consumer} that will call methods on {@link SplitShardInput.Builder} to create a request.
     * @return A Java Future containing the result of the SplitShard operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>ResourceInUseException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.SplitShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SplitShard" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<SplitShardResponse> splitShard(Consumer<SplitShardRequest.Builder> splitShardRequest) {
        return splitShard(SplitShardRequest.builder().apply(splitShardRequest).build());
    }

    /**
     * Invokes the StartStreamEncryption operation asynchronously.
     *
     * @param startStreamEncryptionRequest
     * @return A Java Future containing the result of the StartStreamEncryption operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.StartStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StartStreamEncryption" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<StartStreamEncryptionResponse> startStreamEncryption(
        StartStreamEncryptionRequest startStreamEncryptionRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the StartStreamEncryption operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StartStreamEncryptionRequest.Builder} avoiding the
     * need to create one manually via {@link StartStreamEncryptionRequest#builder()}
     * </p>
     *
     * @param startStreamEncryptionRequest
     *        A {@link Consumer} that will call methods on {@link StartStreamEncryptionInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the StartStreamEncryption operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>KMSDisabledException</li>
     *         <li>KMSInvalidStateException</li>
     *         <li>KMSAccessDeniedException</li>
     *         <li>KMSNotFoundException</li>
     *         <li>KMSOptInRequiredException</li>
     *         <li>KMSThrottlingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.StartStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StartStreamEncryption" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<StartStreamEncryptionResponse> startStreamEncryption(
        Consumer<StartStreamEncryptionRequest.Builder> startStreamEncryptionRequest) {
        return startStreamEncryption(StartStreamEncryptionRequest.builder().apply(startStreamEncryptionRequest).build());
    }

    /**
     * Invokes the StopStreamEncryption operation asynchronously.
     *
     * @param stopStreamEncryptionRequest
     * @return A Java Future containing the result of the StopStreamEncryption operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.StopStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StopStreamEncryption" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<StopStreamEncryptionResponse> stopStreamEncryption(
        StopStreamEncryptionRequest stopStreamEncryptionRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the StopStreamEncryption operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StopStreamEncryptionRequest.Builder} avoiding the
     * need to create one manually via {@link StopStreamEncryptionRequest#builder()}
     * </p>
     *
     * @param stopStreamEncryptionRequest
     *        A {@link Consumer} that will call methods on {@link StopStreamEncryptionInput.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the StopStreamEncryption operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.StopStreamEncryption
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/StopStreamEncryption" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<StopStreamEncryptionResponse> stopStreamEncryption(
        Consumer<StopStreamEncryptionRequest.Builder> stopStreamEncryptionRequest) {
        return stopStreamEncryption(StopStreamEncryptionRequest.builder().apply(stopStreamEncryptionRequest).build());
    }

    /**
     * Invokes the SubscribeToShard operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SubscribeToShardRequest.Builder} avoiding the need
     * to create one manually via {@link SubscribeToShardRequest#builder()}
     * </p>
     *
     * @param subscribeToShardRequest
     *        A {@link Consumer} that will call methods on {@link SubscribeToShardInput.Builder} to create a request.
     * @return A Java Future containing the result of the SubscribeToShard operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>InvalidArgumentException</li>
     *         <li>ProvisionedThroughputExceededException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.SubscribeToShard
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/SubscribeToShard" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<SubscribeToShardResponse> subscribeToShard(
        Consumer<SubscribeToShardRequest.Builder> subscribeToShardRequest) {
        return subscribeToShard(SubscribeToShardRequest.builder().apply(subscribeToShardRequest).build());
    }

    /**
     * Invokes the UpdateShardCount operation asynchronously.
     *
     * @param updateShardCountRequest
     * @return A Java Future containing the result of the UpdateShardCount operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.UpdateShardCount
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/UpdateShardCount" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<UpdateShardCountResponse> updateShardCount(UpdateShardCountRequest updateShardCountRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the UpdateShardCount operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link UpdateShardCountRequest.Builder} avoiding the need
     * to create one manually via {@link UpdateShardCountRequest#builder()}
     * </p>
     *
     * @param updateShardCountRequest
     *        A {@link Consumer} that will call methods on {@link UpdateShardCountInput.Builder} to create a request.
     * @return A Java Future containing the result of the UpdateShardCount operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgumentException</li>
     *         <li>LimitExceededException</li>
     *         <li>ResourceInUseException</li>
     *         <li>ResourceNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>KinesisException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample KinesisAsyncClient.UpdateShardCount
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/kinesis-2013-12-02/UpdateShardCount" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<UpdateShardCountResponse> updateShardCount(
        Consumer<UpdateShardCountRequest.Builder> updateShardCountRequest) {
        return updateShardCount(UpdateShardCountRequest.builder().apply(updateShardCountRequest).build());
    }

}
