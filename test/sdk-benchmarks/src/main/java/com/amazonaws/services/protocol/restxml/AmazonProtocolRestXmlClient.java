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
import com.amazonaws.client.builder.AdvancedConfig;

import com.amazonaws.services.protocol.restxml.AmazonProtocolRestXmlClientBuilder;

import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.protocol.restxml.model.*;

import com.amazonaws.services.protocol.restxml.model.transform.*;

/**
 * Client for accessing RestXmlProtocolTests. All service calls made using this client are blocking, and will not return
 * until the service call completes.
 * <p>
 * 
 */
@ThreadSafe

public class AmazonProtocolRestXmlClient extends AmazonWebServiceClient implements AmazonProtocolRestXml {

    /** Provider for AWS credentials. */
    private final AWSCredentialsProvider awsCredentialsProvider;

    private static final Log log = LogFactory.getLog(AmazonProtocolRestXml.class);

    /** Default signing name for the service. */
    private static final String DEFAULT_SIGNING_NAME = "restxml";

    /** Client configuration factory providing ClientConfigurations tailored to this client */
    protected static final ClientConfigurationFactory configFactory = new ClientConfigurationFactory();

    private final AdvancedConfig advancedConfig;

    /**
     * Map of exception unmarshallers for all modeled exceptions
     */
    private final Map<String, Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallersMap = new HashMap<String, Unmarshaller<AmazonServiceException, Node>>();

    /**
     * List of exception unmarshallers for all modeled exceptions Even though this exceptionUnmarshallers is not used in
     * Clients, this is not removed since this was directly used by Client extended classes. Using this list can cause
     * performance impact.
     */
    protected final List<Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallers = new ArrayList<Unmarshaller<AmazonServiceException, Node>>();

