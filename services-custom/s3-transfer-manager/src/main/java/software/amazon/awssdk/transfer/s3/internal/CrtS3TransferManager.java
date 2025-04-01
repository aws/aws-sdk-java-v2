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
import static software.amazon.awssdk.services.s3.internal.crt.DefaultS3CrtAsyncClient.RESPONSE_FILE_PATH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.CrtResponseFileResponseTransformer;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.CrtFileDownload;
import software.amazon.awssdk.transfer.s3.internal.model.CrtFileUpload;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
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
class CrtS3TransferManager extends GenericS3TransferManager {
    private final S3AsyncClient s3AsyncClient;

    CrtS3TransferManager(TransferManagerConfiguration transferConfiguration, S3AsyncClient s3AsyncClient,
                         boolean isDefaultS3AsyncClient) {
        super(transferConfiguration, s3AsyncClient, isDefaultS3AsyncClient);
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

    // @Override
    // public FileDownload downloadFile(DownloadFileRequest downloadRequest) {
    //     System.out.println("Using the CRT downloadFile!");
    //     Validate.paramNotNull(downloadRequest, "downloadFileRequest");
    //
    //    TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);
    //
    //
    //     // TODO: This could be a single method that takes two builders probably
    //    GetObjectRequest getObjectRequestWithAttributes = attachSdkHttpExecutionAttribute(
    //         attachSdkAttribute(
    //             downloadRequest.getObjectRequest(),
    //             b -> b
    //                 .putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, new MultipartDownloadResumeContext())
    //                 .putExecutionAttribute(RESPONSE_FILE_PATH, downloadRequest.destination())
    //
    //         ),
    //         b -> b.put(CRT_PROGRESS_LISTENER, progressUpdater.crtProgressListener())
    //     );
    //
    //     DownloadFileRequest downloadFileRequestWithAttributes =
    //         downloadRequest.copy(downloadFileRequest -> downloadFileRequest.getObjectRequest(getObjectRequestWithAttributes));
    //
    //     CompletableFuture<CompletedFileDownload> returnFuture = new CompletableFuture<>();
    //     progressUpdater.transferInitiated();
    //     progressUpdater.registerCompletion(returnFuture);
    //
    //     AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
    //         new CrtResponseFileResponseTransformer<>();
    //
    //     // AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
    //     //     AsyncResponseTransformer.toFile(downloadFileRequestWithAttributes.destination(),
    //     //                                     FileTransformerConfiguration.defaultCreateOrReplaceExisting());
    //
    //     responseTransformer = progressUpdater.wrapResponseTransformer(responseTransformer);
    //
    //     assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");
    //
    //     CompletableFuture<GetObjectResponse> future = s3AsyncClient.getObject(
    //         downloadFileRequestWithAttributes.getObjectRequest(),responseTransformer);
    //
    //     // Forward download cancellation to future
    //     CompletableFutureUtils.forwardExceptionTo(returnFuture, future);
    //
    //     CompletableFutureUtils.forwardTransformedResultTo(future, returnFuture,
    //                                                       res -> CompletedFileDownload.builder()
    //                                     .response(res)
    //                                     .build());
    //     return new DefaultFileDownload(
    //         returnFuture,
    //         progressUpdater.progress(),
    //         () -> downloadFileRequestWithAttributes,
    //         null);
    // }

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

    private GetObjectRequest attachSdkHttpExecutionAttribute(GetObjectRequest getObjectRequest,
                                                   Consumer<SdkHttpExecutionAttributes.Builder> builderMutation) {
        SdkHttpExecutionAttributes modifiedAttributes =
            getObjectRequest.overrideConfiguration().map(o -> o.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES))
                            .map(b -> b.toBuilder().applyMutation(builderMutation).build())
                            .orElseGet(() -> SdkHttpExecutionAttributes.builder().applyMutation(builderMutation).build());

        Consumer<AwsRequestOverrideConfiguration.Builder> attachSdkHttpAttributes =
            b -> b.putExecutionAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES, modifiedAttributes);

        AwsRequestOverrideConfiguration modifiedRequestOverrideConfig =
            getObjectRequest.overrideConfiguration()
                            .map(o -> o.toBuilder().applyMutation(attachSdkHttpAttributes).build())
                            .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                            .applyMutation(attachSdkHttpAttributes)
                                                                            .build());

        return getObjectRequest.toBuilder()
                               .overrideConfiguration(modifiedRequestOverrideConfig)
                               .build();
    }
}
