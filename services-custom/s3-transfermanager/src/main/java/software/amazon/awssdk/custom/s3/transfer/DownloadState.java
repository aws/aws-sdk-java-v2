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

package software.amazon.awssdk.custom.s3.transfer;

import java.io.InputStream;
import java.io.OutputStream;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The state of a paused download. The download can be resumed using {@link S3TransferManager#resumeDownload(DownloadState)}.
 */
@SdkPublicApi
public class DownloadState {
    /**
     * Persist this state to the given stream.
     *
     * @param os The stream to write this state to.
     */
    public void persistTo(OutputStream os) {
        throw new UnsupportedOperationException();
    }

    /**
     * Load a persisted state.
     *
     * @param is The stream to read the state from.
     * @return The loaded state.
     */
    public static DownloadState loadFrom(InputStream is) {
        throw new UnsupportedOperationException();
    }
}
