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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

@SdkInternalApi
public class DownloadFileMonitor implements TransferListener {
    private final DownloadFileRequest downloadFileRequest;
    private GetObjectResponse initialResponse;
    private CompletedFileDownload finalResponse;
    private boolean isFinished;

    public DownloadFileMonitor(DownloadFileRequest downloadFileRequest) {
        this.downloadFileRequest = downloadFileRequest;
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {

        if (context.initialResponse() instanceof GetObjectResponse) {
            initialResponse = (GetObjectResponse) context.initialResponse();
        }
    }

    /**
     * @return whether the current transfer is finished or not
     */
    public boolean isFinished() {
        return isFinished;
    }

    public Optional<GetObjectResponse> initialResponse() {
        return Optional.of(initialResponse);
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        isFinished = true;
        finalResponse = (CompletedFileDownload) context.completedTransfer();
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        isFinished = true;
    }

    /**
     * @return the download file request
     */
    public DownloadFileRequest downloadFileRequest() {
        return downloadFileRequest;
    }
}
