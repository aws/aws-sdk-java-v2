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

import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_MAX_DEPTH;
import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.UPLOAD_DIRECTORY_RECURSIVE;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.UploadDirectoryConfiguration;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal transfer manager related configuration
 */
@SdkInternalApi
public final class TransferConfiguration {
    private final AttributeMap options;

    private TransferConfiguration(AttributeMap mergedOptions) {
        this.options = mergedOptions;
    }

    public AttributeMap options() {
        return options;
    }

    public boolean resolveUploadDirectoryRecursive(UploadDirectoryRequest request) {
        return request.overrideConfiguration()
                      .flatMap(UploadDirectoryConfiguration::recursive)
                      .orElse(options().get(UPLOAD_DIRECTORY_RECURSIVE));
    }

    public boolean resolveUploadDirectoryFollowSymbolicLinks(UploadDirectoryRequest request) {
        return request.overrideConfiguration()
                      .flatMap(UploadDirectoryConfiguration::followSymbolicLinks)
                      .orElse(options().get(UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS));
    }

    public int resolveUploadDirectoryMaxDepth(UploadDirectoryRequest request) {
        return request.overrideConfiguration()
                      .flatMap(UploadDirectoryConfiguration::maxDepth)
                      .orElse(options().get(UPLOAD_DIRECTORY_MAX_DEPTH));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();

        public Builder configuration(UploadDirectoryConfiguration configuration) {
            standardOptions.put(TransferConfigurationOption.UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS,
                                configuration.followSymbolicLinks().orElse(null));
            standardOptions.put(TransferConfigurationOption.UPLOAD_DIRECTORY_MAX_DEPTH, configuration.maxDepth().orElse(null));
            standardOptions.put(TransferConfigurationOption.UPLOAD_DIRECTORY_RECURSIVE, configuration.recursive().orElse(null));
            return this;
        }

        public TransferConfiguration build() {
            AttributeMap mergedOptions = standardOptions.build().merge(TransferConfigurationOption.TRANSFER_DEFAULTS);

            return new TransferConfiguration(mergedOptions);
        }
    }
}
