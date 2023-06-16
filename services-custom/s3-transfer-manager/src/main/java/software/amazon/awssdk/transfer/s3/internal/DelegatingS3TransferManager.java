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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;


/**
 * An {@link S3TransferManager} that just delegates to another {@link S3TransferManager}.
 */
@SdkInternalApi
abstract class DelegatingS3TransferManager implements S3TransferManager {
    private final S3TransferManager delegate;

    protected DelegatingS3TransferManager(S3TransferManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        return delegate.upload(uploadRequest);
    }

    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        return delegate.uploadFile(uploadFileRequest);
    }

    @Override
    public DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        return delegate.uploadDirectory(uploadDirectoryRequest);
    }

    @Override
    public <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        return delegate.download(downloadRequest);
    }

    @Override
    public FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        return delegate.downloadFile(downloadRequest);
    }

    @Override
    public FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        return delegate.resumeDownloadFile(resumableFileDownload);
    }

    @Override
    public DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        return delegate.downloadDirectory(downloadDirectoryRequest);
    }

    @Override
    public Copy copy(CopyRequest copyRequest) {
        return delegate.copy(copyRequest);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
