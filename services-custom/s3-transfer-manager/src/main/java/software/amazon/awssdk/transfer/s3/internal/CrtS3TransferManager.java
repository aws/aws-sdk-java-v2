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

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.CRT_PROGRESS_LISTENER;
import static software.amazon.awssdk.services.s3.crt.S3CrtSdkHttpExecutionAttribute.METAREQUEST_PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.transfer.s3.internal.GenericS3TransferManager.assertNotUnsupportedArn;
import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.CrtFileUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link S3TransferManager} that uses CRT-based S3 client under the hood.
 */
@SdkInternalApi
class CrtS3TransferManager extends DelegatingS3TransferManager {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);
    private final S3AsyncClient s3AsyncClient;

    CrtS3TransferManager(TransferManagerConfiguration transferConfiguration, S3AsyncClient s3AsyncClient,
                         boolean isDefaultS3AsyncClient) {
        super(new GenericS3TransferManager(transferConfiguration, s3AsyncClient, isDefaultS3AsyncClient));
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        Validate.paramNotNull(uploadFileRequest, "uploadFileRequest");
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();

        Long fileContentLength = AsyncRequestBody.fromFile(uploadFileRequest.source()).contentLength().orElse(null);
        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadFileRequest, fileContentLength);

        Consumer<SdkHttpExecutionAttributes.Builder> attachObservable =
            b -> b.put(METAREQUEST_PAUSE_OBSERVABLE, observable)
                  .put(CRT_PROGRESS_LISTENER, progressUpdater.crtProgressListener());

        PutObjectRequest putObjectRequest = attachSdkAttribute(uploadFileRequest.putObjectRequest(), attachObservable);

        CompletableFuture<CompletedFileUpload> returnFuture = new CompletableFuture<>();

        progressUpdater.transferInitiated();
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(putObjectRequest.bucket(), "upload");

            CompletableFuture<PutObjectResponse> crtFuture =
                s3AsyncClient.putObject(putObjectRequest, uploadFileRequest.source());

            // Forward upload cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              r -> CompletedFileUpload.builder()
                                                                                      .response(r)
                                                                                      .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }


        return new CrtFileUpload(returnFuture, progressUpdater.progress(), observable, uploadFileRequest);
    }

    private FileUpload uploadFromBeginning(ResumableFileUpload resumableFileUpload, boolean fileModified,
                                           boolean noResumeToken) {
        UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        if (fileModified) {
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

        if (noResumeToken) {
            log.debug(() -> String.format("No resume token is found. " +
                                          "The SDK will upload the requested object in bucket"
                                          + " (%s) with key (%s) from "
                                          + "the beginning.",
                                          putObjectRequest.bucket(),
                                          putObjectRequest.key()));
        }


        return uploadFile(uploadFileRequest);
    }

    @Override
    public FileUpload resumeUploadFile(ResumableFileUpload resumableFileUpload) {
        Validate.paramNotNull(resumableFileUpload, "resumableFileUpload");

        boolean fileModified = !fileNotModified(resumableFileUpload.fileLength(),
                                                resumableFileUpload.fileLastModified(),
                                                resumableFileUpload.uploadFileRequest().source());

        boolean noResumeToken = !hasResumeToken(resumableFileUpload);

        if (fileModified || noResumeToken) {
            return uploadFromBeginning(resumableFileUpload, fileModified, noResumeToken);
        }

        return doResumeUpload(resumableFileUpload);
    }

    private FileUpload doResumeUpload(ResumableFileUpload resumableFileUpload) {
        UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        ResumeToken resumeToken = crtResumeToken(resumableFileUpload);

        Consumer<SdkHttpExecutionAttributes.Builder> attachResumeToken =
            b -> b.put(CRT_PAUSE_RESUME_TOKEN, resumeToken);

        PutObjectRequest modifiedPutObjectRequest = attachSdkAttribute(putObjectRequest, attachResumeToken);

        return uploadFile(uploadFileRequest.toBuilder()
                                           .putObjectRequest(modifiedPutObjectRequest)
                                           .build());
    }

    private static ResumeToken crtResumeToken(ResumableFileUpload resumableFileUpload) {
        return new ResumeToken(new ResumeToken.PutResumeTokenBuilder()
                                   .withNumPartsCompleted(resumableFileUpload.transferredParts().orElse(0L))
                                   .withTotalNumParts(resumableFileUpload.totalParts().orElse(0L))
                                   .withPartSize(resumableFileUpload.partSizeInBytes().getAsLong())
                                   .withUploadId(resumableFileUpload.multipartUploadId().orElse(null)));
    }

    private boolean hasResumeToken(ResumableFileUpload resumableFileUpload) {
        return resumableFileUpload.totalParts().isPresent() && resumableFileUpload.partSizeInBytes().isPresent();
    }

    private PutObjectRequest attachSdkAttribute(PutObjectRequest putObjectRequest,
                                                Consumer<SdkHttpExecutionAttributes.Builder> builderMutation) {
        SdkHttpExecutionAttributes modifiedAttributes =
            putObjectRequest.overrideConfiguration().map(o -> o.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES))
                            .map(b -> b.toBuilder().applyMutation(builderMutation).build())
                            .orElseGet(() -> SdkHttpExecutionAttributes.builder().applyMutation(builderMutation).build());

        Consumer<AwsRequestOverrideConfiguration.Builder> attachSdkHttpAttributes =
            b -> b.putExecutionAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES, modifiedAttributes);

        AwsRequestOverrideConfiguration modifiedRequestOverrideConfig =
            putObjectRequest.overrideConfiguration()
                            .map(o -> o.toBuilder().applyMutation(attachSdkHttpAttributes).build())
                            .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                            .applyMutation(attachSdkHttpAttributes)
                                                                            .build());

        return putObjectRequest.toBuilder()
                               .overrideConfiguration(modifiedRequestOverrideConfig)
                               .build();
    }
}
