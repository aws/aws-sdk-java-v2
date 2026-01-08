/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.protocol.restxml;

import static java.util.concurrent.Executors.newFixedThreadPool;



import com.amazonaws.services.protocol.restxml.model.*;
import com.amazonaws.client.AwsAsyncClientParams;
import com.amazonaws.annotation.ThreadSafe;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.concurrent.ExecutorService;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

/**
 * Client for accessing RestXmlProtocolTests asynchronously. Each asynchronous method will return a Java Future object
 * representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 */
@ThreadSafe

public class AmazonProtocolRestXmlAsyncClient extends AmazonProtocolRestXmlClient implements AmazonProtocolRestXmlAsync {

    private static final int DEFAULT_THREAD_POOL_SIZE = 50;

    private final ExecutorService executorService;

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests. A credentials provider
     * chain will be used that searches for credentials in this order:
     * <ul>
     * <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
     * <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
     * <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
     * <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing 50 threads (to match the default
     * maximum number of concurrent connections to the service).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#defaultClient()}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient() {
        this(DefaultAWSCredentialsProviderChain.getInstance());
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests. A credentials provider
     * chain will be used that searches for credentials in this order:
     * <ul>
     * <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
     * <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
     * <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
     * <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing a number of threads equal to the
     * maximum number of concurrent connections configured via {@code ClientConfiguration.getMaxConnections()}.
     *
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RestXmlProtocolTests (ex: proxy
     *        settings, retry counts, etc).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration, newFixedThreadPool(clientConfiguration.getMaxConnections()));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials.
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing 50 threads (to match the default
     * maximum number of concurrent connections to the service).
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentials awsCredentials) {
        this(awsCredentials, newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials and executor service. Default client settings will be used.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentials awsCredentials, ExecutorService executorService) {

        this(awsCredentials, configFactory.getConfig(), executorService);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials, executor service, and client configuration options.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        Client configuration options (ex: max retry limit, proxy settings, etc).
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration, ExecutorService executorService) {
        super(awsCredentials, clientConfiguration);
        this.executorService = executorService;
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials provider. Default client settings will be used.
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing 50 threads (to match the default
     * maximum number of concurrent connections to the service).
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the provided AWS
     * account credentials provider and client configuration options.
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing a number of threads equal to the
     * maximum number of concurrent connections configured via {@code ClientConfiguration.getMaxConnections()}.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        Client configuration options (ex: max retry limit, proxy settings, etc).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, newFixedThreadPool(clientConfiguration.getMaxConnections()));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials provider and executor service. Default client settings will be used.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ExecutorService executorService) {
        this(awsCredentialsProvider, configFactory.getConfig(), executorService);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified AWS
     * account credentials provider, executor service, and client configuration options.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        Client configuration options (ex: max retry limit, proxy settings, etc).
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link AmazonProtocolRestXmlAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link AmazonProtocolRestXmlAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public AmazonProtocolRestXmlAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
            ExecutorService executorService) {
        super(awsCredentialsProvider, clientConfiguration);
        this.executorService = executorService;
    }

    public static AmazonProtocolRestXmlAsyncClientBuilder asyncBuilder() {
        return AmazonProtocolRestXmlAsyncClientBuilder.standard();
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified
     * parameters.
     *
     * @param asyncClientParams
     *        Object providing client parameters.
     */
    AmazonProtocolRestXmlAsyncClient(AwsAsyncClientParams asyncClientParams) {
        this(asyncClientParams, false);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RestXmlProtocolTests using the specified
     * parameters.
     *
     * @param asyncClientParams
     *        Object providing client parameters.
     * @param endpointDiscoveryEnabled
     *        true will enable endpoint discovery if the service supports it.
     */
    AmazonProtocolRestXmlAsyncClient(AwsAsyncClientParams asyncClientParams, boolean endpointDiscoveryEnabled) {
        super(asyncClientParams, endpointDiscoveryEnabled);
        this.executorService = asyncClientParams.getExecutor();
    }

    /**
     * Returns the executor service used by this client to execute async requests.
     *
     * @return The executor service used by this client to execute async requests.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest request) {

        return allTypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(final AllTypesRequest request,
            final com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler) {
        final AllTypesRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<AllTypesResult>() {
            @Override
            public AllTypesResult call() throws Exception {
                AllTypesResult result = null;

                try {
                    result = executeAllTypes(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest request) {

        return deleteOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(final DeleteOperationRequest request,
            final com.amazonaws.handlers.AsyncHandler<DeleteOperationRequest, DeleteOperationResult> asyncHandler) {
        final DeleteOperationRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<DeleteOperationResult>() {
            @Override
            public DeleteOperationResult call() throws Exception {
                DeleteOperationResult result = null;

                try {
                    result = executeDeleteOperation(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest request) {

        return idempotentOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(final IdempotentOperationRequest request,
            final com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler) {
        final IdempotentOperationRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<IdempotentOperationResult>() {
            @Override
            public IdempotentOperationResult call() throws Exception {
                IdempotentOperationResult result = null;

                try {
                    result = executeIdempotentOperation(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest request) {

        return mapOfStringToListOfStringInQueryParamsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            final MapOfStringToListOfStringInQueryParamsRequest request,
            final com.amazonaws.handlers.AsyncHandler<MapOfStringToListOfStringInQueryParamsRequest, MapOfStringToListOfStringInQueryParamsResult> asyncHandler) {
        final MapOfStringToListOfStringInQueryParamsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<MapOfStringToListOfStringInQueryParamsResult>() {
            @Override
            public MapOfStringToListOfStringInQueryParamsResult call() throws Exception {
                MapOfStringToListOfStringInQueryParamsResult result = null;

                try {
                    result = executeMapOfStringToListOfStringInQueryParams(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest request) {

        return membersInHeadersAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(final MembersInHeadersRequest request,
            final com.amazonaws.handlers.AsyncHandler<MembersInHeadersRequest, MembersInHeadersResult> asyncHandler) {
        final MembersInHeadersRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<MembersInHeadersResult>() {
            @Override
            public MembersInHeadersResult call() throws Exception {
                MembersInHeadersResult result = null;

                try {
                    result = executeMembersInHeaders(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest request) {

        return membersInQueryParamsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(final MembersInQueryParamsRequest request,
            final com.amazonaws.handlers.AsyncHandler<MembersInQueryParamsRequest, MembersInQueryParamsResult> asyncHandler) {
        final MembersInQueryParamsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<MembersInQueryParamsResult>() {
            @Override
            public MembersInQueryParamsResult call() throws Exception {
                MembersInQueryParamsResult result = null;

                try {
                    result = executeMembersInQueryParams(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest request) {

        return multiLocationOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(final MultiLocationOperationRequest request,
            final com.amazonaws.handlers.AsyncHandler<MultiLocationOperationRequest, MultiLocationOperationResult> asyncHandler) {
        final MultiLocationOperationRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<MultiLocationOperationResult>() {
            @Override
            public MultiLocationOperationResult call() throws Exception {
                MultiLocationOperationResult result = null;

                try {
                    result = executeMultiLocationOperation(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest request) {

        return operationWithExplicitPayloadBlobAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            final OperationWithExplicitPayloadBlobRequest request,
            final com.amazonaws.handlers.AsyncHandler<OperationWithExplicitPayloadBlobRequest, OperationWithExplicitPayloadBlobResult> asyncHandler) {
        final OperationWithExplicitPayloadBlobRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<OperationWithExplicitPayloadBlobResult>() {
            @Override
            public OperationWithExplicitPayloadBlobResult call() throws Exception {
                OperationWithExplicitPayloadBlobResult result = null;

                try {
                    result = executeOperationWithExplicitPayloadBlob(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest request) {

        return operationWithGreedyLabelAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(final OperationWithGreedyLabelRequest request,
            final com.amazonaws.handlers.AsyncHandler<OperationWithGreedyLabelRequest, OperationWithGreedyLabelResult> asyncHandler) {
        final OperationWithGreedyLabelRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<OperationWithGreedyLabelResult>() {
            @Override
            public OperationWithGreedyLabelResult call() throws Exception {
                OperationWithGreedyLabelResult result = null;

                try {
                    result = executeOperationWithGreedyLabel(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest request) {

        return operationWithModeledContentTypeAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            final OperationWithModeledContentTypeRequest request,
            final com.amazonaws.handlers.AsyncHandler<OperationWithModeledContentTypeRequest, OperationWithModeledContentTypeResult> asyncHandler) {
        final OperationWithModeledContentTypeRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<OperationWithModeledContentTypeResult>() {
            @Override
            public OperationWithModeledContentTypeResult call() throws Exception {
                OperationWithModeledContentTypeResult result = null;

                try {
                    result = executeOperationWithModeledContentType(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest request) {

        return queryParamWithoutValueAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(final QueryParamWithoutValueRequest request,
            final com.amazonaws.handlers.AsyncHandler<QueryParamWithoutValueRequest, QueryParamWithoutValueResult> asyncHandler) {
        final QueryParamWithoutValueRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<QueryParamWithoutValueResult>() {
            @Override
            public QueryParamWithoutValueResult call() throws Exception {
                QueryParamWithoutValueResult result = null;

                try {
                    result = executeQueryParamWithoutValue(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(RestXmlTypesRequest request) {

        return restXmlTypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(final RestXmlTypesRequest request,
            final com.amazonaws.handlers.AsyncHandler<RestXmlTypesRequest, RestXmlTypesResult> asyncHandler) {
        final RestXmlTypesRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<RestXmlTypesResult>() {
            @Override
            public RestXmlTypesResult call() throws Exception {
                RestXmlTypesResult result = null;

                try {
                    result = executeRestXmlTypes(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    @Override
    public java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest request) {

        return streamingOutputOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(final StreamingOutputOperationRequest request,
            final com.amazonaws.handlers.AsyncHandler<StreamingOutputOperationRequest, StreamingOutputOperationResult> asyncHandler) {
        final StreamingOutputOperationRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<StreamingOutputOperationResult>() {
            @Override
            public StreamingOutputOperationResult call() throws Exception {
                StreamingOutputOperationResult result = null;

                try {
                    result = executeStreamingOutputOperation(finalRequest);
                } catch (Exception ex) {
                    if (asyncHandler != null) {
                        asyncHandler.onError(ex);
                    }
                    throw ex;
                }

                if (asyncHandler != null) {
                    asyncHandler.onSuccess(finalRequest, result);
                }
                return result;
            }
        });
    }

    /**
     * Shuts down the client, releasing all managed resources. This includes forcibly terminating all pending
     * asynchronous service calls. Clients who wish to give pending asynchronous service calls time to complete should
     * call {@code getExecutorService().shutdown()} followed by {@code getExecutorService().awaitTermination()} prior to
     * calling this method.
     */
    @Override
    public void shutdown() {
        super.shutdown();
        executorService.shutdownNow();
    }
}
