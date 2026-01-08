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



import com.amazonaws.services.protocol.restjson.model.*;

/**
 * Interface for accessing JsonProtocolTests asynchronously. Each asynchronous method will return a Java Future object
 * representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractAmazonProtocolRestJsonAsync} instead.
 * </p>
 */

public interface AmazonProtocolRestJsonAsync extends AmazonProtocolRestJson {

    /**
     * @param allTypesRequest
     * @return A Java Future containing the result of the AllTypes operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest allTypesRequest);

    /**
     * @param allTypesRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the AllTypes operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest allTypesRequest,
            com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler);

    /**
     * @param deleteOperationRequest
     * @return A Java Future containing the result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/DeleteOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest deleteOperationRequest);

    /**
     * @param deleteOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/DeleteOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest deleteOperationRequest,
            com.amazonaws.handlers.AsyncHandler<DeleteOperationRequest, DeleteOperationResult> asyncHandler);

    /**
     * @param furtherNestedContainersRequest
     * @return A Java Future containing the result of the FurtherNestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.FurtherNestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/FurtherNestedContainers"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<FurtherNestedContainersResult> furtherNestedContainersAsync(FurtherNestedContainersRequest furtherNestedContainersRequest);

    /**
     * @param furtherNestedContainersRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the FurtherNestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.FurtherNestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/FurtherNestedContainers"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<FurtherNestedContainersResult> furtherNestedContainersAsync(FurtherNestedContainersRequest furtherNestedContainersRequest,
            com.amazonaws.handlers.AsyncHandler<FurtherNestedContainersRequest, FurtherNestedContainersResult> asyncHandler);

    /**
     * @param getOperationWithBodyRequest
     * @return A Java Future containing the result of the GetOperationWithBody operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.GetOperationWithBody
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/GetOperationWithBody" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<GetOperationWithBodyResult> getOperationWithBodyAsync(GetOperationWithBodyRequest getOperationWithBodyRequest);

    /**
     * @param getOperationWithBodyRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the GetOperationWithBody operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.GetOperationWithBody
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/GetOperationWithBody" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<GetOperationWithBodyResult> getOperationWithBodyAsync(GetOperationWithBodyRequest getOperationWithBodyRequest,
            com.amazonaws.handlers.AsyncHandler<GetOperationWithBodyRequest, GetOperationWithBodyResult> asyncHandler);

    /**
     * @param headOperationRequest
     * @return A Java Future containing the result of the HeadOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.HeadOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/HeadOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<HeadOperationResult> headOperationAsync(HeadOperationRequest headOperationRequest);

    /**
     * @param headOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the HeadOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.HeadOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/HeadOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<HeadOperationResult> headOperationAsync(HeadOperationRequest headOperationRequest,
            com.amazonaws.handlers.AsyncHandler<HeadOperationRequest, HeadOperationResult> asyncHandler);

    /**
     * @param idempotentOperationRequest
     * @return A Java Future containing the result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/IdempotentOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest idempotentOperationRequest);

    /**
     * @param idempotentOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/IdempotentOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest idempotentOperationRequest,
            com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler);

    /**
     * @param jsonValuesOperationRequest
     * @return A Java Future containing the result of the JsonValuesOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.JsonValuesOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/JsonValuesOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<JsonValuesOperationResult> jsonValuesOperationAsync(JsonValuesOperationRequest jsonValuesOperationRequest);

    /**
     * @param jsonValuesOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the JsonValuesOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.JsonValuesOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/JsonValuesOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<JsonValuesOperationResult> jsonValuesOperationAsync(JsonValuesOperationRequest jsonValuesOperationRequest,
            com.amazonaws.handlers.AsyncHandler<JsonValuesOperationRequest, JsonValuesOperationResult> asyncHandler);

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @return A Java Future containing the result of the MapOfStringToListOfStringInQueryParams operation returned by
     *         the service.
     * @sample AmazonProtocolRestJsonAsync.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MapOfStringToListOfStringInQueryParams"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest);

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the MapOfStringToListOfStringInQueryParams operation returned by
     *         the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MapOfStringToListOfStringInQueryParams"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest,
            com.amazonaws.handlers.AsyncHandler<MapOfStringToListOfStringInQueryParamsRequest, MapOfStringToListOfStringInQueryParamsResult> asyncHandler);

    /**
     * @param membersInHeadersRequest
     * @return A Java Future containing the result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInHeaders" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest membersInHeadersRequest);

    /**
     * @param membersInHeadersRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInHeaders" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest membersInHeadersRequest,
            com.amazonaws.handlers.AsyncHandler<MembersInHeadersRequest, MembersInHeadersResult> asyncHandler);

    /**
     * @param membersInQueryParamsRequest
     * @return A Java Future containing the result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInQueryParams" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest membersInQueryParamsRequest);

    /**
     * @param membersInQueryParamsRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MembersInQueryParams" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest membersInQueryParamsRequest,
            com.amazonaws.handlers.AsyncHandler<MembersInQueryParamsRequest, MembersInQueryParamsResult> asyncHandler);

    /**
     * @param multiLocationOperationRequest
     * @return A Java Future containing the result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MultiLocationOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest multiLocationOperationRequest);

    /**
     * @param multiLocationOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/MultiLocationOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest multiLocationOperationRequest,
            com.amazonaws.handlers.AsyncHandler<MultiLocationOperationRequest, MultiLocationOperationResult> asyncHandler);

    /**
     * @param nestedContainersRequest
     * @return A Java Future containing the result of the NestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.NestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/NestedContainers" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<NestedContainersResult> nestedContainersAsync(NestedContainersRequest nestedContainersRequest);

    /**
     * @param nestedContainersRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the NestedContainers operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.NestedContainers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/NestedContainers" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<NestedContainersResult> nestedContainersAsync(NestedContainersRequest nestedContainersRequest,
            com.amazonaws.handlers.AsyncHandler<NestedContainersRequest, NestedContainersResult> asyncHandler);

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @return A Java Future containing the result of the OperationWithExplicitPayloadBlob operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsync.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadBlob"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest);

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OperationWithExplicitPayloadBlob operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsyncHandler.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadBlob"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithExplicitPayloadBlobRequest, OperationWithExplicitPayloadBlobResult> asyncHandler);

    /**
     * @param operationWithExplicitPayloadStructureRequest
     * @return A Java Future containing the result of the OperationWithExplicitPayloadStructure operation returned by
     *         the service.
     * @sample AmazonProtocolRestJsonAsync.OperationWithExplicitPayloadStructure
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadStructure"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithExplicitPayloadStructureResult> operationWithExplicitPayloadStructureAsync(
            OperationWithExplicitPayloadStructureRequest operationWithExplicitPayloadStructureRequest);

    /**
     * @param operationWithExplicitPayloadStructureRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OperationWithExplicitPayloadStructure operation returned by
     *         the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.OperationWithExplicitPayloadStructure
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadStructure"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithExplicitPayloadStructureResult> operationWithExplicitPayloadStructureAsync(
            OperationWithExplicitPayloadStructureRequest operationWithExplicitPayloadStructureRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithExplicitPayloadStructureRequest, OperationWithExplicitPayloadStructureResult> asyncHandler);

    /**
     * @param operationWithGreedyLabelRequest
     * @return A Java Future containing the result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithGreedyLabel"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest operationWithGreedyLabelRequest);

    /**
     * @param operationWithGreedyLabelRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithGreedyLabel"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest operationWithGreedyLabelRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithGreedyLabelRequest, OperationWithGreedyLabelResult> asyncHandler);

    /**
     * @param operationWithModeledContentTypeRequest
     * @return A Java Future containing the result of the OperationWithModeledContentType operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsync.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithModeledContentType"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest);

    /**
     * @param operationWithModeledContentTypeRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OperationWithModeledContentType operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsyncHandler.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithModeledContentType"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithModeledContentTypeRequest, OperationWithModeledContentTypeResult> asyncHandler);

    /**
     * @param operationWithNoInputOrOutputRequest
     * @return A Java Future containing the result of the OperationWithNoInputOrOutput operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsync.OperationWithNoInputOrOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithNoInputOrOutput"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithNoInputOrOutputResult> operationWithNoInputOrOutputAsync(
            OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest);

    /**
     * @param operationWithNoInputOrOutputRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the OperationWithNoInputOrOutput operation returned by the
     *         service.
     * @sample AmazonProtocolRestJsonAsyncHandler.OperationWithNoInputOrOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithNoInputOrOutput"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithNoInputOrOutputResult> operationWithNoInputOrOutputAsync(
            OperationWithNoInputOrOutputRequest operationWithNoInputOrOutputRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithNoInputOrOutputRequest, OperationWithNoInputOrOutputResult> asyncHandler);

    /**
     * @param queryParamWithoutValueRequest
     * @return A Java Future containing the result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/QueryParamWithoutValue"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest queryParamWithoutValueRequest);

    /**
     * @param queryParamWithoutValueRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/QueryParamWithoutValue"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest queryParamWithoutValueRequest,
            com.amazonaws.handlers.AsyncHandler<QueryParamWithoutValueRequest, QueryParamWithoutValueResult> asyncHandler);

    /**
     * @param streamingInputOperationRequest
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<StreamingInputOperationResult> streamingInputOperationAsync(StreamingInputOperationRequest streamingInputOperationRequest);

    /**
     * @param streamingInputOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<StreamingInputOperationResult> streamingInputOperationAsync(StreamingInputOperationRequest streamingInputOperationRequest,
            com.amazonaws.handlers.AsyncHandler<StreamingInputOperationRequest, StreamingInputOperationResult> asyncHandler);

    /**
     * @param streamingOutputOperationRequest
     * @return A Java Future containing the result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsync.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest streamingOutputOperationRequest);

    /**
     * @param streamingOutputOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestJsonAsyncHandler.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest streamingOutputOperationRequest,
            com.amazonaws.handlers.AsyncHandler<StreamingOutputOperationRequest, StreamingOutputOperationResult> asyncHandler);

}
