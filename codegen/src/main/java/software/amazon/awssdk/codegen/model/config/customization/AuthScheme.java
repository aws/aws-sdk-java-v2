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

public class AuthScheme {
    private String name;
    private Boolean doubleUrlEncode;
    private Boolean normalizePath;
    private Boolean payloadSigningEnabled;
    private Boolean chunkEncodingEnabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDoubleUrlEncode() {
        return doubleUrlEncode;
    }

    public void setDoubleUrlEncode(Boolean doubleUrlEncode) {
        this.doubleUrlEncode = doubleUrlEncode;
    }

    public Boolean getNormalizePath() {
        return normalizePath;
    }

    public void setNormalizePath(Boolean normalizePath) {
        this.normalizePath = normalizePath;
    }

    public Boolean getPayloadSigningEnabled() {
        return payloadSigningEnabled;
    }

    public void setPayloadSigningEnabled(Boolean payloadSigningEnabled) {
        this.payloadSigningEnabled = payloadSigningEnabled;
    }

    public Boolean getChunkEncodingEnabled() {
        return chunkEncodingEnabled;
    }

    public void setChunkEncodingEnabled(Boolean chunkEncodingEnabled) {
        this.chunkEncodingEnabled = chunkEncodingEnabled;
    }
}
