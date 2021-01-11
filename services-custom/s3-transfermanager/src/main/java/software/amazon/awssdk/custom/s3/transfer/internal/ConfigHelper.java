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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.custom.s3.transfer.MultipartDownloadConfiguration;
import software.amazon.awssdk.custom.s3.transfer.TransferOverrideConfiguration;
import software.amazon.awssdk.utils.Validate;

/**
 * Helper class to determine appropriate configuration values based on global
 * settings set on the transfer manager and per request overrides.
 */
@SdkInternalApi
final class ConfigHelper {
    private final MultipartDownloadConfiguration globalDownloadConfig;

    ConfigHelper(MultipartDownloadConfiguration globalDownloadConfig) {
        this.globalDownloadConfig = Validate.notNull(globalDownloadConfig, "globalDownloadConfig must not be null");
    }

    boolean useMultipartDownloads(DownloadRequest downloadRequest) {
        return multipartDownloadConfiguration(downloadRequest).enableMultipartDownloads();
    }

    long multipartDownloadThreshold(DownloadRequest downloadRequest) {
        return multipartDownloadConfiguration(downloadRequest).multipartDownloadThreshold();
    }

    long minDownloadPartSize(DownloadRequest downloadRequest) {
        return multipartDownloadConfiguration(downloadRequest).minDownloadPartSize();
    }

    int maxDownloadPartCount(DownloadRequest downloadRequest) {
        return multipartDownloadConfiguration(downloadRequest).maxDownloadPartCount();
    }

    /**
     * Resolve the correct {@link MultipartDownloadConfiguration} to use for
     * this request. The override set on the request takes precedence, then
     * the config set globally on TransferManager.
     */
    private MultipartDownloadConfiguration multipartDownloadConfiguration(DownloadRequest downloadRequest) {
        return downloadRequest.overrideConfiguration()
                .map(TransferOverrideConfiguration::multipartDownloadConfiguration)
                .orElse(globalDownloadConfig);
    }
}
