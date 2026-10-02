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
public class S3ObjectAnnotation {
    private final String name;
    private final Long size;
    private final String eTag;

    public S3ObjectAnnotation(String name, Long size, String eTag) {
        this.name = name;
        this.size = size;
        this.eTag = eTag;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public String getETag() {
        return eTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3ObjectAnnotation that = (S3ObjectAnnotation) o;
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(size, that.size)) {
            return false;
        }
        return Objects.equals(eTag, that.eTag);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (eTag != null ? eTag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3ObjectAnnotation")
                       .add("name", name)
                       .add("size", size)
                       .add("eTag", eTag)
                       .build();
    }
}
