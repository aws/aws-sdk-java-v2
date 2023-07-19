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

package software.amazon.awssdk.services.s3.multipart;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * todo
 */
public final class MultipartConfiguration implements ToCopyableBuilder<MultipartConfiguration.Builder, MultipartConfiguration> {
    private static final Long DEFAULT_MIN_PART_SIZE_IN_BYTES = 8L * 1024 * 1024; // 8 Mib
    private static final Long DEFAULT_THRESHOLD_IN_BYTES = 8L * 1024 * 1024; // 8 Mib

    private final Boolean multipartEnable;
    private final Long thresholdInBytes;
    private final Long minimumPartSizeInBytes;
    private final Executor executor;
    private final MultipartDownloadType multipartDownloadType;

    private MultipartConfiguration(DefaultMultipartConfigBuilder builder) {
        this.multipartEnable = Validate.getOrDefault(builder.multipartEnabled, () -> Boolean.TRUE);
        this.thresholdInBytes = Validate.getOrDefault(builder.thresholdInBytes, () -> DEFAULT_THRESHOLD_IN_BYTES);
        this.minimumPartSizeInBytes = Validate.getOrDefault(builder.minimumPartSizeInBytes, () -> DEFAULT_MIN_PART_SIZE_IN_BYTES);
        this.multipartDownloadType = builder.multipartDownloadType;
        this.executor = Validate.getOrDefault(builder.executor, MultipartConfiguration::defaultExecutor);
    }

    public static Builder builder() {
        return new DefaultMultipartConfigBuilder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .executor(executor)
            .minimumPartSizeInBytes(minimumPartSizeInBytes)
            .multipartDownloadType(multipartDownloadType)
            .multipartEnabled(multipartEnable)
            .thresholdInBytes(thresholdInBytes);
    }

    private static Executor defaultExecutor() {
        int maxPoolSize = 100;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, maxPoolSize,
                                                             60, TimeUnit.SECONDS,
                                                             new LinkedBlockingQueue<>(1_000),
                                                             new ThreadFactoryBuilder()
                                                                 .threadNamePrefix("s3-multipart-client").build());
        // Allow idle core threads to time out
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public interface Builder extends CopyableBuilder<Builder, MultipartConfiguration> {

        /**
         *
         * @param multipartEnabled
         * @return
         */
        Builder multipartEnabled(Boolean multipartEnabled);
        Boolean multipartEnabled();

        /**
         *
         * @param thresholdInBytes
         * @return
         */
        Builder thresholdInBytes(Long thresholdInBytes);
        Long thresholdInBytes();

        /**
         *
         * @param minimumPartSizeInBytes
         * @return
         */
        Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes);
        Long minimumPartSizeInBytes();

        /**
         *
         * @param executor
         * @return
         */
        Builder executor(Executor executor);
        Executor executor();

        /**
         *
         * @param multipartDownloadType
         * @return
         */
        Builder multipartDownloadType(MultipartDownloadType multipartDownloadType);
        MultipartDownloadType multipartDownloadType();
    }

    private static class DefaultMultipartConfigBuilder implements Builder {
        private Boolean multipartEnabled;
        private Long thresholdInBytes;
        private Long minimumPartSizeInBytes;
        private Executor executor;
        private MultipartDownloadType multipartDownloadType;

        public Builder multipartEnabled(Boolean multipartEnabled) {
            this.multipartEnabled = multipartEnabled;
            return this;
        }

        public Boolean multipartEnabled() {
            return this.multipartEnabled;
        }

        public Builder thresholdInBytes(Long thresholdInBytes) {
            this.thresholdInBytes = thresholdInBytes;
            return this;
        }

        public Long thresholdInBytes() {
            return this.thresholdInBytes;
        }

        public Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes) {
            this.minimumPartSizeInBytes = minimumPartSizeInBytes;
            return this;
        }

        public Long minimumPartSizeInBytes() {
            return this.minimumPartSizeInBytes;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Executor executor() {
            return this.executor;
        }

        public Builder multipartDownloadType(MultipartDownloadType multipartDownloadType) {
            this.multipartDownloadType = multipartDownloadType;
            return this;
        }

        public MultipartDownloadType multipartDownloadType() {
            return this.multipartDownloadType;
        }

        @Override
        public MultipartConfiguration build() {
            return new MultipartConfiguration(this);
        }
    }
}
