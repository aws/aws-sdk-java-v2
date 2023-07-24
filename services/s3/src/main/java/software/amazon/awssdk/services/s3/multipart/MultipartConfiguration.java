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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 *
 */
@SdkPublicApi
public final class MultipartConfiguration implements ToCopyableBuilder<MultipartConfiguration.Builder, MultipartConfiguration> {
    public static final AttributeMap.Key<MultipartConfiguration> MULTIPART_CONFIGURATION_KEY =
        new AttributeMap.Key<MultipartConfiguration>(MultipartConfiguration.class){};

    private final Boolean multipartEnabled;
    private final Long thresholdInBytes;
    private final Long minimumPartSizeInBytes;
    private final Long maximumMemoryUsageInBytes;
    private final Executor executor;
    private final MultipartDownloadType multipartDownloadType;

    private MultipartConfiguration(DefaultMultipartConfigBuilder builder) {
        this.multipartEnabled = Validate.getOrDefault(builder.multipartEnabled, () -> Boolean.TRUE);
        this.thresholdInBytes = builder.thresholdInBytes;
        this.minimumPartSizeInBytes = builder.minimumPartSizeInBytes;
        this.maximumMemoryUsageInBytes = builder.maximumMemoryUsageInBytes;
        this.multipartDownloadType = builder.multipartDownloadType;
        this.executor = builder.executor;
    }

    public static Builder builder() {
        return new DefaultMultipartConfigBuilder();
    }

    public static MultipartConfiguration create() {
        return builder().build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .executor(executor)
            .minimumPartSizeInBytes(minimumPartSizeInBytes)
            .multipartDownloadType(multipartDownloadType)
            .multipartEnabled(multipartEnabled)
            .thresholdInBytes(thresholdInBytes);
    }

    public Boolean multipartEnabled() {
        return this.multipartEnabled;
    }

    public Long thresholdInBytes() {
        return this.thresholdInBytes;
    }

    public Long minimumPartSizeInBytes() {
        return this.minimumPartSizeInBytes;
    }

    public Long maximumMemoryUsageInBytes() {
        return this.maximumMemoryUsageInBytes;
    }

    public Executor executor() {
        return this.executor;
    }

    public MultipartDownloadType multipartDownloadType() {
        return this.multipartDownloadType;
    }


    public interface Builder extends CopyableBuilder<Builder, MultipartConfiguration> {

        /**
         *
         * @param multipartEnabled
         * @return
         */
        Builder multipartEnabled(Boolean multipartEnabled);

        /**
         *
         * @return
         */
        Boolean multipartEnabled();

        /**
         *
         * @param thresholdInBytes
         * @return
         */
        Builder thresholdInBytes(Long thresholdInBytes);

        /**
         *
         * @return
         */
        Long thresholdInBytes();

        /**
         *
         * @param minimumPartSizeInBytes
         * @return
         */
        Builder minimumPartSizeInBytes(Long minimumPartSizeInBytes);

        /**
         *
         * @return
         */
        Long minimumPartSizeInBytes();

        Builder maximumMemoryUsageInBytes(Long maximumMemoryUsageInBytes);

        /**
         *
         * @return
         */
        Long maximumMemoryUsageInBytes();

        /**
         *
         * @param executor
         * @return
         */
        Builder executor(Executor executor);

        /**
         *
         * @return
         */
        Executor executor();

        /**
         *
         * @param multipartDownloadType
         * @return
         */
        Builder multipartDownloadType(MultipartDownloadType multipartDownloadType);

        /**
         *
         * @return
         */
        MultipartDownloadType multipartDownloadType();
    }

    private static class DefaultMultipartConfigBuilder implements Builder {
        private Boolean multipartEnabled;
        private Long thresholdInBytes;
        private Long minimumPartSizeInBytes;
        private Long maximumMemoryUsageInBytes;
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

        @Override
        public Builder maximumMemoryUsageInBytes(Long maximumMemoryUsageInBytes) {
            this.maximumMemoryUsageInBytes = maximumMemoryUsageInBytes;
            return this;
        }

        @Override
        public Long maximumMemoryUsageInBytes() {
            return maximumMemoryUsageInBytes;
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
