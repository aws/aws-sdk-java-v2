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

import org.w3c.dom.*;

import java.net.*;
import java.util.*;



import org.apache.commons.logging.*;

import com.amazonaws.*;
import com.amazonaws.annotation.SdkInternalApi;
import com.amazonaws.auth.*;

import com.amazonaws.handlers.*;
import com.amazonaws.http.*;
import com.amazonaws.internal.*;
import com.amazonaws.internal.auth.*;
import com.amazonaws.metrics.*;
import com.amazonaws.regions.*;
import com.amazonaws.transform.*;
import com.amazonaws.util.*;
import com.amazonaws.protocol.rpcv2cbor.*;
import com.amazonaws.util.AWSRequestMetrics.Field;
import com.amazonaws.annotation.ThreadSafe;
import com.amazonaws.client.AwsSyncClientParams;
import com.amazonaws.client.builder.AdvancedConfig;

import com.amazonaws.services.protocol.rpcv2protocol.RpcV2ProtocolClientBuilder;

import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.protocol.rpcv2protocol.model.*;

import com.amazonaws.services.protocol.rpcv2protocol.model.transform.*;

/**
 * Client for accessing RpcV2Protocol. All service calls made using this client are blocking, and will not return until
 * the service call completes.
 * <p>
 * 
 */
@ThreadSafe

public class RpcV2ProtocolClient extends AmazonWebServiceClient implements RpcV2Protocol {

    /** Provider for AWS credentials. */
    private final AWSCredentialsProvider awsCredentialsProvider;

    private static final Log log = LogFactory.getLog(RpcV2Protocol.class);

    /** Default signing name for the service. */
    private static final String DEFAULT_SIGNING_NAME = "RpcV2Protocol";

    /** Client configuration factory providing ClientConfigurations tailored to this client */
    protected static final ClientConfigurationFactory configFactory = new ClientConfigurationFactory();

    private final AdvancedConfig advancedConfig;

