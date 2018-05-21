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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkServiceException;

/**
 * Wrapper object to provide additional metadata about a client and protocol to {@link
 * SdkJsonProtocolFactory}
 */
@NotThreadSafe
@SdkProtectedApi
public class JsonClientMetadata {

    private final List<JsonErrorShapeMetadata> errorsMetadata = new ArrayList<>();

    private String contentTypeOverride;

    private boolean supportsCbor;

    private boolean supportsIon;

    /**
     * Base class is initialized to {@link SdkServiceException} for backwards compatibility.
     */
    private Class<? extends RuntimeException> baseServiceExceptionClass = SdkServiceException.class;

    public JsonClientMetadata addErrorMetadata(JsonErrorShapeMetadata errorShapeMetadata) {
        this.errorsMetadata.add(errorShapeMetadata);
        return this;
    }

    public JsonClientMetadata addAllErrorMetadata(JsonErrorShapeMetadata... errorShapeMetadata) {
        Collections.addAll(errorsMetadata, errorShapeMetadata);
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

    public boolean isSupportsCbor() {
        return supportsCbor;
    }

    public JsonClientMetadata withSupportsCbor(boolean supportsCbor) {
        this.supportsCbor = supportsCbor;
        return this;
    }

    public Class<? extends RuntimeException> getBaseServiceExceptionClass() {
        return baseServiceExceptionClass;
    }

    public boolean isSupportsIon() {
        return supportsIon;
    }

    public JsonClientMetadata withSupportsIon(boolean supportsIon) {
        this.supportsIon = supportsIon;
        return this;
    }

    public JsonClientMetadata withBaseServiceExceptionClass(
            Class<? extends RuntimeException> baseServiceExceptionClass) {
        this.baseServiceExceptionClass = baseServiceExceptionClass;
        return this;
    }
}
