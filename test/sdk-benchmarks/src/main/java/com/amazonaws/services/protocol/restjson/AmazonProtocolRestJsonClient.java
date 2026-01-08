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
package com.amazonaws.services.protocol.restjson;

import com.amazonaws.client.builder.AdvancedConfig;
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
import com.amazonaws.protocol.json.*;
import com.amazonaws.util.AWSRequestMetrics.Field;
import com.amazonaws.annotation.ThreadSafe;
import com.amazonaws.client.AwsSyncClientParams;

import com.amazonaws.services.protocol.restjson.AmazonProtocolRestJsonClientBuilder;

import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.protocol.restjson.model.*;

import com.amazonaws.services.protocol.restjson.model.transform.*;

/**
 * Client for accessing JsonProtocolTests. All service calls made using this client are blocking, and will not return
 * until the service call completes.
 * <p>
 * 
 */
@ThreadSafe

public class AmazonProtocolRestJsonClient extends AmazonWebServiceClient implements AmazonProtocolRestJson {

    /** Provider for AWS credentials. */
    private final AWSCredentialsProvider awsCredentialsProvider;

    private static final Log log = LogFactory.getLog(AmazonProtocolRestJson.class);

    /** Default signing name for the service. */
    private static final String DEFAULT_SIGNING_NAME = "restjson";

    /** Client configuration factory providing ClientConfigurations tailored to this client */
    protected static final ClientConfigurationFactory configFactory = new ClientConfigurationFactory();

    private final AdvancedConfig advancedConfig;

