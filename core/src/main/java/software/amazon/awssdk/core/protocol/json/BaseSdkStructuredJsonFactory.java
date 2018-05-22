/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.runtime.http.response.JsonResponseHandler;
import software.amazon.awssdk.core.runtime.http.response.SdkJsonErrorResponseHandler;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Generic implementation of a structured JSON factory that is pluggable for different variants of
 * JSON.
 */
@SdkInternalApi
public abstract class BaseSdkStructuredJsonFactory implements SdkStructuredJsonFactory {

    private final JsonFactory jsonFactory;
    private final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> unmarshallers;

    BaseSdkStructuredJsonFactory(JsonFactory jsonFactory,
                                 Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> unmarshallers) {
        this.jsonFactory = jsonFactory;
        this.unmarshallers = unmarshallers;
    }

    @Override
    public StructuredJsonGenerator createWriter(String contentType) {
        return createWriter(jsonFactory, contentType);
    }

    @Override
    public <T> JsonResponseHandler<T> createResponseHandler(
        JsonOperationMetadata operationMetadata, Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller) {
        return new JsonResponseHandler<>(responseUnmarshaller, unmarshallers, jsonFactory,
                                         operationMetadata.isHasStreamingSuccessResponse(),
                                         operationMetadata.isPayloadJson());
    }

    @Override
    public SdkJsonErrorResponseHandler createErrorResponseHandler(
        final List<SdkJsonErrorUnmarshaller> errorUnmarshallers) {
        return new SdkJsonErrorResponseHandler(errorUnmarshallers,
                                               SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                               jsonFactory);
    }

    protected abstract StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                            String contentType);

}
