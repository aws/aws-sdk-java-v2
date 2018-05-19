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

import java.util.List;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.runtime.http.response.JsonErrorResponseHandler;

/**
 * Contains various metadata needed to create an appropriate {@link JsonErrorResponseHandler}
 * for the client.
 */
@NotThreadSafe
@SdkProtectedApi
public class JsonErrorResponseMetadata {

    /**
     * Custom error code field name in exceptional responses. Currently only used by glacier. If not
     * set the default error code parser will be used.
     */
    private String customErrorCodeFieldName;

    /**
     * Error shapes scoped to a specific operation.
     */
    private List<JsonErrorShapeMetadata> errorShapes;

    public String getCustomErrorCodeFieldName() {
        return customErrorCodeFieldName;
    }

    public JsonErrorResponseMetadata withCustomErrorCodeFieldName(String errorCodeFieldName) {
        this.customErrorCodeFieldName = errorCodeFieldName;
        return this;
    }

    public List<JsonErrorShapeMetadata> getErrorShapes() {
        return errorShapes;
    }

    public JsonErrorResponseMetadata withErrorShapes(List<JsonErrorShapeMetadata> errorShapes) {
        this.errorShapes = errorShapes;
        return this;
    }
}
