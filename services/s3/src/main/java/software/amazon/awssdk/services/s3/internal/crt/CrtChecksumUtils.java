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

package software.amazon.awssdk.services.s3.internal.crt;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class CrtChecksumUtils {
    private static final ChecksumAlgorithm DEFAULT_CHECKSUM_ALGO = ChecksumAlgorithm.CRC32;

    private CrtChecksumUtils() {
    }

    public static ChecksumAlgorithm crtChecksumAlgorithm(HttpChecksum httpChecksum,
                                                         S3MetaRequestOptions.MetaRequestType requestType,
                                                         boolean checksumValidationEnabled) {
        if (httpChecksum == null || requestType != S3MetaRequestOptions.MetaRequestType.PUT_OBJECT) {
            return null;
        }

        if (httpChecksum.requestAlgorithm() == null) {
            return checksumValidationEnabled ? DEFAULT_CHECKSUM_ALGO : null;
        }

        return ChecksumAlgorithm.valueOf(httpChecksum.requestAlgorithm().toUpperCase());
    }

    /**
     * Only validate response checksum if this is getObject operation AND it supports checksum validation AND if either of the
     * following applies: 1. checksum validation is enabled at request level via request validation mode OR 2. checksum validation
     * is enabled at client level
     */
    public static boolean validateResponseChecksum(HttpChecksum httpChecksum,
                                                   S3MetaRequestOptions.MetaRequestType requestType,
                                                   boolean checksumValidationEnabled) {
        if (requestType != S3MetaRequestOptions.MetaRequestType.GET_OBJECT || httpChecksum == null) {
            return false;
        }

        return checksumValidationEnabled || httpChecksum.requestValidationMode() != null;
    }
}
