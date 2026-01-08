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
import com.amazonaws.*;

/**
 * Abstract implementation of {@code AmazonProtocolRestXml}. Convenient method forms pass through to the corresponding
 * overload that takes a request object, which throws an {@code UnsupportedOperationException}.
 */

public class AbstractAmazonProtocolRestXml implements AmazonProtocolRestXml {

    protected AbstractAmazonProtocolRestXml() {
    }

    @Override
    public void setEndpoint(String endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRegion(com.amazonaws.regions.Region region) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AllTypesResult allTypes(AllTypesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteOperationResult deleteOperation(DeleteOperationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdempotentOperationResult idempotentOperation(IdempotentOperationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapOfStringToListOfStringInQueryParamsResult mapOfStringToListOfStringInQueryParams(MapOfStringToListOfStringInQueryParamsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MembersInHeadersResult membersInHeaders(MembersInHeadersRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MembersInQueryParamsResult membersInQueryParams(MembersInQueryParamsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultiLocationOperationResult multiLocationOperation(MultiLocationOperationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationWithExplicitPayloadBlobResult operationWithExplicitPayloadBlob(OperationWithExplicitPayloadBlobRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationWithGreedyLabelResult operationWithGreedyLabel(OperationWithGreedyLabelRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationWithModeledContentTypeResult operationWithModeledContentType(OperationWithModeledContentTypeRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryParamWithoutValueResult queryParamWithoutValue(QueryParamWithoutValueRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RestXmlTypesResult restXmlTypes(RestXmlTypesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StreamingOutputOperationResult streamingOutputOperation(StreamingOutputOperationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        throw new UnsupportedOperationException();
    }

}
