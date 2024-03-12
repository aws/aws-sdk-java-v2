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

import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.JAVA_PROGRESS_LISTENER;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.RESUME_TOKEN;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.transfer.s3.internal.utils.ResumableRequestConverter.toDownloadFileRequestAndTransformer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.multipart.PauseObservable;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultCopy;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDirectoryUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.ResumeTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
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
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
class GenericS3TransferManager implements S3TransferManager {
    protected static final int DEFAULT_FILE_UPLOAD_CHUNK_SIZE = (int) (16 * MB);
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);
    private static final PauseResumeHelper PAUSE_RESUME_HELPER = new PauseResumeHelper();
    private final S3AsyncClient s3AsyncClient;
    private final UploadDirectoryHelper uploadDirectoryHelper;
    private final DownloadDirectoryHelper downloadDirectoryHelper;
    private final boolean isDefaultS3AsyncClient;

    private final TransferManagerConfiguration transferConfiguration;

    GenericS3TransferManager(TransferManagerConfiguration transferConfiguration,
                             S3AsyncClient s3AsyncClient,
                             boolean isDefaultS3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        this.transferConfiguration = transferConfiguration;
        uploadDirectoryHelper = new UploadDirectoryHelper(transferConfiguration, this::uploadFile);
        ListObjectsHelper listObjectsHelper = new ListObjectsHelper(s3AsyncClient::listObjectsV2);
        downloadDirectoryHelper = new DownloadDirectoryHelper(transferConfiguration,
                                                              listObjectsHelper,
                                                              this::downloadFile);
        this.isDefaultS3AsyncClient = isDefaultS3AsyncClient;
    }

    @SdkTestInternalApi
    GenericS3TransferManager(S3AsyncClient s3AsyncClient,
                             UploadDirectoryHelper uploadDirectoryHelper,
                             TransferManagerConfiguration configuration,
                             DownloadDirectoryHelper downloadDirectoryHelper) {
        this.s3AsyncClient = s3AsyncClient;
        this.isDefaultS3AsyncClient = false;
        this.transferConfiguration = configuration;
        this.uploadDirectoryHelper = uploadDirectoryHelper;
        this.downloadDirectoryHelper = downloadDirectoryHelper;
    }

    @Override
    public final Upload upload(UploadRequest uploadRequest) {
        Validate.paramNotNull(uploadRequest, "uploadRequest");

        AsyncRequestBody requestBody = uploadRequest.requestBody();

        CompletableFuture<CompletedUpload> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadRequest,
                                                                              requestBody.contentLength().orElse(null));
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(returnFuture);

        PutObjectRequest putObjectRequest = uploadRequest.putObjectRequest();
        if (isS3ClientMultipartEnabled()) {
            Consumer<AwsRequestOverrideConfiguration.Builder> attachProgressListener =
                b -> b.putExecutionAttribute(JAVA_PROGRESS_LISTENER, progressUpdater.multipartClientProgressListener());
            putObjectRequest = attachSdkAttribute(uploadRequest.putObjectRequest(), attachProgressListener);
        }

        try {
            assertNotUnsupportedArn(uploadRequest.putObjectRequest().bucket(), "upload");

            CompletableFuture<PutObjectResponse> future =
                s3AsyncClient.putObject(putObjectRequest, requestBody);

            // Forward upload cancellation to future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, future);

            CompletableFutureUtils.forwardTransformedResultTo(future, returnFuture,
                                                              r -> CompletedUpload.builder()
                                                                                  .response(r)
                                                                                  .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultUpload(returnFuture, progressUpdater.progress());
    }

    /**
     * May be overridden by subclasses to provide customized behavior
     */
    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        Validate.paramNotNull(uploadFileRequest, "uploadFileRequest");

        AsyncRequestBody requestBody =
            FileAsyncRequestBody.builder()
                                .path(uploadFileRequest.source())
                                .chunkSizeInBytes(DEFAULT_FILE_UPLOAD_CHUNK_SIZE)
                                .build();

        CompletableFuture<CompletedFileUpload> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadFileRequest,
                                                                              requestBody.contentLength().orElse(null));
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(returnFuture);

        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        PauseObservable pauseObservable;
        if (isS3ClientMultipartEnabled()) {
            pauseObservable = new PauseObservable();
            Consumer<AwsRequestOverrideConfiguration.Builder> attachObservableAndListener =
                b -> b.putExecutionAttribute(PAUSE_OBSERVABLE, pauseObservable)
                      .putExecutionAttribute(JAVA_PROGRESS_LISTENER, progressUpdater.multipartClientProgressListener());
            putObjectRequest = attachSdkAttribute(uploadFileRequest.putObjectRequest(), attachObservableAndListener);
        } else {
            pauseObservable = null;
        }


        try {
            assertNotUnsupportedArn(putObjectRequest.bucket(), "upload");

            CompletableFuture<PutObjectResponse> putObjectFuture =
                s3AsyncClient.putObject(putObjectRequest, requestBody);

            // Forward upload cancellation to putObjectFuture
            CompletableFutureUtils.forwardExceptionTo(returnFuture, putObjectFuture);

            CompletableFutureUtils.forwardTransformedResultTo(putObjectFuture, returnFuture,
                                                              r -> CompletedFileUpload.builder()
                                                                                      .response(r)
                                                                                      .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }
        return new DefaultFileUpload(returnFuture, progressUpdater.progress(), pauseObservable, uploadFileRequest);
    }

    @Override
    public final FileUpload resumeUploadFile(ResumableFileUpload resumableFileUpload) {
        Validate.paramNotNull(resumableFileUpload, "resumableFileUpload");

        boolean fileModified = PAUSE_RESUME_HELPER.fileModified(resumableFileUpload, s3AsyncClient);
        boolean noResumeToken = !PAUSE_RESUME_HELPER.hasResumeToken(resumableFileUpload);

        if (fileModified || noResumeToken) {
            return uploadFile(resumableFileUpload.uploadFileRequest());
        }

        return doResumeUpload(resumableFileUpload);
    }

    private boolean isS3ClientMultipartEnabled() {
        // TODO use configuration getter when available
        return s3AsyncClient instanceof MultipartS3AsyncClient;
    }


    /**
     * Can be overridden by subclasses to provide different implementation
     */
    FileUpload doResumeUpload(ResumableFileUpload resumableFileUpload) {
        UploadFileRequest uploadFileRequest = resumableFileUpload.uploadFileRequest();
        PutObjectRequest putObjectRequest = uploadFileRequest.putObjectRequest();
        S3ResumeToken s3ResumeToken = s3ResumeToken(resumableFileUpload);

        Consumer<AwsRequestOverrideConfiguration.Builder> attachResumeToken =
            b -> b.putExecutionAttribute(RESUME_TOKEN, s3ResumeToken);

        PutObjectRequest modifiedPutObjectRequest = attachSdkAttribute(putObjectRequest, attachResumeToken);

        return uploadFile(uploadFileRequest.toBuilder()
                                           .putObjectRequest(modifiedPutObjectRequest)
                                           .build());
    }

    private static S3ResumeToken s3ResumeToken(ResumableFileUpload resumableFileUpload) {
        S3ResumeToken.Builder builder = S3ResumeToken.builder();

        builder.uploadId(resumableFileUpload.multipartUploadId().orElse(null));
        if (resumableFileUpload.partSizeInBytes().isPresent()) {
            builder.partSize(resumableFileUpload.partSizeInBytes().getAsLong());
        }
        if (resumableFileUpload.totalParts().isPresent()) {
            builder.totalNumParts(resumableFileUpload.totalParts().getAsLong());
        }
        if (resumableFileUpload.transferredParts().isPresent()) {
            builder.numPartsCompleted(resumableFileUpload.transferredParts().getAsLong());
        }

        return builder.build();
    }

    private PutObjectRequest attachSdkAttribute(PutObjectRequest putObjectRequest,
                                                Consumer<AwsRequestOverrideConfiguration.Builder> builderMutation) {
        AwsRequestOverrideConfiguration modifiedRequestOverrideConfig =
            putObjectRequest.overrideConfiguration()
                            .map(o -> o.toBuilder().applyMutation(builderMutation).build())
                            .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                            .applyMutation(builderMutation)
                                                                            .build());

        return putObjectRequest.toBuilder()
                               .overrideConfiguration(modifiedRequestOverrideConfig)
                               .build();
    }

    @Override
    public final DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        Validate.paramNotNull(uploadDirectoryRequest, "uploadDirectoryRequest");

        try {
            assertNotUnsupportedArn(uploadDirectoryRequest.bucket(), "uploadDirectory");

            return uploadDirectoryHelper.uploadDirectory(uploadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryUpload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public final <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadRequest");

        AsyncResponseTransformer<GetObjectResponse, ResultT> responseTransformer =
            downloadRequest.responseTransformer();

        CompletableFuture<CompletedDownload<ResultT>> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);
        progressUpdater.transferInitiated();
        responseTransformer = progressUpdater.wrapResponseTransformer(responseTransformer);
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<ResultT> future = doGetObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, future);

            CompletableFutureUtils.forwardTransformedResultTo(future, returnFuture,
                                                              r -> CompletedDownload.builder()
                                                                                    .result(r)
                                                                                    .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultDownload<>(returnFuture, progressUpdater.progress());
    }

    @Override
    public final FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadFileRequest");

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            AsyncResponseTransformer.toFile(downloadRequest.destination(),
                                            FileTransformerConfiguration.defaultCreateOrReplaceExisting());

        CompletableFuture<CompletedFileDownload> returnFuture = new CompletableFuture<>();
        TransferProgressUpdater progressUpdater = doDownloadFile(downloadRequest, responseTransformer, returnFuture);

        return new DefaultFileDownload(returnFuture, progressUpdater.progress(), () -> downloadRequest, null);
    }

    private TransferProgressUpdater doDownloadFile(
        DownloadFileRequest downloadRequest,
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer,
        CompletableFuture<CompletedFileDownload> returnFuture) {
        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);
        try {
            progressUpdater.transferInitiated();
            responseTransformer = progressUpdater.wrapResponseTransformer(responseTransformer);
            progressUpdater.registerCompletion(returnFuture);

            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<GetObjectResponse> future = doGetObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, future);

            CompletableFutureUtils.forwardTransformedResultTo(future, returnFuture,
                                                              res -> CompletedFileDownload.builder()
                                                                                          .response(res)
                                                                                          .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }
        return progressUpdater;
    }

    @Override
    public final FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        Validate.paramNotNull(resumableFileDownload, "resumableFileDownload");
        CompletableFuture<CompletedFileDownload> returnFuture = new CompletableFuture<>();
        DownloadFileRequest originalDownloadRequest = resumableFileDownload.downloadFileRequest();
        GetObjectRequest getObjectRequest = originalDownloadRequest.getObjectRequest();
        CompletableFuture<TransferProgress> progressFuture = new CompletableFuture<>();
        CompletableFuture<DownloadFileRequest> newDownloadFileRequestFuture = new CompletableFuture<>();

        CompletableFuture<HeadObjectResponse> headFuture =
            s3AsyncClient.headObject(b -> b.bucket(getObjectRequest.bucket()).key(getObjectRequest.key()));

        // Ensure cancellations are forwarded to the head future
        CompletableFutureUtils.forwardExceptionTo(returnFuture, headFuture);

        headFuture.thenAccept(headObjectResponse -> {
            Pair<DownloadFileRequest, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
                requestPair = toDownloadFileRequestAndTransformer(resumableFileDownload, headObjectResponse,
                                                                  originalDownloadRequest);

            DownloadFileRequest newDownloadFileRequest = requestPair.left();
            newDownloadFileRequestFuture.complete(newDownloadFileRequest);
            log.debug(() -> "Sending downloadFileRequest " + newDownloadFileRequest);

            TransferProgressUpdater progressUpdater = doDownloadFile(newDownloadFileRequest,
                                                                     requestPair.right(),
                                                                     returnFuture);
            progressFuture.complete(progressUpdater.progress());
        }).exceptionally(throwable -> {
            handleException(returnFuture, progressFuture, newDownloadFileRequestFuture, throwable);
            return null;
        });

        return new DefaultFileDownload(returnFuture,
                                       new ResumeTransferProgress(progressFuture),
                                       () -> newOrOriginalRequestForPause(newDownloadFileRequestFuture, originalDownloadRequest),
                                       resumableFileDownload);
    }

    private DownloadFileRequest newOrOriginalRequestForPause(CompletableFuture<DownloadFileRequest> newDownloadFuture,
                                                             DownloadFileRequest originalDownloadRequest) {
        try {
            return newDownloadFuture.getNow(originalDownloadRequest);
        } catch (CompletionException e) {
            return originalDownloadRequest;
        }
    }

    private static void handleException(CompletableFuture<CompletedFileDownload> returnFuture,
                                        CompletableFuture<TransferProgress> progressFuture,
                                        CompletableFuture<DownloadFileRequest> newDownloadFileRequestFuture,
                                        Throwable throwable) {
        Throwable exceptionCause = throwable instanceof CompletionException ? throwable.getCause() : throwable;

        Throwable propagatedException = exceptionCause instanceof SdkException || exceptionCause instanceof Error
                                        ? exceptionCause
                                        : SdkClientException.create("Failed to resume the request", exceptionCause);

        returnFuture.completeExceptionally(propagatedException);
        progressFuture.completeExceptionally(propagatedException);
        newDownloadFileRequestFuture.completeExceptionally(propagatedException);


    }

    @Override
    public final DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        Validate.paramNotNull(downloadDirectoryRequest, "downloadDirectoryRequest");

        try {
            assertNotUnsupportedArn(downloadDirectoryRequest.bucket(), "downloadDirectoryRequest");

            return downloadDirectoryHelper.downloadDirectory(downloadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryDownload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public final Copy copy(CopyRequest copyRequest) {
        Validate.paramNotNull(copyRequest, "copyRequest");

        CompletableFuture<CompletedCopy> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(copyRequest, null);
        progressUpdater.transferInitiated();
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().sourceBucket(), "copy sourceBucket");
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().destinationBucket(), "copy destinationBucket");

            CompletableFuture<CopyObjectResponse> future =
                s3AsyncClient.copyObject(copyRequest.copyObjectRequest());

            // Forward transfer cancellation to future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, future);

            CompletableFutureUtils.forwardTransformedResultTo(future, returnFuture,
                                                              r -> CompletedCopy.builder()
                                                                                .response(r)
                                                                                .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultCopy(returnFuture, progressUpdater.progress());
    }

    @Override
    public final void close() {
        if (isDefaultS3AsyncClient) {
            IoUtils.closeQuietly(s3AsyncClient, log.logger());
        }
        IoUtils.closeQuietly(transferConfiguration, log.logger());
    }

    protected static void assertNotUnsupportedArn(String bucket, String operation) {
        if (bucket == null) {
            return;
        }

        if (!bucket.startsWith("arn:")) {
            return;
        }

        if (isObjectLambdaArn(bucket)) {
            String error = String.format("%s does not support S3 Object Lambda resources", operation);
            throw new IllegalArgumentException(error);
        }

        Arn arn = Arn.fromString(bucket);

        if (isMrapArn(arn)) {
            String error = String.format("%s does not support S3 multi-region access point ARN", operation);
            throw new IllegalArgumentException(error);
        }
    }

    private static boolean isObjectLambdaArn(String arn) {
        return arn.contains(":s3-object-lambda");
    }

    private static boolean isMrapArn(Arn arn) {
        S3Resource s3Resource = S3ArnConverter.create().convertArn(arn);

        S3AccessPointResource s3EndpointResource =
            Validate.isInstanceOf(S3AccessPointResource.class, s3Resource,
                                  "An ARN was passed as a bucket parameter to an S3 operation, however it does not "
                                  + "appear to be a valid S3 access point ARN.");

        return !s3EndpointResource.region().isPresent();
    }

    // TODO remove once MultipartS3AsyncClient is complete
    private <ResultT> CompletableFuture<ResultT> doGetObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ResultT> asyncResponseTransformer) {
        S3AsyncClient clientToUse = s3AsyncClient;
        if (s3AsyncClient instanceof MultipartS3AsyncClient) {
            clientToUse = (S3AsyncClient) ((DelegatingS3AsyncClient) s3AsyncClient).delegate();
        }
        return clientToUse.getObject(getObjectRequest, asyncResponseTransformer);
    }
}
