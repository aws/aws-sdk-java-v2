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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

@SdkPublicApi
public class S3 {

    private final String configurationId;
    private final S3Bucket bucket;
    private final S3Object object;
    private final String s3SchemaVersion;

    public S3(String configurationId, S3Bucket bucket, S3Object object, String s3SchemaVersion) {
        this.configurationId = configurationId;
        this.bucket = bucket;
        this.object = object;
        this.s3SchemaVersion = s3SchemaVersion;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public S3Bucket getBucket() {
        return bucket;
    }

    public S3Object getObject() {
        return object;
    }

    public String getS3SchemaVersion() {
        return s3SchemaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3 s3 = (S3) o;

        if (!Objects.equals(configurationId, s3.configurationId)) {
            return false;
        }
        if (!Objects.equals(bucket, s3.bucket)) {
            return false;
        }
        if (!Objects.equals(object, s3.object)) {
            return false;
        }
        return Objects.equals(s3SchemaVersion, s3.s3SchemaVersion);
    }

    @Override
    public int hashCode() {
        int result = configurationId != null ? configurationId.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (object != null ? object.hashCode() : 0);
        result = 31 * result + (s3SchemaVersion != null ? s3SchemaVersion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3")
                       .add("configurationId", configurationId)
                       .add("bucket", bucket)
                       .add("object", object)
                       .add("s3SchemaVersion", s3SchemaVersion)
                       .build();
    }
}
