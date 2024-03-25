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

package software.amazon.awssdk.eventnotifications.s3.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

@SdkPublicApi
public class S3Object {

    private final String key;
    private final Long size;
    private final String eTag;
    private final String versionId;
    private final String sequencer;

    public S3Object(String key, Long size, String eTag, String versionId, String sequencer) {
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
    public String getUrlDecodedKey() {
        try {
            return URLDecoder.decode(getKey(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Long getSizeAsLong() {
        return size;
    }

    public String getETag() {
        return eTag;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getSequencer() {
        return sequencer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3Object s3Object = (S3Object) o;

        if (!Objects.equals(key, s3Object.key)) {
            return false;
        }
        if (!Objects.equals(size, s3Object.size)) {
            return false;
        }
        if (!Objects.equals(eTag, s3Object.eTag)) {
            return false;
        }
        if (!Objects.equals(versionId, s3Object.versionId)) {
            return false;
        }
        return Objects.equals(sequencer, s3Object.sequencer);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (eTag != null ? eTag.hashCode() : 0);
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        result = 31 * result + (sequencer != null ? sequencer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3Object")
                       .add("key", key)
                       .add("size", size)
                       .add("eTag", eTag)
                       .add("versionId", versionId)
                       .add("sequencer", sequencer)
                       .build();
    }
}
