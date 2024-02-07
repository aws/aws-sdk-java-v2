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

import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class PauseResumeHelper {
    private static final Logger log = Logger.loggerFor(PauseResumeHelper.class);

    protected boolean fileModified(ResumableFileUpload resumableFileUpload, S3AsyncClient s3AsyncClient) {
        boolean fileModified = !fileNotModified(resumableFileUpload.fileLength(),
                                                resumableFileUpload.fileLastModified(),
                                                resumableFileUpload.uploadFileRequest().source());

        if (fileModified) {
            UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
            PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
            log.debug(() -> String.format("The file (%s) has been modified since "
                                          + "the last pause. " +
                                          "The SDK will upload the requested object in bucket"
                                          + " (%s) with key (%s) from "
                                          + "the "
                                          + "beginning.",
                                          uploadFileRequest.source(),
                                          putObjectRequest.bucket(),
                                          putObjectRequest.key()));
            resumableFileUpload.multipartUploadId()
                               .ifPresent(id -> {
                                   log.debug(() -> "Aborting previous upload with multipartUploadId: " + id);
                                   s3AsyncClient.abortMultipartUpload(
                                                    AbortMultipartUploadRequest.builder()
                                                                               .bucket(putObjectRequest.bucket())
                                                                               .key(putObjectRequest.key())
                                                                               .uploadId(id)
                                                                               .build())
                                                .exceptionally(t -> {
                                                    log.warn(() -> String.format("Failed to abort previous multipart upload "
                                                                                 + "(id: %s)"
                                                                                 + ". You may need to call "
                                                                                 + "S3AsyncClient#abortMultiPartUpload to "
                                                                                 + "free all storage consumed by"
                                                                                 + " all parts. ",
                                                                                 id), t);
                                                    return null;
                                                });
                               });
        }

        return fileModified;
    }

   protected boolean hasResumeToken(ResumableFileUpload resumableFileUpload) {
        boolean hasResumeToken =
            resumableFileUpload.totalParts().isPresent() && resumableFileUpload.partSizeInBytes().isPresent();

        if (!hasResumeToken) {
            UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
            PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
            log.debug(() -> String.format("No resume token is found. " +
                                          "The SDK will upload the requested object in bucket"
                                          + " (%s) with key (%s) from "
                                          + "the beginning.",
                                          putObjectRequest.bucket(),
                                          putObjectRequest.key()));
        }

        return hasResumeToken;
    }
}
