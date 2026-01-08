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
package com.amazonaws.services.protocol.query;



import com.amazonaws.*;
import com.amazonaws.regions.*;

import com.amazonaws.services.protocol.query.model.*;

/**
 * Interface for accessing QueryProtocolTests.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractAmazonProtocolQuery} instead.
 * </p>
 */

public interface AmazonProtocolQuery {

    /**
     * The region metadata service name for computing region endpoints. You can use this value to retrieve metadata
     * (such as supported regions) of the service.
     *
     * @see RegionUtils#getRegionsForService(String)
     */
    String ENDPOINT_PREFIX = "query";

    /**
     * Overrides the default endpoint for this client ("https://protocol-query.us-east-1.amazonaws.com"). Callers can
     * use this method to control which AWS region they want to work with.
     * <p>
     * Callers can pass in just the endpoint (ex: "protocol-query.us-east-1.amazonaws.com") or a full URL, including the
     * protocol (ex: "https://protocol-query.us-east-1.amazonaws.com"). If the protocol is not specified here, the
     * default protocol from this client's {@link ClientConfiguration} will be used, which by default is HTTPS.
     * <p>
     * For more information on using AWS regions with the AWS SDK for Java, and a complete list of all available
     * endpoints for all AWS services, see: <a href=
     * "https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html#region-selection-choose-endpoint"
     * > https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html#region-selection-
     * choose-endpoint</a>
     * <p>
     * <b>This method is not threadsafe. An endpoint should be configured when the client is created and before any
     * service requests are made. Changing it afterwards creates inevitable race conditions for any service requests in
     * transit or retrying.</b>
     *
     * @param endpoint
     *        The endpoint (ex: "protocol-query.us-east-1.amazonaws.com") or a full URL, including the protocol (ex:
     *        "https://protocol-query.us-east-1.amazonaws.com") of the region specific AWS endpoint this client will
     *        communicate with.
     * @deprecated use {@link AwsClientBuilder#setEndpointConfiguration(AwsClientBuilder.EndpointConfiguration)} for
     *             example:
     *             {@code builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));}
     */
    @Deprecated
    void setEndpoint(String endpoint);

    /**
     * An alternative to {@link AmazonProtocolQuery#setEndpoint(String)}, sets the regional endpoint for this client's
     * service calls. Callers can use this method to control which AWS region they want to work with.
     * <p>
     * By default, all service endpoints in all regions use the https protocol. To use http instead, specify it in the
     * {@link ClientConfiguration} supplied at construction.
     * <p>
     * <b>This method is not threadsafe. A region should be configured when the client is created and before any service
     * requests are made. Changing it afterwards creates inevitable race conditions for any service requests in transit
     * or retrying.</b>
     *
     * @param region
     *        The region this client will communicate with. See {@link Region#getRegion(Regions)}
     *        for accessing a given region. Must not be null and must be a region where the service is available.
     *
     * @see Region#getRegion(Regions)
     * @see Region#createClient(Class, com.amazonaws.auth.AWSCredentialsProvider, ClientConfiguration)
     * @see Region#isServiceSupported(String)
     * @deprecated use {@link AwsClientBuilder#setRegion(String)}
     */
    @Deprecated
    void setRegion(Region region);

    /**
     * @param allTypesRequest
     * @return Result of the AllTypes operation returned by the service.
     * @throws EmptyModeledException
     * @sample AmazonProtocolQuery.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    AllTypesResult allTypes(AllTypesRequest allTypesRequest);

    /**
     * @param idempotentOperationRequest
     * @return Result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolQuery.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/IdempotentOperation" target="_top">AWS API
     *      Documentation</a>
     */
    IdempotentOperationResult idempotentOperation(IdempotentOperationRequest idempotentOperationRequest);

    /**
     * @param queryTypesRequest
     * @return Result of the QueryTypes operation returned by the service.
     * @sample AmazonProtocolQuery.QueryTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/QueryTypes" target="_top">AWS API
     *      Documentation</a>
     */
    QueryTypesResult queryTypes(QueryTypesRequest queryTypesRequest);

    /**
     * Shuts down this client object, releasing any resources that might be held open. This is an optional method, and
     * callers are not expected to call it, but can if they want to explicitly release any open resources. Once a client
     * has been shutdown, it should not be used to make any more requests.
     */
    void shutdown();

    /**
     * Returns additional metadata for a previously executed successful request, typically used for debugging issues
     * where a service isn't acting as expected. This data isn't considered part of the result data returned by an
     * operation, so it's available through this separate, diagnostic interface.
     * <p>
     * Response metadata is only cached for a limited period of time, so if you need to access this extra diagnostic
     * information for an executed request, you should use this method to retrieve it as soon as possible after
     * executing a request.
     *
     * @param request
     *        The originally executed request.
     *
     * @return The response metadata for the specified request, or null if none is available.
     */
    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request);

}
