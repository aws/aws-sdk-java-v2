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

package software.amazon.awssdk.services.s3.multipart;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.services.s3.internal.multipart.PausableUpload;

@SdkProtectedApi
public class PauseObservable {

    private volatile PausableUpload pausableUpload;

    public void setPausableUpload(PausableUpload pausableUpload) {
        this.pausableUpload = pausableUpload;
    }

    public S3ResumeToken pause() {
        // single part upload or TM is not used
        if (pausableUpload == null) {
            return null;
        }
        return pausableUpload.pause();
    }

    @SdkTestInternalApi
    public boolean pausableUploadSet() {
        return pausableUpload != null;
    }
}
