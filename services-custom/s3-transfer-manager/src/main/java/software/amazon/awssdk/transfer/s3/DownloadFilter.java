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

import java.nio.file.Path;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * {@link DownloadFilter} allows you to filter out which objects should be downloaded as part of a {@link
 * DownloadDirectoryRequest}. You could use it, for example, to only download objects of a given size, of a given file extension,
 * of a given last-modified date, etc. Multiple {@link DownloadFilter}s can be composed together via {@link #and(Predicate)} and
 * {@link #or(Predicate)} methods.
 */
@SdkPublicApi
@SdkPreviewApi
public interface DownloadFilter extends Predicate<DownloadFileContext> {

    /**
     * Evaluate whether the remote {@link S3Object} should be downloaded to the destination {@link Path}.
     *
     * @param context A context object containing a description of the remote {@link S3Object} and a {@link Path} representing the
     *                local file destination
     * @return true if the object should be downloaded, false if the object should not be downloaded
     */
    @Override
    boolean test(DownloadFileContext context);

    /**
     * A {@link DownloadFilter} that downloads all objects. This is the default behavior if no filter is provided.
     */
    @SdkPreviewApi
    static DownloadFilter allObjects() {
        return ctx -> true;
    }
}
