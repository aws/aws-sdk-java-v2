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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.services.s3.internal.handlers.LegacyMd5ExecutionInterceptor;


/**
 * Plugin that enables legacy MD5 checksum behavior for S3 operations.
 *
 * <p>This plugin configures the S3 client to:
 * <ul>
 *   <li>Set request checksum calculation to WHEN_REQUIRED mode, which calculates default checksums only when
 *       required by the S3 service for specific operations</li>
 *   <li>Set response checksum validation to WHEN_REQUIRED mode, which validates checksums only when
 *       the S3 service provides them in responses</li>
 *   <li>Add an interceptor that maintains backward compatibility with older SDK versions
 *       that automatically calculated MD5 checksums for certain operations</li>
 * </ul>
 *
 * <p>Use this plugin only when you need to maintain compatibility with applications that depend on the
 * legacy MD5 checksum behavior, particularly for operations that previously calculated MD5 checksums
 * automatically.
 *
 * @see RequestChecksumCalculation
 * @see ResponseChecksumValidation
 */

@SdkPublicApi
public final class LegacyMd5Plugin implements SdkPlugin {

    private LegacyMd5Plugin() {
    }

    public static SdkPlugin create() {
        return new LegacyMd5Plugin();
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;
        s3Config.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        s3Config.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
        s3Config.overrideConfiguration(s3Config.overrideConfiguration()
                                               .toBuilder()
                                               .addExecutionInterceptor(LegacyMd5ExecutionInterceptor.create())
                                               .build());
    }
}