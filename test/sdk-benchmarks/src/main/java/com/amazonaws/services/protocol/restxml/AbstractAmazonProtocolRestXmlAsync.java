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
 * Abstract implementation of {@code AmazonProtocolRestXmlAsync}. Convenient method forms pass through to the
 * corresponding overload that takes a request object and an {@code AsyncHandler}, which throws an
 * {@code UnsupportedOperationException}.
 */

public class AbstractAmazonProtocolRestXmlAsync extends AbstractAmazonProtocolRestXml implements AmazonProtocolRestXmlAsync {

    protected AbstractAmazonProtocolRestXmlAsync() {
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest request) {

        return allTypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<AllTypesResult> allTypesAsync(AllTypesRequest request,
            com.amazonaws.handlers.AsyncHandler<AllTypesRequest, AllTypesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest request) {

        return deleteOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<DeleteOperationResult> deleteOperationAsync(DeleteOperationRequest request,
            com.amazonaws.handlers.AsyncHandler<DeleteOperationRequest, DeleteOperationResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest request) {

        return idempotentOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<IdempotentOperationResult> idempotentOperationAsync(IdempotentOperationRequest request,
            com.amazonaws.handlers.AsyncHandler<IdempotentOperationRequest, IdempotentOperationResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest request) {

        return mapOfStringToListOfStringInQueryParamsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MapOfStringToListOfStringInQueryParamsResult> mapOfStringToListOfStringInQueryParamsAsync(
            MapOfStringToListOfStringInQueryParamsRequest request,
            com.amazonaws.handlers.AsyncHandler<MapOfStringToListOfStringInQueryParamsRequest, MapOfStringToListOfStringInQueryParamsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest request) {

        return membersInHeadersAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MembersInHeadersResult> membersInHeadersAsync(MembersInHeadersRequest request,
            com.amazonaws.handlers.AsyncHandler<MembersInHeadersRequest, MembersInHeadersResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest request) {

        return membersInQueryParamsAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MembersInQueryParamsResult> membersInQueryParamsAsync(MembersInQueryParamsRequest request,
            com.amazonaws.handlers.AsyncHandler<MembersInQueryParamsRequest, MembersInQueryParamsResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest request) {

        return multiLocationOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<MultiLocationOperationResult> multiLocationOperationAsync(MultiLocationOperationRequest request,
            com.amazonaws.handlers.AsyncHandler<MultiLocationOperationRequest, MultiLocationOperationResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest request) {

        return operationWithExplicitPayloadBlobAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithExplicitPayloadBlobResult> operationWithExplicitPayloadBlobAsync(
            OperationWithExplicitPayloadBlobRequest request,
            com.amazonaws.handlers.AsyncHandler<OperationWithExplicitPayloadBlobRequest, OperationWithExplicitPayloadBlobResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest request) {

        return operationWithGreedyLabelAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithGreedyLabelResult> operationWithGreedyLabelAsync(OperationWithGreedyLabelRequest request,
            com.amazonaws.handlers.AsyncHandler<OperationWithGreedyLabelRequest, OperationWithGreedyLabelResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest request) {

        return operationWithModeledContentTypeAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<OperationWithModeledContentTypeResult> operationWithModeledContentTypeAsync(
            OperationWithModeledContentTypeRequest request,
            com.amazonaws.handlers.AsyncHandler<OperationWithModeledContentTypeRequest, OperationWithModeledContentTypeResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest request) {

        return queryParamWithoutValueAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<QueryParamWithoutValueResult> queryParamWithoutValueAsync(QueryParamWithoutValueRequest request,
            com.amazonaws.handlers.AsyncHandler<QueryParamWithoutValueRequest, QueryParamWithoutValueResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(RestXmlTypesRequest request) {

        return restXmlTypesAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<RestXmlTypesResult> restXmlTypesAsync(RestXmlTypesRequest request,
            com.amazonaws.handlers.AsyncHandler<RestXmlTypesRequest, RestXmlTypesResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest request) {

        return streamingOutputOperationAsync(request, null);
    }

    @Override
    public java.util.concurrent.Future<StreamingOutputOperationResult> streamingOutputOperationAsync(StreamingOutputOperationRequest request,
            com.amazonaws.handlers.AsyncHandler<StreamingOutputOperationRequest, StreamingOutputOperationResult> asyncHandler) {

        throw new UnsupportedOperationException();
    }

}