    private static final SdkRpcV2CborProtocolFactory protocolFactory = new SdkRpcV2CborProtocolFactory(
            new RpcV2CborClientMetadata()
                    .addErrorMetadata(
                            new RpcV2CborErrorShapeMetadata().withErrorCode("ValidationException").withExceptionUnmarshaller(
                                    ValidationExceptionUnmarshaller.getInstance()))
                    .addErrorMetadata(
                            new RpcV2CborErrorShapeMetadata().withErrorCode("InvalidGreeting").withExceptionUnmarshaller(
                                    InvalidGreetingExceptionUnmarshaller.getInstance()))
                    .addErrorMetadata(
                            new RpcV2CborErrorShapeMetadata().withErrorCode("ComplexError").withExceptionUnmarshaller(
                                    ComplexErrorExceptionUnmarshaller.getInstance()))
                    .withBaseServiceExceptionClass(RpcV2ProtocolException.class));

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol. A credentials provider chain will be used
     * that searches for credentials in this order:
     * <ul>
     * <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
     * <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
     * <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @see DefaultAWSCredentialsProviderChain
     * @deprecated use {@link RpcV2ProtocolClientBuilder#defaultClient()}
     */
    @Deprecated
    public RpcV2ProtocolClient() {
        this(DefaultAWSCredentialsProviderChain.getInstance(), configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol. A credentials provider chain will be used
     * that searches for credentials in this order:
     * <ul>
     * <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
     * <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
     * <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RpcV2Protocol (ex: proxy
     *        settings, retry counts, etc.).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public RpcV2ProtocolClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified AWS account credentials.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withCredentials(AWSCredentialsProvider)} for example:
     *             {@code RpcV2ProtocolClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();}
     */
    @Deprecated
    public RpcV2ProtocolClient(AWSCredentials awsCredentials) {
        this(awsCredentials, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified AWS account credentials
     * and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RpcV2Protocol (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public RpcV2ProtocolClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.awsCredentialsProvider = new StaticCredentialsProvider(awsCredentials);
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified AWS account credentials
     * provider.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public RpcV2ProtocolClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified AWS account credentials
     * provider and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RpcV2Protocol (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public RpcV2ProtocolClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified AWS account credentials
     * provider, client configuration options, and request metric collector.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RpcV2Protocol (ex: proxy
     *        settings, retry counts, etc.).
     * @param requestMetricCollector
     *        optional request metric collector
     * @deprecated use {@link RpcV2ProtocolClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link RpcV2ProtocolClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link RpcV2ProtocolClientBuilder#withMetricsCollector(RequestMetricCollector)}
     */
    @Deprecated
    public RpcV2ProtocolClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    public static RpcV2ProtocolClientBuilder builder() {
        return RpcV2ProtocolClientBuilder.standard();
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    RpcV2ProtocolClient(AwsSyncClientParams clientParams) {
        this(clientParams, false);
    }

    /**
     * Constructs a new client to invoke service methods on RpcV2Protocol using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    RpcV2ProtocolClient(AwsSyncClientParams clientParams, boolean endpointDiscoveryEnabled) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        this.advancedConfig = clientParams.getAdvancedConfig();
        init();
    }

    private void init() {
        setServiceNameIntern(DEFAULT_SIGNING_NAME);
        setEndpointPrefix(ENDPOINT_PREFIX);
        // calling this.setEndPoint(...) will also modify the signer accordingly
        setEndpoint("https://protocol-rpcv2protocol.us-east-1.amazonaws.com");
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandler2s.addAll(chainFactory.newRequestHandlerChain("/com/amazonaws/services/protocol/rpcv2protocol/request.handlers"));
        requestHandler2s.addAll(chainFactory.newRequestHandler2Chain("/com/amazonaws/services/protocol/rpcv2protocol/request.handler2s"));
        requestHandler2s.addAll(chainFactory.getGlobalHandlers());
    }

    /**
     * @param emptyInputOutputRequest
     * @return Result of the EmptyInputOutput operation returned by the service.
     * @sample RpcV2Protocol.EmptyInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/EmptyInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public EmptyInputOutputResult emptyInputOutput(EmptyInputOutputRequest request) {
        request = beforeClientExecution(request);
        return executeEmptyInputOutput(request);
    }

    @SdkInternalApi
    final EmptyInputOutputResult executeEmptyInputOutput(EmptyInputOutputRequest emptyInputOutputRequest) {

        ExecutionContext executionContext = createExecutionContext(emptyInputOutputRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<EmptyInputOutputRequest> request = null;
        Response<EmptyInputOutputResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new EmptyInputOutputRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(emptyInputOutputRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "EmptyInputOutput");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<EmptyInputOutputResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new EmptyInputOutputResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param float16Request
     * @return Result of the Float16 operation returned by the service.
     * @sample RpcV2Protocol.Float16
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/Float16" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public Float16Result float16(Float16Request request) {
        request = beforeClientExecution(request);
        return executeFloat16(request);
    }

    @SdkInternalApi
    final Float16Result executeFloat16(Float16Request float16Request) {

        ExecutionContext executionContext = createExecutionContext(float16Request);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<Float16Request> request = null;
        Response<Float16Result> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new Float16RequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(float16Request));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "Float16");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<Float16Result>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new Float16ResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param fractionalSecondsRequest
     * @return Result of the FractionalSeconds operation returned by the service.
     * @sample RpcV2Protocol.FractionalSeconds
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public FractionalSecondsResult fractionalSeconds(FractionalSecondsRequest request) {
        request = beforeClientExecution(request);
        return executeFractionalSeconds(request);
    }

    @SdkInternalApi
    final FractionalSecondsResult executeFractionalSeconds(FractionalSecondsRequest fractionalSecondsRequest) {

        ExecutionContext executionContext = createExecutionContext(fractionalSecondsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<FractionalSecondsRequest> request = null;
        Response<FractionalSecondsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new FractionalSecondsRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(fractionalSecondsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "FractionalSeconds");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<FractionalSecondsResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new FractionalSecondsResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * <p>
     * This operation has three possible return values:
     * </p>
     * <ol>
     * <li>A successful response in the form of GreetingWithErrorsOutput</li>
     * <li>An InvalidGreeting error.</li>
     * <li>A ComplexError error.</li>
     * </ol>
     * <p>
     * Implementations must be able to successfully take a response and properly deserialize successful and error
     * responses.
     * </p>
     * 
     * @param greetingWithErrorsRequest
     * @return Result of the GreetingWithErrors operation returned by the service.
     * @throws ComplexErrorException
     *         This error is thrown when a request is invalid.
     * @throws InvalidGreetingException
     *         This error is thrown when an invalid greeting value is provided.
     * @sample RpcV2Protocol.GreetingWithErrors
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GreetingWithErrorsResult greetingWithErrors(GreetingWithErrorsRequest request) {
        request = beforeClientExecution(request);
        return executeGreetingWithErrors(request);
    }

    @SdkInternalApi
    final GreetingWithErrorsResult executeGreetingWithErrors(GreetingWithErrorsRequest greetingWithErrorsRequest) {

        ExecutionContext executionContext = createExecutionContext(greetingWithErrorsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<GreetingWithErrorsRequest> request = null;
        Response<GreetingWithErrorsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new GreetingWithErrorsRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(greetingWithErrorsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "GreetingWithErrors");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<GreetingWithErrorsResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new GreetingWithErrorsResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param noInputOutputRequest
     * @return Result of the NoInputOutput operation returned by the service.
     * @sample RpcV2Protocol.NoInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/NoInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public NoInputOutputResult noInputOutput(NoInputOutputRequest request) {
        request = beforeClientExecution(request);
        return executeNoInputOutput(request);
    }

    @SdkInternalApi
    final NoInputOutputResult executeNoInputOutput(NoInputOutputRequest noInputOutputRequest) {

        ExecutionContext executionContext = createExecutionContext(noInputOutputRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<NoInputOutputRequest> request = null;
        Response<NoInputOutputResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new NoInputOutputRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(noInputOutputRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "NoInputOutput");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<NoInputOutputResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new NoInputOutputResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param optionalInputOutputRequest
     * @return Result of the OptionalInputOutput operation returned by the service.
     * @sample RpcV2Protocol.OptionalInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OptionalInputOutputResult optionalInputOutput(OptionalInputOutputRequest request) {
        request = beforeClientExecution(request);
        return executeOptionalInputOutput(request);
    }

    @SdkInternalApi
    final OptionalInputOutputResult executeOptionalInputOutput(OptionalInputOutputRequest optionalInputOutputRequest) {

        ExecutionContext executionContext = createExecutionContext(optionalInputOutputRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OptionalInputOutputRequest> request = null;
        Response<OptionalInputOutputResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OptionalInputOutputRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(optionalInputOutputRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OptionalInputOutput");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OptionalInputOutputResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new OptionalInputOutputResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param recursiveShapesRequest
     * @return Result of the RecursiveShapes operation returned by the service.
     * @sample RpcV2Protocol.RecursiveShapes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapes" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public RecursiveShapesResult recursiveShapes(RecursiveShapesRequest request) {
        request = beforeClientExecution(request);
        return executeRecursiveShapes(request);
    }

    @SdkInternalApi
    final RecursiveShapesResult executeRecursiveShapes(RecursiveShapesRequest recursiveShapesRequest) {

        ExecutionContext executionContext = createExecutionContext(recursiveShapesRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<RecursiveShapesRequest> request = null;
        Response<RecursiveShapesResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new RecursiveShapesRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(recursiveShapesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "RecursiveShapes");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<RecursiveShapesResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new RecursiveShapesResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * <p>
     * The example tests basic map serialization.
     * </p>
     * 
     * @param rpcV2CborDenseMapsRequest
     * @return Result of the RpcV2CborDenseMaps operation returned by the service.
     * @throws ValidationException
     *         A standard error for input validation failures. This should be thrown by services when a member of the
     *         input structure falls outside of the modeled or documented constraints.
     * @sample RpcV2Protocol.RpcV2CborDenseMaps
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public RpcV2CborDenseMapsResult rpcV2CborDenseMaps(RpcV2CborDenseMapsRequest request) {
        request = beforeClientExecution(request);
        return executeRpcV2CborDenseMaps(request);
    }

    @SdkInternalApi
    final RpcV2CborDenseMapsResult executeRpcV2CborDenseMaps(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest) {

        ExecutionContext executionContext = createExecutionContext(rpcV2CborDenseMapsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<RpcV2CborDenseMapsRequest> request = null;
        Response<RpcV2CborDenseMapsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new RpcV2CborDenseMapsRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(rpcV2CborDenseMapsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "RpcV2CborDenseMaps");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<RpcV2CborDenseMapsResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new RpcV2CborDenseMapsResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * <p>
     * This test case serializes JSON lists for the following cases for both input and output:
     * </p>
     * <ol>
     * <li>Normal lists.</li>
     * <li>Normal sets.</li>
     * <li>Lists of lists.</li>
     * <li>Lists of structures.</li>
     * </ol>
     * 
     * @param rpcV2CborListsRequest
     * @return Result of the RpcV2CborLists operation returned by the service.
     * @throws ValidationException
     *         A standard error for input validation failures. This should be thrown by services when a member of the
     *         input structure falls outside of the modeled or documented constraints.
     * @sample RpcV2Protocol.RpcV2CborLists
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborLists" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public RpcV2CborListsResult rpcV2CborLists(RpcV2CborListsRequest request) {
        request = beforeClientExecution(request);
        return executeRpcV2CborLists(request);
    }

    @SdkInternalApi
    final RpcV2CborListsResult executeRpcV2CborLists(RpcV2CborListsRequest rpcV2CborListsRequest) {

        ExecutionContext executionContext = createExecutionContext(rpcV2CborListsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<RpcV2CborListsRequest> request = null;
        Response<RpcV2CborListsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new RpcV2CborListsRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(rpcV2CborListsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "RpcV2CborLists");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<RpcV2CborListsResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new RpcV2CborListsResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param simpleScalarPropertiesRequest
     * @return Result of the SimpleScalarProperties operation returned by the service.
     * @sample RpcV2Protocol.SimpleScalarProperties
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public SimpleScalarPropertiesResult simpleScalarProperties(SimpleScalarPropertiesRequest request) {
        request = beforeClientExecution(request);
        return executeSimpleScalarProperties(request);
    }

    @SdkInternalApi
    final SimpleScalarPropertiesResult executeSimpleScalarProperties(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest) {

        ExecutionContext executionContext = createExecutionContext(simpleScalarPropertiesRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<SimpleScalarPropertiesRequest> request = null;
        Response<SimpleScalarPropertiesResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new SimpleScalarPropertiesRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(simpleScalarPropertiesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "RpcV2Protocol");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "SimpleScalarProperties");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<SimpleScalarPropertiesResult>> responseHandler = protocolFactory.createResponseHandler(
                    new RpcV2CborOperationMetadata().withPayloadRpcV2Cbor(true).withHasStreamingSuccessResponse(false),
                    new SimpleScalarPropertiesResultRpcV2CborUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * Returns additional metadata for a previously executed successful, request, typically used for debugging issues
     * where a service isn't acting as expected. This data isn't considered part of the result data returned by an
     * operation, so it's available through this separate, diagnostic interface.
     * <p>
     * Response metadata is only cached for a limited period of time, so if you need to access this extra diagnostic
     * information for an executed request, you should use this method to retrieve it as soon as possible after
     * executing the request.
     *
     * @param request
     *        The originally executed request
     *
     * @return The response metadata for the specified request, or null if none is available.
     */
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return client.getResponseMetadataForRequest(request);
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the request level.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> invoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext) {

        return invoke(request, responseHandler, executionContext, null, null);
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the request level.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> invoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext, URI cachedEndpoint, URI uriFromEndpointTrait) {

        executionContext.setCredentialsProvider(CredentialUtils.getCredentialsProvider(request.getOriginalRequest(), awsCredentialsProvider));

        return doInvoke(request, responseHandler, executionContext, cachedEndpoint, uriFromEndpointTrait);
    }

    /**
     * Invoke with no authentication. Credentials are not required and any credentials set on the client or request will
     * be ignored for this operation.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> anonymousInvoke(Request<Y> request,
            HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler, ExecutionContext executionContext) {

        return doInvoke(request, responseHandler, executionContext, null, null);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been configured in the
     * ExecutionContext beforehand.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> doInvoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
            ExecutionContext executionContext, URI discoveredEndpoint, URI uriFromEndpointTrait) {

        if (discoveredEndpoint != null) {
            request.setEndpoint(discoveredEndpoint);
            request.getOriginalRequest().getRequestClientOptions().appendUserAgent("endpoint-discovery");
        } else if (uriFromEndpointTrait != null) {
            request.setEndpoint(uriFromEndpointTrait);
        } else {
            request.setEndpoint(endpoint);
        }

        request.setTimeOffset(timeOffset);

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler(new RpcV2CborErrorResponseMetadata());

        return client.execute(request, responseHandler, errorResponseHandler, executionContext);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
