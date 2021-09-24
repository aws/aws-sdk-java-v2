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

package software.amazon.awssdk.transfer.s3.internal;

import java.nio.file.Path;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.FailedUpload;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
final class DefaultFailedUpload implements FailedUpload {
    private final Throwable throwable;
    private final Path path;

    DefaultFailedUpload(Builder builder) {
        this.throwable = builder.throwable;
        this.path = builder.path;
    }

    public Throwable exception() {
        return throwable;
    }

    public Path path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultFailedUpload that = (DefaultFailedUpload) o;

        if (!Objects.equals(throwable, that.throwable)) {
            return false;
        }
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = throwable != null ? throwable.hashCode() : 0;
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("FailedUpload")
                       .add("exception", throwable)
                       .add("path", path)
                       .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Throwable throwable;
        private Path path;

        private Builder() {
        }

        public Builder exception(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public DefaultFailedUpload build() {
            return new DefaultFailedUpload(this);
        }
    }
}
