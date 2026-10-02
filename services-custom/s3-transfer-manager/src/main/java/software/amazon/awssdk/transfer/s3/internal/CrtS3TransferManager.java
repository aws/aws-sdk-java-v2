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
import static software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient.RESPONSE_FILE_OPTION;
import static software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient.RESPONSE_FILE_PATH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.transfer.s3.internal.utils.ResumableRequestConverter.canResumeDownload;
import static software.amazon.awssdk.transfer.s3.internal.utils.ResumableRequestConverter.toCrtDownloadFileRequest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions.ResponseFileOption;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.CrtResponseFileResponseTransformer;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.CrtFileDownload;
import software.amazon.awssdk.transfer.s3.internal.model.CrtFileUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.ResumeTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link S3TransferManager} that uses CRT-based S3 client under the hood.
 */
@SdkInternalApi
class CrtS3TransferManager extends GenericS3TransferManager {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private final S3AsyncClient s3AsyncClient;

    CrtS3TransferManager(TransferManagerConfiguration transferConfiguration, S3AsyncClient s3AsyncClient,
                         boolean isDefaultS3AsyncClient) {
        super(transferConfiguration, s3AsyncClient, isDefaultS3AsyncClient);
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public final Upload upload(UploadRequest uploadRequest) {
        Validate.paramNotNull(uploadRequest, "uploadRequest");

        AsyncRequestBody requestBody = uploadRequest.requestBody();

        CompletableFuture<CompletedUpload> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadRequest,
                                                                              requestBody.contentLength().orElse(null));
        progressUpdater.transferInitiated();
        progressUpdater.registerCompletion(returnFuture);

        Consumer<SdkHttpExecutionAttributes.Builder> attachProgress =
            b -> b.put(CRT_PROGRESS_LISTENER, progressUpdater.crtProgressListener());

        PutObjectRequest putObjectRequest = attachCrtSdkAttribute(uploadRequest.putObjectRequest(), attachProgress);

        doUpload(putObjectRequest, requestBody, returnFuture);

        return new DefaultUpload(returnFuture, progressUpdater.progress());
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

        PutObjectRequest putObjectRequest = attachCrtSdkAttribute(uploadFileRequest.putObjectRequest(), attachObservable);

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

    @Override
    FileUpload doResumeUpload(ResumableFileUpload resumableFileUpload) {
        UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        ResumeToken resumeToken = crtResumeToken(resumableFileUpload);

        Consumer<SdkHttpExecutionAttributes.Builder> attachResumeToken =
            b -> b.put(CRT_PAUSE_RESUME_TOKEN, resumeToken);

        PutObjectRequest modifiedPutObjectRequest = attachCrtSdkAttribute(putObjectRequest, attachResumeToken);

        return uploadFile(uploadFileRequest.toBuilder()
                                           .putObjectRequest(modifiedPutObjectRequest)
                                           .build());
    }

    /**
     * Downloads the object by handing the destination file to CRT, which writes the response body to it directly rather than
     * streaming the body back through the SDK.
     */
    @Override
    FileDownload doDownloadFile(DownloadFileRequest downloadRequest) {
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();
        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);

        DownloadFileRequest crtDownloadRequest = crtDownloadFileRequest(downloadRequest, observable, progressUpdater,
                                                                       ResponseFileOption.CREATE_OR_REPLACE);

        CompletableFuture<CompletedFileDownload> returnFuture = new CompletableFuture<>();
        initiateCrtDownload(crtDownloadRequest, progressUpdater, returnFuture);

        return new CrtFileDownload(returnFuture, progressUpdater.progress(), observable, () -> crtDownloadRequest, null);
    }

    @Override
    FileDownload doResumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        DownloadFileRequest originalDownloadRequest = resumableFileDownload.downloadFileRequest();
        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();

        CompletableFuture<CompletedFileDownload> returnFuture = new CompletableFuture<>();
        CompletableFuture<TransferProgress> progressFuture = new CompletableFuture<>();
        CompletableFuture<DownloadFileRequest> newDownloadFileRequestFuture = new CompletableFuture<>();
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();

        CompletableFuture<HeadObjectResponse> headFuture =
            s3AsyncClient.headObject(b -> b.bucket(getObjectRequest.bucket()).key(getObjectRequest.key()));

        // Ensure cancellations are forwarded to the head future
        CompletableFutureUtils.forwardExceptionTo(returnFuture, headFuture);

