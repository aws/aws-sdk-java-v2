/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonErrorCodeParser;

/**
 * Generic implementation of a structured JSON factory that is pluggable for different variants of
 * JSON.
 */
@SdkProtectedApi
public abstract class BaseAwsStructuredJsonFactory implements StructuredJsonFactory {

    private final JsonFactory jsonFactory;

    protected BaseAwsStructuredJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public StructuredJsonGenerator createWriter(String contentType) {
        return createWriter(jsonFactory, contentType);
    }

    protected abstract StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                            String contentType);

    @Override
    public ErrorCodeParser getErrorCodeParser(String customErrorCodeFieldName) {
        return new JsonErrorCodeParser(customErrorCodeFieldName);
    }
}
