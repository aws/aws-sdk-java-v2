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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.runtime.http.response.SdkJsonErrorResponseHandler;

/**
 * Common interface for creating generators (writers) and protocol handlers for JSON like protocols.
 * Current implementations include {@link SdkStructuredPlainJsonFactory} and {@link
 * SdkStructuredCborFactory}
 */
@SdkProtectedApi
public interface SdkStructuredJsonFactory extends StructuredJsonFactory {

    /**
     * Returns the error response handler for handling a error response.
     *
     * @param errorUnmarshallers Response unmarshallers to unamrshall the error responses.
     */
    SdkJsonErrorResponseHandler createErrorResponseHandler(
        List<SdkJsonErrorUnmarshaller> errorUnmarshallers);
}