        headFuture.thenAccept(headObjectResponse -> {
            boolean restartFromBeginning = !canResumeDownload(resumableFileDownload, headObjectResponse)
                                           || hasCompletedParts(resumableFileDownload);

            DownloadFileRequest newDownloadFileRequest = toCrtDownloadFileRequest(resumableFileDownload, headObjectResponse,
                                                                                 originalDownloadRequest,
                                                                                 restartFromBeginning);

            // CRT appends to whatever the destination file already holds, so appending is only correct when the download is
            // genuinely being continued. Otherwise the file has to be replaced.
            ResponseFileOption responseFileOption = restartFromBeginning ? ResponseFileOption.CREATE_OR_REPLACE
                                                                        : ResponseFileOption.CREATE_OR_APPEND;

            TransferProgressUpdater progressUpdater = new TransferProgressUpdater(newDownloadFileRequest, null);
            DownloadFileRequest crtDownloadRequest = crtDownloadFileRequest(newDownloadFileRequest, observable,
                                                                           progressUpdater, responseFileOption);

            newDownloadFileRequestFuture.complete(crtDownloadRequest);
            log.debug(() -> "Sending downloadFileRequest " + crtDownloadRequest);

            initiateCrtDownload(crtDownloadRequest, progressUpdater, returnFuture);
            progressFuture.complete(progressUpdater.progress());
        }).exceptionally(throwable -> {
            handleException(returnFuture, progressFuture, newDownloadFileRequestFuture, throwable);
            return null;
        });

        return new CrtFileDownload(returnFuture,
                                   new ResumeTransferProgress(progressFuture),
                                   observable,
                                   () -> newOrOriginalRequestForPause(newDownloadFileRequestFuture,
                                                                      originalDownloadRequest),
                                   resumableFileDownload);
    }

    /**
     * A download that was paused while fetching individual parts can leave gaps in the destination file, because parts are
     * written at their own offsets rather than in order. The CRT-based client only ever appends to the end of the file, so
     * such a download cannot be continued and has to start over. This only arises when a {@link ResumableFileDownload}
     * produced by the Java-based transfer manager is resumed with the CRT-based one.
     */
    private static boolean hasCompletedParts(ResumableFileDownload resumableFileDownload) {
        if (resumableFileDownload.completedParts().isEmpty()) {
            return false;
        }
        log.debug(() -> "The paused download had completed individual parts, which the CRT-based S3 client cannot continue "
                        + "from. The SDK will download the S3 object from the beginning.");
        return true;
    }

    private void initiateCrtDownload(DownloadFileRequest downloadRequest,
                                     TransferProgressUpdater progressUpdater,
                                     CompletableFuture<CompletedFileDownload> returnFuture) {
        try {
            progressUpdater.transferInitiated();
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                progressUpdater.wrapCrtResponseFileTransformer(new CrtResponseFileResponseTransformer<>());
            progressUpdater.registerCompletion(returnFuture);

            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<GetObjectResponse> crtFuture =
                s3AsyncClient.getObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                             res -> CompletedFileDownload.builder()
                                                                                         .response(res)
                                                                                         .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }
    }

    /**
     * Attaches the execution attributes that tell the CRT-based client to write the response body straight to the destination
     * file, together with the progress listener and pause observable needed to report progress on and pause that transfer.
     */
    private DownloadFileRequest crtDownloadFileRequest(DownloadFileRequest downloadRequest,
                                                       S3MetaRequestPauseObservable observable,
                                                       TransferProgressUpdater progressUpdater,
                                                       ResponseFileOption responseFileOption) {
        GetObjectRequest getObjectRequest = attachSdkAttribute(
            downloadRequest.getObjectRequest(),
            b -> b.putExecutionAttribute(RESPONSE_FILE_PATH, downloadRequest.destination())
                  .putExecutionAttribute(RESPONSE_FILE_OPTION, responseFileOption));

        GetObjectRequest crtGetObjectRequest = attachCrtSdkAttribute(
            getObjectRequest,
            b -> b.put(METAREQUEST_PAUSE_OBSERVABLE, observable)
                  .put(CRT_PROGRESS_LISTENER, progressUpdater.crtProgressListener()));

        return downloadRequest.copy(r -> r.getObjectRequest(crtGetObjectRequest));
    }

    private static ResumeToken crtResumeToken(ResumableFileUpload resumableFileUpload) {
        return new ResumeToken(new ResumeToken.PutResumeTokenBuilder()
                                   .withNumPartsCompleted(resumableFileUpload.transferredParts().orElse(0L))
                                   .withTotalNumParts(resumableFileUpload.totalParts().orElse(0L))
                                   .withPartSize(resumableFileUpload.partSizeInBytes().getAsLong())
                                   .withUploadId(resumableFileUpload.multipartUploadId().orElse(null)));
    }

    private PutObjectRequest attachCrtSdkAttribute(PutObjectRequest putObjectRequest,
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

    private GetObjectRequest attachCrtSdkAttribute(GetObjectRequest getObjectRequest,
                                                   Consumer<SdkHttpExecutionAttributes.Builder> builderMutation) {
        SdkHttpExecutionAttributes existingAttributes =
            getObjectRequest.overrideConfiguration()
                            .map(o -> o.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES))
                            .orElse(null);

        SdkHttpExecutionAttributes modifiedAttributes =
            (existingAttributes == null ? SdkHttpExecutionAttributes.builder() : existingAttributes.toBuilder())
                .applyMutation(builderMutation)
                .build();

        return attachSdkAttribute(getObjectRequest,
                                  b -> b.putExecutionAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES, modifiedAttributes));
    }
}
