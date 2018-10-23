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

package software.amazon.awssdk.protocols.json;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonErrorMessageParser;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonErrorResponseHandler;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonErrorCodeParser;

/**
 * Generic implementation of a structured JSON factory that is pluggable for different variants of
 * JSON.
 */
@SdkProtectedApi
public abstract class BaseAwsStructuredJsonFactory implements AwsStructuredJsonFactory {

    private final JsonFactory jsonFactory;

    protected BaseAwsStructuredJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public StructuredJsonGenerator createWriter(String contentType) {
        return createWriter(jsonFactory, contentType);
    }

    @Override
    public AwsJsonErrorResponseHandler createErrorResponseHandler(
        final List<AwsJsonErrorUnmarshaller> errorUnmarshallers, String customErrorCodeFieldName) {
        return new AwsJsonErrorResponseHandler(errorUnmarshallers,
                                               getErrorCodeParser(customErrorCodeFieldName),
                                               AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER,
                                               jsonFactory);
    }

    protected abstract StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                            String contentType);

    protected ErrorCodeParser getErrorCodeParser(String customErrorCodeFieldName) {
        return new JsonErrorCodeParser(customErrorCodeFieldName);
    }
}