    private static final SdkJsonProtocolFactory protocolFactory = new SdkJsonProtocolFactory(
            new JsonClientMetadata()
                    .withProtocolVersion("1.1")
                    .withSupportsCbor(false)
                    .withSupportsIon(false)
                    .withContentTypeOverride("application/json")
                    .addErrorMetadata(
                            new JsonErrorShapeMetadata().withErrorCode("ModeledException").withExceptionUnmarshaller(
                                    ModeledExceptionUnmarshaller.getInstance()))
                    .addErrorMetadata(
                            new JsonErrorShapeMetadata().withErrorCode("EmptyModeledException").withExceptionUnmarshaller(
                                    EmptyModeledExceptionUnmarshaller.getInstance()))
                    .withBaseServiceExceptionClass(AmazonProtocolRestJsonException.class));

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests. A credentials provider chain will be used
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
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#defaultClient()}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient() {
        this(DefaultAWSCredentialsProviderChain.getInstance(), configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests. A credentials provider chain will be used
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
     *        The client configuration options controlling how this client connects to JsonProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified AWS account
     * credentials.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withCredentials(AWSCredentialsProvider)} for example:
     *             {@code AmazonProtocolRestJsonClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(AWSCredentials awsCredentials) {
        this(awsCredentials, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified AWS account
     * credentials and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to JsonProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestJsonClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.awsCredentialsProvider = new StaticCredentialsProvider(awsCredentials);
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified AWS account
     * credentials provider.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified AWS account
     * credentials provider and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to JsonProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestJsonClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified AWS account
     * credentials provider, client configuration options, and request metric collector.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to JsonProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @param requestMetricCollector
     *        optional request metric collector
     * @deprecated use {@link AmazonProtocolRestJsonClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestJsonClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link AmazonProtocolRestJsonClientBuilder#withMetricsCollector(RequestMetricCollector)}
     */
    @Deprecated
    public AmazonProtocolRestJsonClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    public static AmazonProtocolRestJsonClientBuilder builder() {
        return AmazonProtocolRestJsonClientBuilder.standard();
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolRestJsonClient(AwsSyncClientParams clientParams) {
        this(clientParams, false);
    }

    /**
     * Constructs a new client to invoke service methods on JsonProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolRestJsonClient(AwsSyncClientParams clientParams, boolean endpointDiscoveryEnabled) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        this.advancedConfig = clientParams.getAdvancedConfig();
        init();
    }

    private void init() {
        setServiceNameIntern(DEFAULT_SIGNING_NAME);
        setEndpointPrefix(ENDPOINT_PREFIX);
        // calling this.setEndPoint(...) will also modify the signer accordingly
        setEndpoint("https://protocol-restjson.us-east-1.amazonaws.com");
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandler2s.addAll(chainFactory.newRequestHandlerChain("/com/amazonaws/services/protocol/restjson/request.handlers"));
        requestHandler2s.addAll(chainFactory.newRequestHandler2Chain("/com/amazonaws/services/protocol/restjson/request.handler2s"));
        requestHandler2s.addAll(chainFactory.getGlobalHandlers());
    }

    /**
     * @param allTypesRequest
     * @return Result of the AllTypes operation returned by the service.
     * @throws EmptyModeledException
     * @throws ModeledException
     * @sample AmazonProtocolRestJson.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public AllTypesResult allTypes(AllTypesRequest request) {
        request = beforeClientExecution(request);
        return executeAllTypes(request);
    }

    @SdkInternalApi
    final AllTypesResult executeAllTypes(AllTypesRequest allTypesRequest) {

        ExecutionContext executionContext = createExecutionContext(allTypesRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<AllTypesRequest> request = null;
        Response<AllTypesResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new AllTypesRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(allTypesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "AllTypes");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<AllTypesResult>> responseHandler = protocolFactory.createResponseHandler(new JsonOperationMetadata()
                    .withPayloadJson(true).withHasStreamingSuccessResponse(false), new AllTypesResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param deleteOperationRequest
     * @return Result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/DeleteOperation" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public DeleteOperationResult deleteOperation(DeleteOperationRequest request) {
        request = beforeClientExecution(request);
        return executeDeleteOperation(request);
    }

    @SdkInternalApi
    final DeleteOperationResult executeDeleteOperation(DeleteOperationRequest deleteOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(deleteOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<DeleteOperationRequest> request = null;
        Response<DeleteOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new DeleteOperationRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(deleteOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "DeleteOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<DeleteOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new DeleteOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param furtherNestedContainersRequest
     * @return Result of the FurtherNestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJson.FurtherNestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/FurtherNestedContainers"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public FurtherNestedContainersResult furtherNestedContainers(FurtherNestedContainersRequest request) {
        request = beforeClientExecution(request);
        return executeFurtherNestedContainers(request);
    }

    @SdkInternalApi
    final FurtherNestedContainersResult executeFurtherNestedContainers(FurtherNestedContainersRequest furtherNestedContainersRequest) {

        ExecutionContext executionContext = createExecutionContext(furtherNestedContainersRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<FurtherNestedContainersRequest> request = null;
        Response<FurtherNestedContainersResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new FurtherNestedContainersRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(furtherNestedContainersRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "FurtherNestedContainers");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<FurtherNestedContainersResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new FurtherNestedContainersResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param getOperationWithBodyRequest
     * @return Result of the GetOperationWithBody operation returned by the service.
     * @sample AmazonProtocolRestJson.GetOperationWithBody
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/GetOperationWithBody" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public GetOperationWithBodyResult getOperationWithBody(GetOperationWithBodyRequest request) {
        request = beforeClientExecution(request);
        return executeGetOperationWithBody(request);
    }

    @SdkInternalApi
    final GetOperationWithBodyResult executeGetOperationWithBody(GetOperationWithBodyRequest getOperationWithBodyRequest) {

        ExecutionContext executionContext = createExecutionContext(getOperationWithBodyRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<GetOperationWithBodyRequest> request = null;
        Response<GetOperationWithBodyResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new GetOperationWithBodyRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(getOperationWithBodyRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "GetOperationWithBody");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<GetOperationWithBodyResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new GetOperationWithBodyResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param headOperationRequest
     * @return Result of the HeadOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.HeadOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/HeadOperation" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public HeadOperationResult headOperation(HeadOperationRequest request) {
        request = beforeClientExecution(request);
        return executeHeadOperation(request);
    }

    @SdkInternalApi
    final HeadOperationResult executeHeadOperation(HeadOperationRequest headOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(headOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<HeadOperationRequest> request = null;
        Response<HeadOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new HeadOperationRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(headOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "HeadOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<HeadOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new HeadOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param idempotentOperationRequest
     * @return Result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/IdempotentOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public IdempotentOperationResult idempotentOperation(IdempotentOperationRequest request) {
        request = beforeClientExecution(request);
        return executeIdempotentOperation(request);
    }

    @SdkInternalApi
    final IdempotentOperationResult executeIdempotentOperation(IdempotentOperationRequest idempotentOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(idempotentOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<IdempotentOperationRequest> request = null;
        Response<IdempotentOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new IdempotentOperationRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(idempotentOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "IdempotentOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<IdempotentOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new IdempotentOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param jsonValuesOperationRequest
     * @return Result of the JsonValuesOperation operation returned by the service.
     * @throws EmptyModeledException
     * @sample AmazonProtocolRestJson.JsonValuesOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/JsonValuesOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public JsonValuesOperationResult jsonValuesOperation(JsonValuesOperationRequest request) {
        request = beforeClientExecution(request);
        return executeJsonValuesOperation(request);
    }

    @SdkInternalApi
    final JsonValuesOperationResult executeJsonValuesOperation(JsonValuesOperationRequest jsonValuesOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(jsonValuesOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<JsonValuesOperationRequest> request = null;
        Response<JsonValuesOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new JsonValuesOperationRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(jsonValuesOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "JsonValuesOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<JsonValuesOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new JsonValuesOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @return Result of the MapOfStringToListOfStringInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJson.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MapOfStringToListOfStringInQueryParams"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public MapOfStringToListOfStringInQueryParamsResult mapOfStringToListOfStringInQueryParams(MapOfStringToListOfStringInQueryParamsRequest request) {
        request = beforeClientExecution(request);
        return executeMapOfStringToListOfStringInQueryParams(request);
    }

    @SdkInternalApi
    final MapOfStringToListOfStringInQueryParamsResult executeMapOfStringToListOfStringInQueryParams(
            MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest) {

        ExecutionContext executionContext = createExecutionContext(mapOfStringToListOfStringInQueryParamsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<MapOfStringToListOfStringInQueryParamsRequest> request = null;
        Response<MapOfStringToListOfStringInQueryParamsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new MapOfStringToListOfStringInQueryParamsRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(mapOfStringToListOfStringInQueryParamsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MapOfStringToListOfStringInQueryParams");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<MapOfStringToListOfStringInQueryParamsResult>> responseHandler = protocolFactory
                    .createResponseHandler(new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                            new MapOfStringToListOfStringInQueryParamsResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param membersInHeadersRequest
     * @return Result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestJson.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInHeaders" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public MembersInHeadersResult membersInHeaders(MembersInHeadersRequest request) {
        request = beforeClientExecution(request);
        return executeMembersInHeaders(request);
    }

    @SdkInternalApi
    final MembersInHeadersResult executeMembersInHeaders(MembersInHeadersRequest membersInHeadersRequest) {

        ExecutionContext executionContext = createExecutionContext(membersInHeadersRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<MembersInHeadersRequest> request = null;
        Response<MembersInHeadersResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new MembersInHeadersRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(membersInHeadersRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MembersInHeaders");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<MembersInHeadersResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new MembersInHeadersResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param membersInQueryParamsRequest
     * @return Result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJson.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInQueryParams" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public MembersInQueryParamsResult membersInQueryParams(MembersInQueryParamsRequest request) {
        request = beforeClientExecution(request);
        return executeMembersInQueryParams(request);
    }

    @SdkInternalApi
    final MembersInQueryParamsResult executeMembersInQueryParams(MembersInQueryParamsRequest membersInQueryParamsRequest) {

        ExecutionContext executionContext = createExecutionContext(membersInQueryParamsRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<MembersInQueryParamsRequest> request = null;
        Response<MembersInQueryParamsResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new MembersInQueryParamsRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(membersInQueryParamsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MembersInQueryParams");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<MembersInQueryParamsResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new MembersInQueryParamsResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param multiLocationOperationRequest
     * @return Result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MultiLocationOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public MultiLocationOperationResult multiLocationOperation(MultiLocationOperationRequest request) {
        request = beforeClientExecution(request);
        return executeMultiLocationOperation(request);
    }

    @SdkInternalApi
    final MultiLocationOperationResult executeMultiLocationOperation(MultiLocationOperationRequest multiLocationOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(multiLocationOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<MultiLocationOperationRequest> request = null;
        Response<MultiLocationOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new MultiLocationOperationRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(multiLocationOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MultiLocationOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<MultiLocationOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new MultiLocationOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param nestedContainersRequest
     * @return Result of the NestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJson.NestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/NestedContainers" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public NestedContainersResult nestedContainers(NestedContainersRequest request) {
        request = beforeClientExecution(request);
        return executeNestedContainers(request);
    }

    @SdkInternalApi
    final NestedContainersResult executeNestedContainers(NestedContainersRequest nestedContainersRequest) {

        ExecutionContext executionContext = createExecutionContext(nestedContainersRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<NestedContainersRequest> request = null;
        Response<NestedContainersResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new NestedContainersRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(nestedContainersRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "NestedContainers");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<NestedContainersResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false), new NestedContainersResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @return Result of the OperationWithExplicitPayloadBlob operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadBlob"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithExplicitPayloadBlobResult operationWithExplicitPayloadBlob(OperationWithExplicitPayloadBlobRequest request) {
        request = beforeClientExecution(request);
        return executeOperationWithExplicitPayloadBlob(request);
    }

    @SdkInternalApi
    final OperationWithExplicitPayloadBlobResult executeOperationWithExplicitPayloadBlob(
            OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest) {

        ExecutionContext executionContext = createExecutionContext(operationWithExplicitPayloadBlobRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OperationWithExplicitPayloadBlobRequest> request = null;
        Response<OperationWithExplicitPayloadBlobResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OperationWithExplicitPayloadBlobRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(operationWithExplicitPayloadBlobRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithExplicitPayloadBlob");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OperationWithExplicitPayloadBlobResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(false).withHasStreamingSuccessResponse(false),
                    new OperationWithExplicitPayloadBlobResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithExplicitPayloadStructureRequest
     * @return Result of the OperationWithExplicitPayloadStructure operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithExplicitPayloadStructure
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadStructure"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithExplicitPayloadStructureResult operationWithExplicitPayloadStructure(OperationWithExplicitPayloadStructureRequest request) {
        request = beforeClientExecution(request);
        return executeOperationWithExplicitPayloadStructure(request);
    }

    @SdkInternalApi
    final OperationWithExplicitPayloadStructureResult executeOperationWithExplicitPayloadStructure(
            OperationWithExplicitPayloadStructureRequest operationWithExplicitPayloadStructureRequest) {

        ExecutionContext executionContext = createExecutionContext(operationWithExplicitPayloadStructureRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OperationWithExplicitPayloadStructureRequest> request = null;
        Response<OperationWithExplicitPayloadStructureResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OperationWithExplicitPayloadStructureRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(operationWithExplicitPayloadStructureRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithExplicitPayloadStructure");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OperationWithExplicitPayloadStructureResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new OperationWithExplicitPayloadStructureResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithGreedyLabelRequest
     * @return Result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithGreedyLabel"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithGreedyLabelResult operationWithGreedyLabel(OperationWithGreedyLabelRequest request) {
        request = beforeClientExecution(request);
        return executeOperationWithGreedyLabel(request);
    }

    @SdkInternalApi
    final OperationWithGreedyLabelResult executeOperationWithGreedyLabel(OperationWithGreedyLabelRequest operationWithGreedyLabelRequest) {

        ExecutionContext executionContext = createExecutionContext(operationWithGreedyLabelRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OperationWithGreedyLabelRequest> request = null;
        Response<OperationWithGreedyLabelResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OperationWithGreedyLabelRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(operationWithGreedyLabelRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithGreedyLabel");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OperationWithGreedyLabelResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new OperationWithGreedyLabelResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithModeledContentTypeRequest
     * @return Result of the OperationWithModeledContentType operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithModeledContentType"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithModeledContentTypeResult operationWithModeledContentType(OperationWithModeledContentTypeRequest request) {
        request = beforeClientExecution(request);
        return executeOperationWithModeledContentType(request);
    }

    @SdkInternalApi
    final OperationWithModeledContentTypeResult executeOperationWithModeledContentType(
            OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest) {

        ExecutionContext executionContext = createExecutionContext(operationWithModeledContentTypeRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OperationWithModeledContentTypeRequest> request = null;
        Response<OperationWithModeledContentTypeResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OperationWithModeledContentTypeRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(operationWithModeledContentTypeRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithModeledContentType");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OperationWithModeledContentTypeResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new OperationWithModeledContentTypeResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithNoInputOrOutputRequest
     * @return Result of the OperationWithNoInputOrOutput operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithNoInputOrOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithNoInputOrOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithNoInputOrOutputResult operationWithNoInputOrOutput(OperationWithNoInputOrOutputRequest request) {
        request = beforeClientExecution(request);
        return executeOperationWithNoInputOrOutput(request);
    }

    @SdkInternalApi
    final OperationWithNoInputOrOutputResult executeOperationWithNoInputOrOutput(OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest) {

        ExecutionContext executionContext = createExecutionContext(operationWithNoInputOrOutputRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<OperationWithNoInputOrOutputRequest> request = null;
        Response<OperationWithNoInputOrOutputResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new OperationWithNoInputOrOutputRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(operationWithNoInputOrOutputRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithNoInputOrOutput");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<OperationWithNoInputOrOutputResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new OperationWithNoInputOrOutputResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param queryParamWithoutValueRequest
     * @return Result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestJson.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/QueryParamWithoutValue"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public QueryParamWithoutValueResult queryParamWithoutValue(QueryParamWithoutValueRequest request) {
        request = beforeClientExecution(request);
        return executeQueryParamWithoutValue(request);
    }

    @SdkInternalApi
    final QueryParamWithoutValueResult executeQueryParamWithoutValue(QueryParamWithoutValueRequest queryParamWithoutValueRequest) {

        ExecutionContext executionContext = createExecutionContext(queryParamWithoutValueRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<QueryParamWithoutValueRequest> request = null;
        Response<QueryParamWithoutValueResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new QueryParamWithoutValueRequestProtocolMarshaller(protocolFactory).marshall(super.beforeMarshalling(queryParamWithoutValueRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "QueryParamWithoutValue");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<QueryParamWithoutValueResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new QueryParamWithoutValueResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param streamingInputOperationRequest
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResult streamingInputOperation(StreamingInputOperationRequest request) {
        request = beforeClientExecution(request);
        return executeStreamingInputOperation(request);
    }

    @SdkInternalApi
    final StreamingInputOperationResult executeStreamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(streamingInputOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<StreamingInputOperationRequest> request = null;
        Response<StreamingInputOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new StreamingInputOperationRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(streamingInputOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "StreamingInputOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

                request.addHandlerContext(HandlerContextKey.HAS_STREAMING_INPUT, Boolean.TRUE);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<StreamingInputOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new StreamingInputOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param streamingOutputOperationRequest
     * @return Result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingOutputOperationResult streamingOutputOperation(StreamingOutputOperationRequest request) {
        request = beforeClientExecution(request);
        return executeStreamingOutputOperation(request);
    }

    @SdkInternalApi
    final StreamingOutputOperationResult executeStreamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest) {

        ExecutionContext executionContext = createExecutionContext(streamingOutputOperationRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<StreamingOutputOperationRequest> request = null;
        Response<StreamingOutputOperationResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new StreamingOutputOperationRequestProtocolMarshaller(protocolFactory).marshall(super
                        .beforeMarshalling(streamingOutputOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestJson");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "StreamingOutputOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            HttpResponseHandler<AmazonWebServiceResponse<StreamingOutputOperationResult>> responseHandler = protocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(false).withHasStreamingSuccessResponse(true),
                    new StreamingOutputOperationResultJsonUnmarshaller());
            response = invoke(request, responseHandler, executionContext);

            request.addHandlerContext(HandlerContextKey.HAS_STREAMING_OUTPUT, Boolean.TRUE);

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

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());

        return client.execute(request, responseHandler, errorResponseHandler, executionContext);
    }

    @SdkInternalApi
    static SdkJsonProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
