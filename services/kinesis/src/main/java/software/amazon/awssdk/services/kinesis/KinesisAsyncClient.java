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

import com.example.ResponseIterator;
import com.example.SdkFlowResponseHandler;
import java.util.Iterator;
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
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsResponse;
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
import software.amazon.awssdk.services.kinesis.model.RecordBatchEvent;
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

    String serviceName();

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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the DeleteStream operation asynchronously.
     *
     * @param deleteStreamRequest
     * @return A Java Future containing the result of the DeleteStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the DescribeLimits operation asynchronously.
     *
     * @param describeLimitsRequest
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the DescribeStream operation asynchronously.
     *
     * @param describeStreamRequest
     * @return A Java Future containing the result of the DescribeStream operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the DescribeStreamSummary operation asynchronously.
     *
     * @param describeStreamSummaryRequest
     * @return A Java Future containing the result of the DescribeStreamSummary operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException</li>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the ListStreams operation asynchronously.
     *
     * @param listStreamsRequest
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
                                                                 SdkFlowResponseHandler<SubscribeToShardResponse, RecordBatchEvent, ReturnT> flowResponseHandler) {
        throw new UnsupportedOperationException();
    }

    default ResponseIterator<SubscribeToShardResponse, RecordBatchEvent> subscribeToShardBlocking(SubscribeToShardRequest request) {
        throw new UnsupportedOperationException();
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the DescribeLimits operation asynchronously.
     *
     * @return A Java Future containing the result of the DescribeLimits operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
     * Invokes the ListStreams operation asynchronously.
     *
     * @return A Java Future containing the result of the ListStreams operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
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
}
