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

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.Logger;


/**
 * An {@link S3TransferManager} factory that instantiate an {@link S3TransferManager} implementation based on the underlying
 * {@link S3AsyncClient}.
 */
@SdkInternalApi
public final class TransferManagerFactory {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private TransferManagerFactory() {
    }

    public static S3TransferManager createTransferManager(DefaultBuilder tmBuilder) {
        TransferManagerConfiguration transferConfiguration = resolveTransferManagerConfiguration(tmBuilder);
        S3AsyncClient s3AsyncClient;
        boolean isDefaultS3AsyncClient;
        if (tmBuilder.s3AsyncClient == null) {
            isDefaultS3AsyncClient = true;
            s3AsyncClient = defaultS3AsyncClient().get();
        } else {
            isDefaultS3AsyncClient = false;
            s3AsyncClient = tmBuilder.s3AsyncClient;
        }

        if (s3AsyncClient instanceof S3CrtAsyncClient) {
            return new CrtS3TransferManager(transferConfiguration, s3AsyncClient, isDefaultS3AsyncClient);
        }

        if (s3AsyncClient.getClass().getName().equals("software.amazon.awssdk.services.s3.DefaultS3AsyncClient")) {
            log.warn(() -> "The provided DefaultS3AsyncClient is not an instance of S3CrtAsyncClient, and thus multipart"
                           + " upload/download feature is not enabled and resumable file upload is not supported. To benefit "
                           + "from maximum throughput, consider using S3AsyncClient.crtBuilder().build() instead.");
        } else {
            log.debug(() -> "The provided S3AsyncClient is not an instance of S3CrtAsyncClient, and thus multipart"
                            + " upload/download feature may not be enabled and resumable file upload may not be supported.");
        }

        return new GenericS3TransferManager(transferConfiguration, s3AsyncClient, isDefaultS3AsyncClient);
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

    public static final class DefaultBuilder implements S3TransferManager.Builder {
        private S3AsyncClient s3AsyncClient;
        private Executor executor;
        private Boolean uploadDirectoryFollowSymbolicLinks;
        private Integer uploadDirectoryMaxDepth;

        @Override
        public DefaultBuilder s3Client(S3AsyncClient s3AsyncClient) {
            this.s3AsyncClient = s3AsyncClient;
            return this;
        }

        @Override
        public DefaultBuilder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public DefaultBuilder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks) {
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
        public DefaultBuilder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth) {
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
            return createTransferManager(this);
        }
    }
}