    protected Unmarshaller<AmazonServiceException, Node> defaultUnmarshaller;

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests. A credentials provider chain will be
     * used that searches for credentials in this order:
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
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#defaultClient()}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient() {
        this(DefaultAWSCredentialsProviderChain.getInstance(), configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests. A credentials provider chain will be
     * used that searches for credentials in this order:
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
     *        The client configuration options controlling how this client connects to RestXmlProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified AWS account
     * credentials.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withCredentials(AWSCredentialsProvider)} for example:
     *             {@code AmazonProtocolRestXmlClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(AWSCredentials awsCredentials) {
        this(awsCredentials, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified AWS account
     * credentials and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RestXmlProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.awsCredentialsProvider = new StaticCredentialsProvider(awsCredentials);
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified AWS account
     * credentials provider.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified AWS account
     * credentials provider and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RestXmlProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified AWS account
     * credentials provider, client configuration options, and request metric collector.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to RestXmlProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @param requestMetricCollector
     *        optional request metric collector
     * @deprecated use {@link AmazonProtocolRestXmlClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolRestXmlClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link AmazonProtocolRestXmlClientBuilder#withMetricsCollector(RequestMetricCollector)}
     */
    @Deprecated
    public AmazonProtocolRestXmlClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    public static AmazonProtocolRestXmlClientBuilder builder() {
        return AmazonProtocolRestXmlClientBuilder.standard();
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolRestXmlClient(AwsSyncClientParams clientParams) {
        this(clientParams, false);
    }

    /**
     * Constructs a new client to invoke service methods on RestXmlProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolRestXmlClient(AwsSyncClientParams clientParams, boolean endpointDiscoveryEnabled) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        this.advancedConfig = clientParams.getAdvancedConfig();
        init();
    }

    private void init() {
        if (exceptionUnmarshallersMap.get("EmptyModeledException") == null) {
            exceptionUnmarshallersMap.put("EmptyModeledException", new EmptyModeledExceptionUnmarshaller());
        }
        exceptionUnmarshallers.add(new EmptyModeledExceptionUnmarshaller());
        defaultUnmarshaller = new StandardErrorUnmarshaller(AmazonProtocolRestXmlException.class);
        exceptionUnmarshallers.add(new StandardErrorUnmarshaller(AmazonProtocolRestXmlException.class));

        setServiceNameIntern(DEFAULT_SIGNING_NAME);
        setEndpointPrefix(ENDPOINT_PREFIX);
        // calling this.setEndPoint(...) will also modify the signer accordingly
        this.setEndpoint("https://protocol-restxml.us-east-1.amazonaws.com");
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandler2s.addAll(chainFactory.newRequestHandlerChain("/com/amazonaws/services/protocol/restxml/request.handlers"));
        requestHandler2s.addAll(chainFactory.newRequestHandler2Chain("/com/amazonaws/services/protocol/restxml/request.handler2s"));
        requestHandler2s.addAll(chainFactory.getGlobalHandlers());
    }

    /**
     * @param allTypesRequest
     * @return Result of the AllTypes operation returned by the service.
     * @throws EmptyModeledException
     * @sample AmazonProtocolRestXml.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/AllTypes" target="_top">AWS API
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
                request = new AllTypesRequestMarshaller().marshall(super.beforeMarshalling(allTypesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "AllTypes");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<AllTypesResult> responseHandler = new StaxResponseHandler<AllTypesResult>(new AllTypesResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param deleteOperationRequest
     * @return Result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestXml.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/DeleteOperation" target="_top">AWS API
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
                request = new DeleteOperationRequestMarshaller().marshall(super.beforeMarshalling(deleteOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "DeleteOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<DeleteOperationResult> responseHandler = new StaxResponseHandler<DeleteOperationResult>(
                    new DeleteOperationResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param idempotentOperationRequest
     * @return Result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestXml.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/IdempotentOperation" target="_top">AWS
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
                request = new IdempotentOperationRequestMarshaller().marshall(super.beforeMarshalling(idempotentOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "IdempotentOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<IdempotentOperationResult> responseHandler = new StaxResponseHandler<IdempotentOperationResult>(
                    new IdempotentOperationResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @return Result of the MapOfStringToListOfStringInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestXml.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MapOfStringToListOfStringInQueryParams"
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
                request = new MapOfStringToListOfStringInQueryParamsRequestMarshaller().marshall(super
                        .beforeMarshalling(mapOfStringToListOfStringInQueryParamsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MapOfStringToListOfStringInQueryParams");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<MapOfStringToListOfStringInQueryParamsResult> responseHandler = new StaxResponseHandler<MapOfStringToListOfStringInQueryParamsResult>(
                    new MapOfStringToListOfStringInQueryParamsResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param membersInHeadersRequest
     * @return Result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestXml.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInHeaders" target="_top">AWS API
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
                request = new MembersInHeadersRequestMarshaller().marshall(super.beforeMarshalling(membersInHeadersRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MembersInHeaders");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<MembersInHeadersResult> responseHandler = new StaxResponseHandler<MembersInHeadersResult>(
                    new MembersInHeadersResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param membersInQueryParamsRequest
     * @return Result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestXml.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInQueryParams" target="_top">AWS
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
                request = new MembersInQueryParamsRequestMarshaller().marshall(super.beforeMarshalling(membersInQueryParamsRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MembersInQueryParams");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<MembersInQueryParamsResult> responseHandler = new StaxResponseHandler<MembersInQueryParamsResult>(
                    new MembersInQueryParamsResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param multiLocationOperationRequest
     * @return Result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestXml.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MultiLocationOperation" target="_top">AWS
     *      API Documentation</a>
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
                request = new MultiLocationOperationRequestMarshaller().marshall(super.beforeMarshalling(multiLocationOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "MultiLocationOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<MultiLocationOperationResult> responseHandler = new StaxResponseHandler<MultiLocationOperationResult>(
                    new MultiLocationOperationResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @return Result of the OperationWithExplicitPayloadBlob operation returned by the service.
     * @sample AmazonProtocolRestXml.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithExplicitPayloadBlob"
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
                request = new OperationWithExplicitPayloadBlobRequestMarshaller().marshall(super.beforeMarshalling(operationWithExplicitPayloadBlobRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithExplicitPayloadBlob");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<OperationWithExplicitPayloadBlobResult> responseHandler = new StaxResponseHandler<OperationWithExplicitPayloadBlobResult>(
                    new OperationWithExplicitPayloadBlobResultStaxUnmarshaller(), false, false);

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithGreedyLabelRequest
     * @return Result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestXml.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithGreedyLabel"
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
                request = new OperationWithGreedyLabelRequestMarshaller().marshall(super.beforeMarshalling(operationWithGreedyLabelRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithGreedyLabel");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<OperationWithGreedyLabelResult> responseHandler = new StaxResponseHandler<OperationWithGreedyLabelResult>(
                    new OperationWithGreedyLabelResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param operationWithModeledContentTypeRequest
     * @return Result of the OperationWithModeledContentType operation returned by the service.
     * @sample AmazonProtocolRestXml.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithModeledContentType"
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
                request = new OperationWithModeledContentTypeRequestMarshaller().marshall(super.beforeMarshalling(operationWithModeledContentTypeRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "OperationWithModeledContentType");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<OperationWithModeledContentTypeResult> responseHandler = new StaxResponseHandler<OperationWithModeledContentTypeResult>(
                    new OperationWithModeledContentTypeResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param queryParamWithoutValueRequest
     * @return Result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestXml.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/QueryParamWithoutValue" target="_top">AWS
     *      API Documentation</a>
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
                request = new QueryParamWithoutValueRequestMarshaller().marshall(super.beforeMarshalling(queryParamWithoutValueRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "QueryParamWithoutValue");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<QueryParamWithoutValueResult> responseHandler = new StaxResponseHandler<QueryParamWithoutValueResult>(
                    new QueryParamWithoutValueResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param restXmlTypesRequest
     * @return Result of the RestXmlTypes operation returned by the service.
     * @sample AmazonProtocolRestXml.RestXmlTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/RestXmlTypes" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public RestXmlTypesResult restXmlTypes(RestXmlTypesRequest request) {
        request = beforeClientExecution(request);
        return executeRestXmlTypes(request);
    }

    @SdkInternalApi
    final RestXmlTypesResult executeRestXmlTypes(RestXmlTypesRequest restXmlTypesRequest) {

        ExecutionContext executionContext = createExecutionContext(restXmlTypesRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<RestXmlTypesRequest> request = null;
        Response<RestXmlTypesResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new RestXmlTypesRequestMarshaller().marshall(super.beforeMarshalling(restXmlTypesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "RestXmlTypes");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<RestXmlTypesResult> responseHandler = new StaxResponseHandler<RestXmlTypesResult>(new RestXmlTypesResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param streamingOutputOperationRequest
     * @return Result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestXml.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/StreamingOutputOperation"
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
                request = new StreamingOutputOperationRequestMarshaller().marshall(super.beforeMarshalling(streamingOutputOperationRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolRestXml");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "StreamingOutputOperation");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<StreamingOutputOperationResult> responseHandler = new StaxResponseHandler<StreamingOutputOperationResult>(
                    new StreamingOutputOperationResultStaxUnmarshaller(), true, false);

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

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallersMap, defaultUnmarshaller);

        return client.execute(request, responseHandler, errorResponseHandler, executionContext);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
