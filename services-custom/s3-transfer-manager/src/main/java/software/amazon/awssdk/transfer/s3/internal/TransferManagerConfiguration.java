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

import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.TRANSFER_MANAGER_DEFAULTS;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_MAX_DEPTH;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.ExecutorUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Contains resolved configuration settings for {@link S3TransferManager}.
 * This configuration object can be {@link #close()}d to release all closeable resources configured within it.
 */
@SdkInternalApi
public class TransferManagerConfiguration implements SdkAutoCloseable {
    private final AttributeMap options;

    private TransferManagerConfiguration(Builder builder) {
        AttributeMap.Builder standardOptions = AttributeMap.builder();
        standardOptions.put(UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS, builder.uploadDirectoryFollowSymbolicLinks);
        standardOptions.put(UPLOAD_DIRECTORY_MAX_DEPTH, builder.uploadDirectoryMaxDepth);
        finalizeExecutor(builder, standardOptions);
        options = standardOptions.build().merge(TRANSFER_MANAGER_DEFAULTS);
    }

    private void finalizeExecutor(Builder builder, AttributeMap.Builder standardOptions) {
        if (builder.executor != null) {
            standardOptions.put(TransferConfigurationOption.EXECUTOR, ExecutorUtils.unmanagedExecutor(builder.executor));
        } else {

            standardOptions.put(TransferConfigurationOption.EXECUTOR, defaultExecutor());
        }
    }

    /**
     * Retrieve the value of a specific option.
     */
    public <T> T option(TransferConfigurationOption<T> option) {
        return options.get(option);
    }

    public boolean resolveUploadDirectoryFollowSymbolicLinks(UploadDirectoryRequest request) {
        return request.followSymbolicLinks()
                      .orElseGet(() -> options.get(UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS));
    }

    public int resolveUploadDirectoryMaxDepth(UploadDirectoryRequest request) {
        return request.maxDepth()
                      .orElseGet(() -> options.get(UPLOAD_DIRECTORY_MAX_DEPTH));
    }

    @Override
    public void close() {
        options.close();
    }

    private Executor defaultExecutor() {
        int maxPoolSize = 100;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, maxPoolSize,
                                                             60, TimeUnit.SECONDS,
                                                             new LinkedBlockingQueue<>(1_000),
                                                             new ThreadFactoryBuilder()
                                                                 .threadNamePrefix("s3-transfer-manager").build());
        // Allow idle core threads to time out
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Boolean uploadDirectoryFollowSymbolicLinks;
        private Integer uploadDirectoryMaxDepth;
        private Executor executor;


        public Builder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks) {
            this.uploadDirectoryFollowSymbolicLinks = uploadDirectoryFollowSymbolicLinks;
            return this;
        }

        public Builder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth) {
            this.uploadDirectoryMaxDepth = uploadDirectoryMaxDepth;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public TransferManagerConfiguration build() {
            return new TransferManagerConfiguration(this);
        }
    }
}
