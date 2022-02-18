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

package software.amazon.awssdk.transfer.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiPredicate;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * {@link DownloadFilter} allows you to filter out which objects should be downloaded as part of a {@link
 * DownloadDirectoryRequest}. You could use it, for example, to only download objects of a given size, of a given file extension,
 * of a given last-modified date, etc. Multiple {@link DownloadFilter}s can be composed together via {@link #and(BiPredicate)} and
 * {@link #or(BiPredicate)} methods.
 */
@SdkPublicApi
@SdkPreviewApi
public interface DownloadFilter extends BiPredicate<S3Object, Path> {

    /**
     * Evaluate whether the remote {@link S3Object} should be downloaded to the destination {@link Path}.
     *
     * @param s3Object        A description of the remote S3 object
     * @param destinationPath A path representing the local file destination
     * @return true if the object should be downloaded, false if the object should not be downloaded
     */
    @Override
    boolean test(S3Object s3Object, Path destinationPath);

    /**
     * A {@link DownloadFilter} that only downloads objects that are newer (i.e., modified more recently) than local files. This
     * relies on comparing the remote object's last modified time to the local file's last modified time. It does not compare
     * content checksums.
     * <p>
     * This filter is considered preview/experimental and may be modified or removed before release.
     */
    @SdkPreviewApi
    static DownloadFilter newerObjects() {
        return (remote, local) -> {
            try {
                if (Files.notExists(local)) {
                    return true;
                }
                if (Files.size(local) != remote.size()) {
                    return true;
                }
                if (Files.getLastModifiedTime(local).toInstant().isBefore(remote.lastModified())) {
                    return true;
                }
                return false;
            } catch (IOException e) {
                return true;
            }
        };
    }

    /**
     * A {@link DownloadFilter} that downloads all objects. This is the default behavior if no filter is provided.
     */
    @SdkPreviewApi
    static DownloadFilter allObjects() {
        return (remote, local) -> true;
    }
}
