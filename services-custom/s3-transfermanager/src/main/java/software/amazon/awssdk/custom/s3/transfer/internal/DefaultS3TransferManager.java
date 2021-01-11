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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.Download;
import software.amazon.awssdk.custom.s3.transfer.DownloadDirectory;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.custom.s3.transfer.DownloadState;
import software.amazon.awssdk.custom.s3.transfer.MultipartDownloadConfiguration;
import software.amazon.awssdk.custom.s3.transfer.MultipartUploadConfiguration;
import software.amazon.awssdk.custom.s3.transfer.S3TransferManager;
import software.amazon.awssdk.custom.s3.transfer.TransferProgressListener;
import software.amazon.awssdk.custom.s3.transfer.Upload;
import software.amazon.awssdk.custom.s3.transfer.UploadDirectory;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.custom.s3.transfer.UploadState;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Default implementation of the {@link S3TransferManager}.
 */
@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3AsyncClient s3Client;
    private final boolean manageS3Client;
    private final Integer maxConcurrency;
    private final Long maxUploadBytesPerSecond;
    private final Long maxDownloadBytesPerSecond;
    private final MultipartDownloadConfiguration multipartDownloadConfiguration;
    private final MultipartUploadConfiguration multipartUploadConfiguration;
    private final List<TransferProgressListener> progressListeners;

    private final DownloadManager downloadManager;
    private final UploadManager uploadManager;

    private DefaultS3TransferManager(BuilderImpl builder) {
        if (builder.s3Client != null) {
            this.s3Client = builder.s3Client;
            this.manageS3Client = false;
        } else {
            this.s3Client = S3AsyncClient.create();
            this.manageS3Client = true;
        }

        this.maxConcurrency = builder.maxConcurrency;
        this.maxUploadBytesPerSecond = builder.maxUploadBytesPerSecond;
        this.maxDownloadBytesPerSecond = builder.maxDownloadBytesPerSecond;
        this.multipartDownloadConfiguration = resolveMultipartDownloadConfiguration(builder.multipartDownloadConfiguration);
        this.multipartUploadConfiguration = resolveMultipartUploadConfiguration(builder.multipartUploadConfiguration);
        this.progressListeners = resolveProgressListeners(builder.progressListeners);

        //TODO should we use builder pattern in DownloadManager too?
        this.downloadManager = new DownloadManager(this.s3Client, this.multipartDownloadConfiguration);
        this.uploadManager = UploadManager.builder()
                                          .configuration(this.multipartUploadConfiguration)
                                          .s3Client(this.s3Client)
                                          .build();
    }

    @Override
    public Download download(DownloadRequest request, Path file) {
        return download(request, TransferResponseTransformer.forFile(file));
    }

    @Override
    public Download resumeDownload(DownloadState downloadState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DownloadDirectory downloadDirectory(String bucket, String prefix, Path destinationDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Upload upload(UploadRequest request, Path file) {
        return upload(request, TransferRequestBody.fromFile(file));
    }

    @Override
    public Upload resumeUpload(UploadState uploadState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadDirectory uploadDirectory(String bucket, String prefix, Path sourceDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public void close() {
        if (manageS3Client) {
            s3Client.close();
        }
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    private Upload upload(UploadRequest request, TransferRequestBody requestBody) {
        return uploadManager.uploadObject(request, requestBody);
    }

    private Download download(DownloadRequest request, TransferResponseTransformer responseTransformer) {
        return downloadManager.downloadObject(request, responseTransformer);
    }

    private static MultipartDownloadConfiguration resolveMultipartDownloadConfiguration(
            MultipartDownloadConfiguration configured) {
        if (configured != null) {
            return configured;
        }
        return MultipartDownloadConfiguration.defaultConfig();
    }

    private static MultipartUploadConfiguration resolveMultipartUploadConfiguration(MultipartUploadConfiguration configured) {
        if (configured != null) {
            return configured;
        }
        return MultipartUploadConfiguration.defaultConfig();
    }

    private static List<TransferProgressListener> resolveProgressListeners(List<TransferProgressListener> configured) {
        if (configured != null) {
            return configured;
        }
        return Collections.emptyList();
    }

    private static class BuilderImpl implements S3TransferManager.Builder {
        private S3AsyncClient s3Client;
        private Integer maxConcurrency;
        private Long maxUploadBytesPerSecond;
        private Long maxDownloadBytesPerSecond;
        private MultipartDownloadConfiguration multipartDownloadConfiguration;
        private MultipartUploadConfiguration multipartUploadConfiguration;
        private List<TransferProgressListener> progressListeners;

        private BuilderImpl(DefaultS3TransferManager transferManager) {
            this.s3Client = transferManager.s3Client;
            this.maxConcurrency = transferManager.maxConcurrency;
            this.maxDownloadBytesPerSecond = transferManager.maxDownloadBytesPerSecond;
            this.maxUploadBytesPerSecond = transferManager.maxUploadBytesPerSecond;
            this.multipartDownloadConfiguration = transferManager.multipartDownloadConfiguration;
            this.multipartUploadConfiguration = transferManager.multipartUploadConfiguration;
            this.progressListeners = new ArrayList<>(transferManager.progressListeners);
        }

        private BuilderImpl() {
        }

        @Override
        public S3TransferManager.Builder s3client(S3AsyncClient s3Client) {
            this.s3Client = s3Client;
            return this;
        }

        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public Builder maxUploadBytesPerSecond(Long maxUploadBytesPerSecond) {
            this.maxUploadBytesPerSecond = maxUploadBytesPerSecond;
            return this;
        }

        @Override
        public Builder maxDownloadBytesPerSecond(Long maxDownloadBytesPerSecond) {
            this.maxDownloadBytesPerSecond = maxDownloadBytesPerSecond;
            return this;
        }

        @Override
        public Builder multipartDownloadConfiguration(MultipartDownloadConfiguration multipartDownloadConfiguration) {
            this.multipartDownloadConfiguration = multipartDownloadConfiguration;
            return this;
        }

        @Override
        public Builder multipartUploadConfiguration(MultipartUploadConfiguration multipartUploadConfiguration) {
            this.multipartUploadConfiguration = multipartUploadConfiguration;
            return this;
        }

        @Override
        public S3TransferManager.Builder addProgressListener(TransferProgressListener progressListener) {
            if (this.progressListeners == null) {
                this.progressListeners = new ArrayList<>();
            }
            this.progressListeners.add(progressListener);
            return this;
        }

        @Override
        public S3TransferManager.Builder progressListeners(Collection<? extends TransferProgressListener> progressListeners) {
            this.progressListeners = new ArrayList<>(progressListeners);
            return this;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
