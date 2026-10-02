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
 * <p>This plugin configures the S3 client to add an interceptor that maintains backward compatibility
 * with older SDK versions that automatically calculated MD5 checksums for certain operations.</p>
 *
 * <p>Use this plugin only when you need to maintain compatibility with applications that depend on the
 * legacy MD5 checksum behavior, particularly for operations that previously calculated MD5 checksums
 * automatically.</p>
 *
 * <p><b>Example usage:</b></p>
 *
 * {@snippet :
 * // For synchronous S3 client
 * S3Client s3Client = S3Client.builder()
 *                             .addPlugin(LegacyMd5Plugin.create())
 *                             .build();
 *
 * // For asynchronous S3 client
 * S3AsyncClient asyncClient = S3AsyncClient.builder()
 *                                          .addPlugin(LegacyMd5Plugin.create())
 *                                          .build();
 * }
 *
 * <p>If you want to add MD5 checksums to the operations that require checksums and want
 * to skip adding of SDK Default checksums for operations that support checksums but not required,
 * then you can enable ClientBuilder options requestChecksumCalculation and responseChecksumValidation
 * as WHEN_REQUIRED, this will add SDK default checksums only to operation that required checksums</p>
 *
 * {@snippet :
 *
 * // Use LegacyMd5Plugin with requestChecksumCalculation and responseChecksumValidation set to WHEN_REQUIRED
 * S3AsyncClient asyncClient = S3AsyncClient.builder()
 *                                          .addPlugin(LegacyMd5Plugin.create())
 *                                          .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
 *                                          .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
 *                                          .build();
 * }
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
        if (!(config instanceof S3ServiceClientConfiguration.Builder)) {
            return;
        }
        S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;
        s3Config.overrideConfiguration(s3Config.overrideConfiguration()
                                               .toBuilder()
                                               .addExecutionInterceptor(LegacyMd5ExecutionInterceptor.create())
                                               .build());
    }
}