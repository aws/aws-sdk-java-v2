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

package software.amazon.awssdk.eventnotifications.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class S3ObjectEntity {

    private final String key;
    private final Long size;
    private final String eTag;
    private final String versionId;
    private final String sequencer;

    // @JsonCreator
    public S3ObjectEntity(
        // @JsonProperty(value = "key")
        String key,
        // @JsonProperty(value = "size")
        Long size,
        // @JsonProperty(value = "eTag")
        String eTag,
        // @JsonProperty(value = "versionId")
        String versionId,
        // @JsonProperty(value = "sequencer")
        String sequencer) {
        this.key = key;
        this.size = size;
        this.eTag = eTag;
        this.versionId = versionId;
        this.sequencer = sequencer;
    }

    public String getKey() {
        return key;
    }

    /**
     * S3 URL encodes the key of the object involved in the event. This is a convenience method to automatically URL decode the
     * key.
     *
     * @return The URL decoded object key.
     */
    // @JsonIgnore
    public String getUrlDecodedKey() {
        try {
            return URLDecoder.decode(getKey(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // @JsonProperty("size")
    public Long getSizeAsLong() {
        return size;
    }

    // @JsonProperty("eTag")
    public String getETag() {
        return eTag;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getSequencer() {
        return sequencer;
    }
}
