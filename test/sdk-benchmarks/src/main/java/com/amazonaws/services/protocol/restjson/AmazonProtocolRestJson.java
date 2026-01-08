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



import com.amazonaws.*;
import com.amazonaws.regions.*;

import com.amazonaws.services.protocol.restjson.model.*;

/**
 * Interface for accessing JsonProtocolTests.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractAmazonProtocolRestJson} instead.
 * </p>
 */

public interface AmazonProtocolRestJson {

    /**
     * The region metadata service name for computing region endpoints. You can use this value to retrieve metadata
     * (such as supported regions) of the service.
     *
     * @see RegionUtils#getRegionsForService(String)
     */
    String ENDPOINT_PREFIX = "restjson";

    /**
     * Overrides the default endpoint for this client ("https://protocol-restjson.us-east-1.amazonaws.com"). Callers can
     * use this method to control which AWS region they want to work with.
     * <p>
     * Callers can pass in just the endpoint (ex: "protocol-restjson.us-east-1.amazonaws.com") or a full URL, including
     * the protocol (ex: "https://protocol-restjson.us-east-1.amazonaws.com"). If the protocol is not specified here,
     * the default protocol from this client's {@link ClientConfiguration} will be used, which by default is HTTPS.
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
     *        The endpoint (ex: "protocol-restjson.us-east-1.amazonaws.com") or a full URL, including the protocol (ex:
     *        "https://protocol-restjson.us-east-1.amazonaws.com") of the region specific AWS endpoint this client will
     *        communicate with.
     * @deprecated use {@link AwsClientBuilder#setEndpointConfiguration(AwsClientBuilder.EndpointConfiguration)} for
     *             example:
     *             {@code builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));}
     */
    @Deprecated
    void setEndpoint(String endpoint);

    /**
     * An alternative to {@link AmazonProtocolRestJson#setEndpoint(String)}, sets the regional endpoint for this
     * client's service calls. Callers can use this method to control which AWS region they want to work with.
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
     * @throws ModeledException
     * @sample AmazonProtocolRestJson.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    AllTypesResult allTypes(AllTypesRequest allTypesRequest);

    /**
     * @param deleteOperationRequest
     * @return Result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/DeleteOperation" target="_top">AWS API
     *      Documentation</a>
     */
    DeleteOperationResult deleteOperation(DeleteOperationRequest deleteOperationRequest);

    /**
     * @param furtherNestedContainersRequest
     * @return Result of the FurtherNestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJson.FurtherNestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/FurtherNestedContainers"
     *      target="_top">AWS API Documentation</a>
     */
    FurtherNestedContainersResult furtherNestedContainers(FurtherNestedContainersRequest furtherNestedContainersRequest);

    /**
     * @param getOperationWithBodyRequest
     * @return Result of the GetOperationWithBody operation returned by the service.
     * @sample AmazonProtocolRestJson.GetOperationWithBody
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/GetOperationWithBody" target="_top">AWS
     *      API Documentation</a>
     */
    GetOperationWithBodyResult getOperationWithBody(GetOperationWithBodyRequest getOperationWithBodyRequest);

    /**
     * @param headOperationRequest
     * @return Result of the HeadOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.HeadOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/HeadOperation" target="_top">AWS API
     *      Documentation</a>
     */
    HeadOperationResult headOperation(HeadOperationRequest headOperationRequest);

    /**
     * @param idempotentOperationRequest
     * @return Result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/IdempotentOperation" target="_top">AWS
     *      API Documentation</a>
     */
    IdempotentOperationResult idempotentOperation(IdempotentOperationRequest idempotentOperationRequest);

    /**
     * @param jsonValuesOperationRequest
     * @return Result of the JsonValuesOperation operation returned by the service.
     * @throws EmptyModeledException
     * @sample AmazonProtocolRestJson.JsonValuesOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/JsonValuesOperation" target="_top">AWS
     *      API Documentation</a>
     */
    JsonValuesOperationResult jsonValuesOperation(JsonValuesOperationRequest jsonValuesOperationRequest);

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @return Result of the MapOfStringToListOfStringInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJson.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MapOfStringToListOfStringInQueryParams"
     *      target="_top">AWS API Documentation</a>
     */
    MapOfStringToListOfStringInQueryParamsResult mapOfStringToListOfStringInQueryParams(
            MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest);

    /**
     * @param membersInHeadersRequest
     * @return Result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestJson.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInHeaders" target="_top">AWS API
     *      Documentation</a>
     */
    MembersInHeadersResult membersInHeaders(MembersInHeadersRequest membersInHeadersRequest);

    /**
     * @param membersInQueryParamsRequest
     * @return Result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJson.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInQueryParams" target="_top">AWS
     *      API Documentation</a>
     */
    MembersInQueryParamsResult membersInQueryParams(MembersInQueryParamsRequest membersInQueryParamsRequest);

    /**
     * @param multiLocationOperationRequest
     * @return Result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MultiLocationOperation"
     *      target="_top">AWS API Documentation</a>
     */
    MultiLocationOperationResult multiLocationOperation(MultiLocationOperationRequest multiLocationOperationRequest);

    /**
     * @param nestedContainersRequest
     * @return Result of the NestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJson.NestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/NestedContainers" target="_top">AWS API
     *      Documentation</a>
     */
    NestedContainersResult nestedContainers(NestedContainersRequest nestedContainersRequest);

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @return Result of the OperationWithExplicitPayloadBlob operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadBlob"
     *      target="_top">AWS API Documentation</a>
     */
    OperationWithExplicitPayloadBlobResult operationWithExplicitPayloadBlob(OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest);

    /**
     * @param operationWithExplicitPayloadStructureRequest
     * @return Result of the OperationWithExplicitPayloadStructure operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithExplicitPayloadStructure
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadStructure"
     *      target="_top">AWS API Documentation</a>
     */
    OperationWithExplicitPayloadStructureResult operationWithExplicitPayloadStructure(
            OperationWithExplicitPayloadStructureRequest operationWithExplicitPayloadStructureRequest);

    /**
     * @param operationWithGreedyLabelRequest
     * @return Result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithGreedyLabel"
     *      target="_top">AWS API Documentation</a>
     */
    OperationWithGreedyLabelResult operationWithGreedyLabel(OperationWithGreedyLabelRequest operationWithGreedyLabelRequest);

    /**
     * @param operationWithModeledContentTypeRequest
     * @return Result of the OperationWithModeledContentType operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithModeledContentType"
     *      target="_top">AWS API Documentation</a>
     */
    OperationWithModeledContentTypeResult operationWithModeledContentType(OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest);

    /**
     * @param operationWithNoInputOrOutputRequest
     * @return Result of the OperationWithNoInputOrOutput operation returned by the service.
     * @sample AmazonProtocolRestJson.OperationWithNoInputOrOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithNoInputOrOutput"
     *      target="_top">AWS API Documentation</a>
     */
    OperationWithNoInputOrOutputResult operationWithNoInputOrOutput(OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest);

    /**
     * @param queryParamWithoutValueRequest
     * @return Result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestJson.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/QueryParamWithoutValue"
     *      target="_top">AWS API Documentation</a>
     */
    QueryParamWithoutValueResult queryParamWithoutValue(QueryParamWithoutValueRequest queryParamWithoutValueRequest);

    /**
     * @param streamingInputOperationRequest
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    StreamingInputOperationResult streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest);

    /**
     * @param streamingOutputOperationRequest
     * @return Result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestJson.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    StreamingOutputOperationResult streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest);

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
