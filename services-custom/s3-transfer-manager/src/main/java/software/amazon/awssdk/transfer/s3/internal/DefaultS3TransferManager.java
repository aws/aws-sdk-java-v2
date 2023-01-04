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
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.METAREQUEST_PAUSE_OBSERVABLE;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.transfer.s3.internal.utils.FileUtils.fileNotModified;
import static software.amazon.awssdk.transfer.s3.internal.utils.ResumableRequestConverter.toDownloadFileRequestAndTransformer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
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
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);
    private static final int DEFAULT_FILE_UPLOAD_CHUNK_SIZE = (int) (16 * MB);
    private final S3AsyncClient s3AsyncClient;
    private final TransferManagerConfiguration transferConfiguration;
    private final UploadDirectoryHelper uploadDirectoryHelper;
    private final DownloadDirectoryHelper downloadDirectoryHelper;
    private final boolean isDefaultS3AsyncClient;

    private final S3ClientType s3ClientType;

    public DefaultS3TransferManager(DefaultBuilder tmBuilder) {
        transferConfiguration = resolveTransferManagerConfiguration(tmBuilder);
        if (tmBuilder.s3AsyncClient == null) {
            isDefaultS3AsyncClient = true;
            s3AsyncClient = defaultS3AsyncClient().get();
        } else {
            isDefaultS3AsyncClient = false;
            s3AsyncClient = tmBuilder.s3AsyncClient;
        }

        uploadDirectoryHelper = new UploadDirectoryHelper(transferConfiguration, this::uploadFile);
        ListObjectsHelper listObjectsHelper = new ListObjectsHelper(s3AsyncClient::listObjectsV2);
        downloadDirectoryHelper = new DownloadDirectoryHelper(transferConfiguration,
                                                              listObjectsHelper,
                                                              this::downloadFile);

        if (s3AsyncClient instanceof S3CrtAsyncClient) {
            s3ClientType = S3ClientType.CRT_BASED;
        } else {
            s3ClientType = S3ClientType.JAVA_BASED;
            log.warn(() -> "The provided S3AsyncClient is not an instance of S3CrtAsyncClient, and thus multipart"
                           + " upload/download feature is not enabled and resumable file upload is not supported. To benefit "
                           + "from maximum throughput,"
                           + " consider using S3AsyncClient.crtBuilder().build() instead.");
        }
    }

    @SdkTestInternalApi
    DefaultS3TransferManager(S3AsyncClient s3CrtAsyncClient,
                             UploadDirectoryHelper uploadDirectoryHelper,
                             TransferManagerConfiguration configuration,
                             DownloadDirectoryHelper downloadDirectoryHelper) {
        this.s3AsyncClient = s3CrtAsyncClient;
        this.isDefaultS3AsyncClient = false;
        this.transferConfiguration = configuration;
        this.uploadDirectoryHelper = uploadDirectoryHelper;
        this.downloadDirectoryHelper = downloadDirectoryHelper;
        s3ClientType = s3CrtAsyncClient instanceof S3CrtAsyncClient ? S3ClientType.CRT_BASED : S3ClientType.JAVA_BASED;
    }

    private static Supplier<S3AsyncClient> defaultS3AsyncClient() {
        if (crtInClasspath()) {
            return S3AsyncClient::crtCreate;
        }
        return S3AsyncClient::create;
    }

    private static boolean crtInClasspath() {
        try {
            ClassLoaderHelper.loadClass("software.amazon.awssdk.crt.s3.S3Client", false);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private static TransferManagerConfiguration resolveTransferManagerConfiguration(DefaultBuilder tmBuilder) {
        TransferManagerConfiguration.Builder transferConfigBuilder = TransferManagerConfiguration.builder();
        transferConfigBuilder.uploadDirectoryFollowSymbolicLinks(tmBuilder.uploadDirectoryFollowSymbolicLinks);
        transferConfigBuilder.uploadDirectoryMaxDepth(tmBuilder.uploadDirectoryMaxDepth);
        transferConfigBuilder.executor(tmBuilder.executor);
        return transferConfigBuilder.build();
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        Validate.paramNotNull(uploadRequest, "uploadRequest");

        AsyncRequestBody requestBody = uploadRequest.requestBody();

        CompletableFuture<CompletedUpload> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadRequest, requestBody);
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(uploadRequest.putObjectRequest().bucket(), "upload");

            CompletableFuture<PutObjectResponse> crtFuture =
                s3AsyncClient.putObject(uploadRequest.putObjectRequest(), requestBody);

            // Forward upload cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              r -> CompletedUpload.builder()
                                                                                  .response(r)
                                                                                  .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultUpload(returnFuture, progressUpdater.progress());
    }

    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        Validate.paramNotNull(uploadFileRequest, "uploadFileRequest");
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();

        AsyncRequestBody requestBody =
            FileAsyncRequestBody.builder()
                                .path(uploadFileRequest.source())
                                .chunkSizeInBytes(DEFAULT_FILE_UPLOAD_CHUNK_SIZE)
                                .build();

        Consumer<SdkHttpExecutionAttributes.Builder> attachObservable =
            b -> b.put(METAREQUEST_PAUSE_OBSERVABLE, observable);

        PutObjectRequest putObjectRequest = attachSdkAttribute(uploadFileRequest.putObjectRequest(), attachObservable);

        CompletableFuture<CompletedFileUpload> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadFileRequest, requestBody);
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(putObjectRequest.bucket(), "upload");

            CompletableFuture<PutObjectResponse> crtFuture =
                s3AsyncClient.putObject(putObjectRequest, requestBody);

            // Forward upload cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              r -> CompletedFileUpload.builder()
                                                                                      .response(r)
                                                                                      .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }


        return new DefaultFileUpload(returnFuture, progressUpdater.progress(), observable, uploadFileRequest, s3ClientType);
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

    @Override
    public DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        Validate.paramNotNull(uploadDirectoryRequest, "uploadDirectoryRequest");

        try {
            assertNotUnsupportedArn(uploadDirectoryRequest.bucket(), "uploadDirectory");

            return uploadDirectoryHelper.uploadDirectory(uploadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryUpload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
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

            CompletableFuture<ResultT> crtFuture =
                s3AsyncClient.getObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              r -> CompletedDownload.builder()
                                                                                    .result(r)
                                                                                    .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultDownload<>(returnFuture, progressUpdater.progress());
    }

    @Override
    public FileDownload downloadFile(DownloadFileRequest downloadRequest) {
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

            CompletableFuture<GetObjectResponse> crtFuture =
                s3AsyncClient.getObject(downloadRequest.getObjectRequest(),
                                        responseTransformer);

            // Forward download cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              res -> CompletedFileDownload.builder()
                                                                                          .response(res)
                                                                                          .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }
        return progressUpdater;
    }

    @Override
    public FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
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
    public DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        Validate.paramNotNull(downloadDirectoryRequest, "downloadDirectoryRequest");

        try {
            assertNotUnsupportedArn(downloadDirectoryRequest.bucket(), "downloadDirectoryRequest");

            return downloadDirectoryHelper.downloadDirectory(downloadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryDownload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public Copy copy(CopyRequest copyRequest) {
        Validate.paramNotNull(copyRequest, "copyRequest");

        CompletableFuture<CompletedCopy> returnFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(copyRequest, null);
        progressUpdater.transferInitiated();
        progressUpdater.registerCompletion(returnFuture);

        try {
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().sourceBucket(), "copy sourceBucket");
            assertNotUnsupportedArn(copyRequest.copyObjectRequest().destinationBucket(), "copy destinationBucket");

            CompletableFuture<CopyObjectResponse> crtFuture =
                s3AsyncClient.copyObject(copyRequest.copyObjectRequest());

            // Forward transfer cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

            CompletableFutureUtils.forwardTransformedResultTo(crtFuture, returnFuture,
                                                              r -> CompletedCopy.builder()
                                                                                .response(r)
                                                                                .build());
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return new DefaultCopy(returnFuture, progressUpdater.progress());
    }

    @Override
    public void close() {
        if (isDefaultS3AsyncClient) {
            s3AsyncClient.close();
        }
        transferConfiguration.close();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private static void assertNotUnsupportedArn(String bucket, String operation) {
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

    private static final class DefaultBuilder implements S3TransferManager.Builder {
        private S3AsyncClient s3AsyncClient;
        private Executor executor;
        private Boolean uploadDirectoryFollowSymbolicLinks;
        private Integer uploadDirectoryMaxDepth;

        private DefaultBuilder() {
        }

        @Override
        public Builder s3Client(S3AsyncClient s3AsyncClient) {
            this.s3AsyncClient = s3AsyncClient;
            return this;
        }

        @Override
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public Builder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks) {
            this.uploadDirectoryFollowSymbolicLinks = uploadDirectoryFollowSymbolicLinks;
            return this;
        }

        public void setUploadDirectoryFollowSymbolicLinks(Boolean followSymbolicLinks) {
            uploadDirectoryFollowSymbolicLinks(followSymbolicLinks);
        }

        public Boolean getUploadDirectoryFollowSymbolicLinks() {
            return uploadDirectoryFollowSymbolicLinks;
        }

        @Override
        public Builder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth) {
            this.uploadDirectoryMaxDepth = uploadDirectoryMaxDepth;
            return this;
        }

        public void setUploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth) {
            uploadDirectoryMaxDepth(uploadDirectoryMaxDepth);
        }

        public Integer getUploadDirectoryMaxDepth() {
            return uploadDirectoryMaxDepth;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
