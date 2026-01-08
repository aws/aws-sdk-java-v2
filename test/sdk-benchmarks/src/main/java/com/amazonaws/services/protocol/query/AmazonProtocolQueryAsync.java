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



import com.amazonaws.services.protocol.query.model.*;

/**
 * Interface for accessing QueryProtocolTests asynchronously. Each asynchronous method will return a Java Future object
 * representing the asynchronous operation; overloads which accept an {@code AsyncHandler} can be used to receive
 * notification when an asynchronous operation completes.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractAmazonProtocolQueryAsync} instead.
 * </p>
 */

public interface AmazonProtocolQueryAsync extends AmazonProtocolQuery {

    /**
     * @param allTypesRequest
     * @return A Java Future containing the result of the AllTypes operation returned by the service.
     * @sample AmazonProtocolQueryAsync.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/AllTypes" target="_top">AWS API
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
     * @sample AmazonProtocolQueryAsyncHandler.AllTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/AllTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest allTypesRequest,
            com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler);

    /**
     * @param idempotentOperationRequest
     * @return A Java Future containing the result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolQueryAsync.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/IdempotentOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest idempotentOperationRequest);

    /**
     * @param idempotentOperationRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the IdempotentOperation operation returned by the service.
     * @sample AmazonProtocolQueryAsyncHandler.IdempotentOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/IdempotentOperation" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest idempotentOperationRequest,
            com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler);

    /**
     * @param queryTypesRequest
     * @return A Java Future containing the result of the QueryTypes operation returned by the service.
     * @sample AmazonProtocolQueryAsync.QueryTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/QueryTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<QueryTypesResult> queryTypesAsync(QueryTypesRequest queryTypesRequest);

    /**
     * @param queryTypesRequest
     * @param asyncHandler
     *        Asynchronous callback handler for events in the lifecycle of the request. Users can provide an
     *        implementation of the callback methods in this interface to receive notification of successful or
     *        unsuccessful completion of the operation.
     * @return A Java Future containing the result of the QueryTypes operation returned by the service.
     * @sample AmazonProtocolQueryAsyncHandler.QueryTypes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-2016-03-11/QueryTypes" target="_top">AWS API
     *      Documentation</a>
     */
    java.util.concurrent.Future<QueryTypesResult> queryTypesAsync(QueryTypesRequest queryTypesRequest,
            com.amazonaws.handlers.AsyncHandler<QueryTypesRequest, QueryTypesResult> asyncHandler);

}
