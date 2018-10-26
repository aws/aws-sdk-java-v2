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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Wrapper object to provide additional metadata about a client and protocol to a protocol factory.
 */
@NotThreadSafe
@SdkProtectedApi
// TODO can we collapse this? or change to traditional builder
public class JsonClientMetadata {

    private final List<JsonErrorShapeMetadata> errorsMetadata = new ArrayList<>();

    private String contentTypeOverride;

    private Class<? extends RuntimeException> baseServiceExceptionClass;

    public JsonClientMetadata addErrorMetadata(JsonErrorShapeMetadata errorShapeMetadata) {
        this.errorsMetadata.add(errorShapeMetadata);
        return this;
    }

    public List<JsonErrorShapeMetadata> getErrorShapeMetadata() {
        return errorsMetadata;
    }

    public String getContentTypeOverride() {
        return contentTypeOverride;
    }

    public JsonClientMetadata withContentTypeOverride(String contentType) {
        this.contentTypeOverride = contentType;
        return this;
    }

    public Class<? extends RuntimeException> getBaseServiceExceptionClass() {
        return baseServiceExceptionClass;
    }

    public JsonClientMetadata withBaseServiceExceptionClass(
            Class<? extends RuntimeException> baseServiceExceptionClass) {
        this.baseServiceExceptionClass = baseServiceExceptionClass;
        return this;
    }
}
