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

package software.amazon.awssdk.transfer.s3.config;

import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;

/**
 * {@link DownloadFilter} allows you to filter out which objects should be downloaded as part of a {@link
 * DownloadDirectoryRequest}. You could use it, for example, to only download objects of a given size, of a given file extension,
 * of a given last-modified date, etc. Multiple {@link DownloadFilter}s can be composed together via {@link #and(Predicate)} and
 * {@link #or(Predicate)} methods.
 */
@SdkPublicApi
public interface DownloadFilter extends Predicate<S3Object> {

    /**
     * Evaluate condition the remote {@link S3Object} should be downloaded.
     *
     * @param s3Object Remote {@link S3Object}
     * @return true if the object should be downloaded, false if the object should not be downloaded
     */
    @Override
    boolean test(S3Object s3Object);

    /**
     * Returns a composed filter that performs AND operation
     */
    @Override
    default DownloadFilter and(Predicate<? super S3Object> other) {
        return s3Object -> test(s3Object) && other.test(s3Object);
    }

    /**
     * Returns a composed filter that performs OR operation
     */
    @Override
    default DownloadFilter or(Predicate<? super S3Object> other) {
        return s3Object -> test(s3Object) || other.test(s3Object);
    }

    /**
     * Returns a filter that negates the result
     */
    @Override
    default DownloadFilter negate() {
        return s3Object -> !test(s3Object);
    }

    /**
     * A {@link DownloadFilter} that downloads all non-folder objects. A folder is a 0-byte object created when a customer uses S3
     * console to create a folder, and it always ends with "/".
     *
     * <p>
     * This is the default behavior if no filter is provided.
     */
    static DownloadFilter allObjects() {
        return s3Object -> {
            boolean isFolder = s3Object.key().endsWith("/") &&
                               s3Object.size() != null &&
                               s3Object.size() == 0;
            return !isFolder;
        };
    }
}
