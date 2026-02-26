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

package software.amazon.awssdk.codegen.model.config.customization;

/**
 * Smithy-native equivalent of {@link MetadataConfig}. Uses Smithy protocol
 * trait ShapeId instead of C2J protocol string.
 */
public class SmithyMetadataConfig {

    /**
     * Smithy protocol trait ShapeId
     * (e.g., "aws.protocols#restJson1" instead of "rest-json").
     */
    private String protocol;

    /**
     * Custom Content-Type header value.
     */
    private String contentType;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
