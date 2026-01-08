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
package com.amazonaws.services.protocol.ec2;

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

import com.amazonaws.services.protocol.ec2.AmazonProtocolEc2ClientBuilder;

import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.protocol.ec2.model.*;

import com.amazonaws.services.protocol.ec2.model.transform.*;

/**
 * Client for accessing EC2ProtocolTests. All service calls made using this client are blocking, and will not return
 * until the service call completes.
 * <p>
 * 
 */
@ThreadSafe

public class AmazonProtocolEc2Client extends AmazonWebServiceClient implements AmazonProtocolEc2 {

    /** Provider for AWS credentials. */
    private final AWSCredentialsProvider awsCredentialsProvider;

    private static final Log log = LogFactory.getLog(AmazonProtocolEc2.class);

    /** Default signing name for the service. */
    private static final String DEFAULT_SIGNING_NAME = "ec2";

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
     * Constructs a new client to invoke service methods on EC2ProtocolTests. A credentials provider chain will be used
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
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#defaultClient()}
     */
    @Deprecated
    public AmazonProtocolEc2Client() {
        this(DefaultAWSCredentialsProviderChain.getInstance(), configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests. A credentials provider chain will be used
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
     *        The client configuration options controlling how this client connects to EC2ProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     *
     * @see DefaultAWSCredentialsProviderChain
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolEc2Client(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified AWS account
     * credentials.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withCredentials(AWSCredentialsProvider)} for example:
     *             {@code AmazonProtocolEc2ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();}
     */
    @Deprecated
    public AmazonProtocolEc2Client(AWSCredentials awsCredentials) {
        this(awsCredentials, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified AWS account credentials
     * and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentials
     *        The AWS credentials (access key ID and secret key) to use when authenticating with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to EC2ProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolEc2ClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolEc2Client(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
        this.awsCredentialsProvider = new StaticCredentialsProvider(awsCredentials);
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified AWS account credentials
     * provider.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withCredentials(AWSCredentialsProvider)}
     */
    @Deprecated
    public AmazonProtocolEc2Client(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified AWS account credentials
     * provider and client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to EC2ProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolEc2ClientBuilder#withClientConfiguration(ClientConfiguration)}
     */
    @Deprecated
    public AmazonProtocolEc2Client(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified AWS account credentials
     * provider, client configuration options, and request metric collector.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider
     *        The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration
     *        The client configuration options controlling how this client connects to EC2ProtocolTests (ex: proxy
     *        settings, retry counts, etc.).
     * @param requestMetricCollector
     *        optional request metric collector
     * @deprecated use {@link AmazonProtocolEc2ClientBuilder#withCredentials(AWSCredentialsProvider)} and
     *             {@link AmazonProtocolEc2ClientBuilder#withClientConfiguration(ClientConfiguration)} and
     *             {@link AmazonProtocolEc2ClientBuilder#withMetricsCollector(RequestMetricCollector)}
     */
    @Deprecated
    public AmazonProtocolEc2Client(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    public static AmazonProtocolEc2ClientBuilder builder() {
        return AmazonProtocolEc2ClientBuilder.standard();
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolEc2Client(AwsSyncClientParams clientParams) {
        this(clientParams, false);
    }

    /**
     * Constructs a new client to invoke service methods on EC2ProtocolTests using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    AmazonProtocolEc2Client(AwsSyncClientParams clientParams, boolean endpointDiscoveryEnabled) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        this.advancedConfig = clientParams.getAdvancedConfig();
        init();
    }

    private void init() {
        defaultUnmarshaller = new LegacyErrorUnmarshaller(AmazonProtocolEc2Exception.class);
        exceptionUnmarshallers.add(new LegacyErrorUnmarshaller(AmazonProtocolEc2Exception.class));

        setServiceNameIntern(DEFAULT_SIGNING_NAME);
        setEndpointPrefix(ENDPOINT_PREFIX);
        // calling this.setEndPoint(...) will also modify the signer accordingly
        this.setEndpoint("https://protocol-ec2.us-east-1.amazonaws.com");
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandler2s.addAll(chainFactory.newRequestHandlerChain("/com/amazonaws/services/protocol/ec2/request.handlers"));
        requestHandler2s.addAll(chainFactory.newRequestHandler2Chain("/com/amazonaws/services/protocol/ec2/request.handler2s"));
        requestHandler2s.addAll(chainFactory.getGlobalHandlers());
    }

    /**
     * @param allTypesRequest
     * @return Result of the AllTypes operation returned by the service.
     * @sample AmazonProtocolEc2.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/ec2-2016-03-11/AllTypes" target="_top">AWS API
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
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolEc2");
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
     * @param ec2TypesRequest
     * @return Result of the Ec2Types operation returned by the service.
     * @sample AmazonProtocolEc2.Ec2Types
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/ec2-2016-03-11/Ec2Types" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public Ec2TypesResult ec2Types(Ec2TypesRequest request) {
        request = beforeClientExecution(request);
        return executeEc2Types(request);
    }

    @SdkInternalApi
    final Ec2TypesResult executeEc2Types(Ec2TypesRequest ec2TypesRequest) {

        ExecutionContext executionContext = createExecutionContext(ec2TypesRequest);
        AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(Field.ClientExecuteTime);
        Request<Ec2TypesRequest> request = null;
        Response<Ec2TypesResult> response = null;

        try {
            awsRequestMetrics.startEvent(Field.RequestMarshallTime);
            try {
                request = new Ec2TypesRequestMarshaller().marshall(super.beforeMarshalling(ec2TypesRequest));
                // Binds the request metrics to the current request.
                request.setAWSRequestMetrics(awsRequestMetrics);
                request.addHandlerContext(HandlerContextKey.CLIENT_ENDPOINT, endpoint);
                request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
                request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolEc2");
                request.addHandlerContext(HandlerContextKey.OPERATION_NAME, "Ec2Types");
                request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, advancedConfig);

            } finally {
                awsRequestMetrics.endEvent(Field.RequestMarshallTime);
            }

            StaxResponseHandler<Ec2TypesResult> responseHandler = new StaxResponseHandler<Ec2TypesResult>(new Ec2TypesResultStaxUnmarshaller());

            response = invoke(request, responseHandler, executionContext);

            return response.getAwsResponse();

        } finally {

            endClientExecution(awsRequestMetrics, request, response);
        }
    }

    /**
     * @param idempotentOperationRequest
     * @return Result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolEc2.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/ec2-2016-03-11/IdempotentOperation" target="_top">AWS API
     *      Documentation</a>
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
                request.addHandlerContext(HandlerContextKey.SERVICE_ID, "ProtocolEc2");
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
