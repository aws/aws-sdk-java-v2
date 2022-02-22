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
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.DownloadFileContext;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;


@SdkInternalApi
@SdkPreviewApi
public final class DefaultDownloadFileContext implements DownloadFileContext {

    private final S3Object source;
    private final Path destination;

    public DefaultDownloadFileContext(S3Object source, Path destination) {
        this.source = Validate.notNull(source, "source");
        this.destination = Validate.notNull(destination, "destination");
    }

    @Override
    public S3Object source() {
        return source;
    }

    @Override
    public Path destination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDownloadFileContext that = (DefaultDownloadFileContext) o;

        if (!Objects.equals(source, that.source)) {
            return false;
        }
        return Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultDownloadFileContext")
                       .add("source", source)
                       .add("destination", destination)
                       .build();
    }
}
