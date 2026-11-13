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

package software.amazon.awssdk.s3.mockserver.server;

import java.util.Objects;
import software.amazon.awssdk.s3.mockserver.client.Payload;

public final class ServedVersion {
    private final String key;
    private final Payload content;
    private final boolean altered;

    public ServedVersion(String key, Payload content, boolean altered) {
        this.key = key;
        this.content = content;
        this.altered = altered;
    }

    public String key() {
        return key;
    }

    public Payload content() {
        return content;
    }

    public boolean altered() {
        return altered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServedVersion that = (ServedVersion) o;
        return Objects.equals(key, that.key)
                && Objects.equals(content, that.content)
                && altered == that.altered;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(content);
        result = 31 * result + Objects.hashCode(altered);
        return result;
    }

    @Override
    public String toString() {
        return "ServedVersion[key=" + key + ", content=" + content + ", altered=" + altered + "]";
    }
}
