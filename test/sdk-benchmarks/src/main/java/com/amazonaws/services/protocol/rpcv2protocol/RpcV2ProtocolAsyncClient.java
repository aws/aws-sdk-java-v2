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
package com.amazonaws.services.protocol.rpcv2protocol;

import static java.util.concurrent.Executors.newFixedThreadPool;



import com.amazonaws.services.protocol.rpcv2protocol.model.*;
import com.amazonaws.client.AwsAsyncClientParams;
import com.amazonaws.annotation.ThreadSafe;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.concurrent.ExecutorService;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

/**
 * Client for accessing RpcV2Protocol asynchronously. Each asynchronous method will return a Java Future object
 * representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 */
@ThreadSafe

public class RpcV2ProtocolAsyncClient extends RpcV2ProtocolClient implements RpcV2ProtocolAsync {

    private static final int DEFAULT_THREAD_POOL_SIZE = 50;

    private final ExecutorService executorService;

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol. A credentials provider chain
     * will be used that searches for credentials in this order:
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
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#defaultClient()}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient() {
        this(DefaultAWSCredentialsProviderChain.getInstance());
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol. A credentials provider chain
     * will be used that searches for credentials in this order:
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
     *        The client configuration options controlling how this client connects to RpcV2Protocol (ex: proxy
     *        settings, retry counts, etc).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration, newFixedThreadPool(clientConfiguration.getMaxConnections()));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials.
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing 50 threads (to match the default
     * maximum number of concurrent connections to the service).
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentials awsCredentials) {
        this(awsCredentials, newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials and executor service. Default client settings will be used.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentials awsCredentials, ExecutorService executorService) {

        this(awsCredentials, configFactory.getConfig(), executorService);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials, executor service, and client configuration options.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        Client configuration options (ex: max retry limit, proxy settings, etc).
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration, ExecutorService executorService) {
        super(awsCredentials, clientConfiguration);
        this.executorService = executorService;
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials provider. Default client settings will be used.
     * <p>
     * Asynchronous methods are delegated to a fixed-size thread pool containing 50 threads (to match the default
     * maximum number of concurrent connections to the service).
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @see java.util.concurrent.Executors#newFixedThreadPool(int)
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the provided AWS account
     * credentials provider and client configuration options.
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
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, newFixedThreadPool(clientConfiguration.getMaxConnections()));
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials provider and executor service. Default client settings will be used.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ExecutorService executorService) {
        this(awsCredentialsProvider, configFactory.getConfig(), executorService);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified AWS account
     * credentials provider, executor service, and client configuration options.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        Client configuration options (ex: max retry limit, proxy settings, etc).
     * @param executorService
     *        The executor service by which all asynchronous requests will be executed.
     * @deprecated use {@link RpcV2ProtocolAsyncClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link RpcV2ProtocolAsyncClientBuilder#withExecutorFactory(com.amazonaws.client.builder.ExecutorFactory)}
     */
    @Deprecated
    public RpcV2ProtocolAsyncClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration, ExecutorService executorService) {
        super(awsCredentialsProvider, clientConfiguration);
        this.executorService = executorService;
    }

    public static RpcV2ProtocolAsyncClientBuilder asyncBuilder() {
        return RpcV2ProtocolAsyncClientBuilder.standard();
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified parameters.
     *
     * @param asyncClientParams
     *        Object providing client parameters.
     */
    RpcV2ProtocolAsyncClient(AwsAsyncClientParams asyncClientParams) {
        this(asyncClientParams, false);
    }

    /**
     * Constructs a new asynchronous client to invoke service methods on RpcV2Protocol using the specified parameters.
     *
     * @param asyncClientParams
     *        Object providing client parameters.
     * @param endpointDiscoveryEnabled
     *        true will enable endpoint discovery if the service supports it.
     */
    RpcV2ProtocolAsyncClient(AwsAsyncClientParams asyncClientParams, boolean endpointDiscoveryEnabled) {
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
    public java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(EmptyInputOutputRequest request) {

        return emptyInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<EmptyInputOutputResult> emptyInputOutputAsync(final EmptyInputOutputRequest request,
            final com.amazonaws.handlers.AsyncHandler<EmptyInputOutputRequest, EmptyInputOutputResult> asyncHandler) {
        final EmptyInputOutputRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<EmptyInputOutputResult>() {
            @Override
            public EmptyInputOutputResult call() throws Exception {
                EmptyInputOutputResult result = null;

                try {
                    result = executeEmptyInputOutput(finalRequest);
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
    public java.util.concurrent.Future<Float16Result> float16Async(Float16Request request) {

        return float16Async(request, null);
    }

    @Override
    public java.util.concurrent.Future<Float16Result> float16Async(final Float16Request request,
            final com.amazonaws.handlers.AsyncHandler<Float16Request, Float16Result> asyncHandler) {
        final Float16Request finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<Float16Result>() {
            @Override
            public Float16Result call() throws Exception {
                Float16Result result = null;

                try {
                    result = executeFloat16(finalRequest);
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
    public java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(FractionalSecondsRequest request) {

        return fractionalSecondsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<FractionalSecondsResult> fractionalSecondsAsync(final FractionalSecondsRequest request,
            final com.amazonaws.handlers.AsyncHandler<FractionalSecondsRequest, FractionalSecondsResult> asyncHandler) {
        final FractionalSecondsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<FractionalSecondsResult>() {
            @Override
            public FractionalSecondsResult call() throws Exception {
                FractionalSecondsResult result = null;

                try {
                    result = executeFractionalSeconds(finalRequest);
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
    public java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(GreetingWithErrorsRequest request) {

        return greetingWithErrorsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<GreetingWithErrorsResult> greetingWithErrorsAsync(final GreetingWithErrorsRequest request,
            final com.amazonaws.handlers.AsyncHandler<GreetingWithErrorsRequest, GreetingWithErrorsResult> asyncHandler) {
        final GreetingWithErrorsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<GreetingWithErrorsResult>() {
            @Override
            public GreetingWithErrorsResult call() throws Exception {
                GreetingWithErrorsResult result = null;

                try {
                    result = executeGreetingWithErrors(finalRequest);
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
    public java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(NoInputOutputRequest request) {

        return noInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<NoInputOutputResult> noInputOutputAsync(final NoInputOutputRequest request,
            final com.amazonaws.handlers.AsyncHandler<NoInputOutputRequest, NoInputOutputResult> asyncHandler) {
        final NoInputOutputRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<NoInputOutputResult>() {
            @Override
            public NoInputOutputResult call() throws Exception {
                NoInputOutputResult result = null;

                try {
                    result = executeNoInputOutput(finalRequest);
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
    public java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(OptionalInputOutputRequest request) {

        return optionalInputOutputAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OptionalInputOutputResult> optionalInputOutputAsync(final OptionalInputOutputRequest request,
            final com.amazonaws.handlers.AsyncHandler<OptionalInputOutputRequest, OptionalInputOutputResult> asyncHandler) {
        final OptionalInputOutputRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<OptionalInputOutputResult>() {
            @Override
            public OptionalInputOutputResult call() throws Exception {
                OptionalInputOutputResult result = null;

                try {
                    result = executeOptionalInputOutput(finalRequest);
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
    public java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(RecursiveShapesRequest request) {

        return recursiveShapesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RecursiveShapesResult> recursiveShapesAsync(final RecursiveShapesRequest request,
            final com.amazonaws.handlers.AsyncHandler<RecursiveShapesRequest, RecursiveShapesResult> asyncHandler) {
        final RecursiveShapesRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<RecursiveShapesResult>() {
            @Override
            public RecursiveShapesResult call() throws Exception {
                RecursiveShapesResult result = null;

                try {
                    result = executeRecursiveShapes(finalRequest);
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
    public java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(RpcV2CborDenseMapsRequest request) {

        return rpcV2CborDenseMapsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborDenseMapsResult> rpcV2CborDenseMapsAsync(final RpcV2CborDenseMapsRequest request,
            final com.amazonaws.handlers.AsyncHandler<RpcV2CborDenseMapsRequest, RpcV2CborDenseMapsResult> asyncHandler) {
        final RpcV2CborDenseMapsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<RpcV2CborDenseMapsResult>() {
            @Override
            public RpcV2CborDenseMapsResult call() throws Exception {
                RpcV2CborDenseMapsResult result = null;

                try {
                    result = executeRpcV2CborDenseMaps(finalRequest);
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
    public java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(RpcV2CborListsRequest request) {

        return rpcV2CborListsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RpcV2CborListsResult> rpcV2CborListsAsync(final RpcV2CborListsRequest request,
            final com.amazonaws.handlers.AsyncHandler<RpcV2CborListsRequest, RpcV2CborListsResult> asyncHandler) {
        final RpcV2CborListsRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<RpcV2CborListsResult>() {
            @Override
            public RpcV2CborListsResult call() throws Exception {
                RpcV2CborListsResult result = null;

                try {
                    result = executeRpcV2CborLists(finalRequest);
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
    public java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(SimpleScalarPropertiesRequest request) {

        return simpleScalarPropertiesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<SimpleScalarPropertiesResult> simpleScalarPropertiesAsync(final SimpleScalarPropertiesRequest request,
            final com.amazonaws.handlers.AsyncHandler<SimpleScalarPropertiesRequest, SimpleScalarPropertiesResult> asyncHandler) {
        final SimpleScalarPropertiesRequest finalRequest = beforeClientExecution(request);

        return executorService.submit(new java.util.concurrent.Callable<SimpleScalarPropertiesResult>() {
            @Override
            public SimpleScalarPropertiesResult call() throws Exception {
                SimpleScalarPropertiesResult result = null;

                try {
                    result = executeSimpleScalarProperties(finalRequest);
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
