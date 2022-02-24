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

package software.amazon.awssdk.auth.signer.internal;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.SdkChecksum;

/**
 * Encapsulates Hash in String format and FlexibleChecksum Instance for a Request Content.
 */
@SdkInternalApi
public class ContentChecksum {
    private final String hash;

    private final SdkChecksum contentFlexibleChecksum;

    public ContentChecksum(String hash, SdkChecksum contentFlexibleChecksum) {
        this.hash = hash;
        this.contentFlexibleChecksum = contentFlexibleChecksum;
    }

    public String contentHash() {
        return hash;
    }

    public SdkChecksum contentFlexibleChecksum() {
        return contentFlexibleChecksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContentChecksum that = (ContentChecksum) o;
        return Objects.equals(hash, that.hash) &&
                Objects.equals(contentFlexibleChecksum, that.contentFlexibleChecksum);
    }

    @Override
    public int hashCode() {
        int result = hash != null ? hash.hashCode() : 0;
        result = 31 * result + (contentFlexibleChecksum != null ? contentFlexibleChecksum.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ContentChecksum{" +
                "hash='" + hash + '\'' +
                ", contentFlexibleChecksum=" + contentFlexibleChecksum +
                '}';
    }
}
