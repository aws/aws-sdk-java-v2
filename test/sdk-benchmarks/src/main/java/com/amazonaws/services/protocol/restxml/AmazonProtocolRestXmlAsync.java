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



import com.amazonaws.services.protocol.restxml.model.*;

/**
 * Interface for accessing RestXmlProtocolTests asynchronously. Each asynchronous method will return a Java Future
 * object representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractAmazonProtocolRestXmlAsync} instead.
 * </p>
 */

public interface AmazonProtocolRestXmlAsync extends AmazonProtocolRestXml {

    /**
     * @param allTypesRequest
     * @return A Java Future containing the result of the AllTypes operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/AllTypes" target="_top">AWS API
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
     * @sample AmazonProtocolRestXmlAsyncHandler.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest allTypesRequest,
            com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler);

    /**
     * @param deleteOperationRequest
     * @return A Java Future containing the result of the DeleteOperation operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/DeleteOperation" target="_top">AWS API
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
     * @sample AmazonProtocolRestXmlAsyncHandler.DeleteOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/DeleteOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest deleteOperationRequest,
            com.amazonaws.handlers.AsyncHandler<DeleteOperationRequest, DeleteOperationResult> asyncHandler);

    /**
     * @param idempotentOperationRequest
     * @return A Java Future containing the result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/IdempotentOperation" target="_top">AWS
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
     * @sample AmazonProtocolRestXmlAsyncHandler.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/IdempotentOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest idempotentOperationRequest,
            com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler);

    /**
     * @param mapOfStringToListOfStringInQueryParamsRequest
     * @return A Java Future containing the result of the MapOfStringToListOfStringInQueryParams operation returned by
     *         the service.
     * @sample AmazonProtocolRestXmlAsync.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MapOfStringToListOfStringInQueryParams"
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
     * @sample AmazonProtocolRestXmlAsyncHandler.MapOfStringToListOfStringInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MapOfStringToListOfStringInQueryParams"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest mapOfStringToListOfStringInQueryParamsRequest,
            com.amazonaws.handlers.AsyncHandler<MapOfStringToListOfStringInQueryParamsRequest, MapOfStringToListOfStringInQueryParamsResult> asyncHandler);

    /**
     * @param membersInHeadersRequest
     * @return A Java Future containing the result of the MembersInHeaders operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInHeaders" target="_top">AWS API
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
     * @sample AmazonProtocolRestXmlAsyncHandler.MembersInHeaders
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInHeaders" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest membersInHeadersRequest,
            com.amazonaws.handlers.AsyncHandler<MembersInHeadersRequest, MembersInHeadersResult> asyncHandler);

    /**
     * @param membersInQueryParamsRequest
     * @return A Java Future containing the result of the MembersInQueryParams operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInQueryParams" target="_top">AWS
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
     * @sample AmazonProtocolRestXmlAsyncHandler.MembersInQueryParams
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MembersInQueryParams" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest membersInQueryParamsRequest,
            com.amazonaws.handlers.AsyncHandler<MembersInQueryParamsRequest, MembersInQueryParamsResult> asyncHandler);

    /**
     * @param multiLocationOperationRequest
     * @return A Java Future containing the result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MultiLocationOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest multiLocationOperationRequest);

    /**
     * @param multiLocationOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the MultiLocationOperation operation returned by the service.
     * @sample AmazonProtocolRestXmlAsyncHandler.MultiLocationOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/MultiLocationOperation" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest multiLocationOperationRequest,
            com.amazonaws.handlers.AsyncHandler<MultiLocationOperationRequest, MultiLocationOperationResult> asyncHandler);

    /**
     * @param operationWithExplicitPayloadBlobRequest
     * @return A Java Future containing the result of the OperationWithExplicitPayloadBlob operation returned by the
     *         service.
     * @sample AmazonProtocolRestXmlAsync.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithExplicitPayloadBlob"
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
     * @sample AmazonProtocolRestXmlAsyncHandler.OperationWithExplicitPayloadBlob
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithExplicitPayloadBlob"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest operationWithExplicitPayloadBlobRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithExplicitPayloadBlobRequest, OperationWithExplicitPayloadBlobResult> asyncHandler);

    /**
     * @param operationWithGreedyLabelRequest
     * @return A Java Future containing the result of the OperationWithGreedyLabel operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithGreedyLabel"
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
     * @sample AmazonProtocolRestXmlAsyncHandler.OperationWithGreedyLabel
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithGreedyLabel"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest operationWithGreedyLabelRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithGreedyLabelRequest, OperationWithGreedyLabelResult> asyncHandler);

    /**
     * @param operationWithModeledContentTypeRequest
     * @return A Java Future containing the result of the OperationWithModeledContentType operation returned by the
     *         service.
     * @sample AmazonProtocolRestXmlAsync.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithModeledContentType"
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
     * @sample AmazonProtocolRestXmlAsyncHandler.OperationWithModeledContentType
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithModeledContentType"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest operationWithModeledContentTypeRequest,
            com.amazonaws.handlers.AsyncHandler<OperationWithModeledContentTypeRequest, OperationWithModeledContentTypeResult> asyncHandler);

    /**
     * @param queryParamWithoutValueRequest
     * @return A Java Future containing the result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/QueryParamWithoutValue" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest queryParamWithoutValueRequest);

    /**
     * @param queryParamWithoutValueRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the QueryParamWithoutValue operation returned by the service.
     * @sample AmazonProtocolRestXmlAsyncHandler.QueryParamWithoutValue
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/QueryParamWithoutValue" target="_top">AWS
     *      API Documentation</a>
     */
    java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest queryParamWithoutValueRequest,
            com.amazonaws.handlers.AsyncHandler<QueryParamWithoutValueRequest, QueryParamWithoutValueResult> asyncHandler);

    /**
     * @param restXmlTypesRequest
     * @return A Java Future containing the result of the RestXmlTypes operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.RestXmlTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/RestXmlTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(RestXmlTypesRequest restXmlTypesRequest);

    /**
     * @param restXmlTypesRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the RestXmlTypes operation returned by the service.
     * @sample AmazonProtocolRestXmlAsyncHandler.RestXmlTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/RestXmlTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(RestXmlTypesRequest restXmlTypesRequest,
            com.amazonaws.handlers.AsyncHandler<RestXmlTypesRequest, RestXmlTypesResult> asyncHandler);

    /**
     * @param streamingOutputOperationRequest
     * @return A Java Future containing the result of the StreamingOutputOperation operation returned by the service.
     * @sample AmazonProtocolRestXmlAsync.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/StreamingOutputOperation"
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
     * @sample AmazonProtocolRestXmlAsyncHandler.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest streamingOutputOperationRequest,
            com.amazonaws.handlers.AsyncHandler<StreamingOutputOperationRequest, StreamingOutputOperationResult> asyncHandler);

}
