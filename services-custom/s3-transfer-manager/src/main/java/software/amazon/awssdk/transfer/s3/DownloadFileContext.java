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
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Context object for determining which objects should be downloaded as part of a {@link DownloadDirectoryRequest}.
 *
 * @see DownloadFilter
 */
public interface DownloadFileContext {
    /**
     * @return A description of the remote S3 object
     */
    S3Object source();

    /**
     * @return A path representing the local file destination
     */
    Path destination();
}
