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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.ChecksumConfig;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;

@SdkInternalApi
public final class CrtChecksumUtils {
    private static final ChecksumAlgorithm DEFAULT_CHECKSUM_ALGO = ChecksumAlgorithm.CRC32;

    private CrtChecksumUtils() {
    }

    /**
     * CRT checksum is only enabled for PUT_OBJECT and GET_OBJECT, for everything else,
     * we rely on SDK checksum implementation
     */
    public static ChecksumConfig checksumConfig(HttpChecksum httpChecksum,
                                                 S3MetaRequestOptions.MetaRequestType requestType,
                                                boolean checksumValidationEnabled) {
        if (checksumNotApplicable(requestType, httpChecksum)) {
            return new ChecksumConfig();
        }

        ChecksumAlgorithm checksumAlgorithm =
            crtChecksumAlgorithm(httpChecksum, requestType, checksumValidationEnabled);

        boolean validateChecksum =
            validateResponseChecksum(httpChecksum, requestType,  checksumValidationEnabled);

        ChecksumConfig.ChecksumLocation checksumLocation = checksumAlgorithm == ChecksumAlgorithm.NONE ?
                                                           ChecksumConfig.ChecksumLocation.NONE :
                                                           ChecksumConfig.ChecksumLocation.TRAILER;

        return new ChecksumConfig()
            .withChecksumAlgorithm(checksumAlgorithm)
            .withValidateChecksum(validateChecksum)
            .withChecksumLocation(checksumLocation)
            .withValidateChecksumAlgorithmList(checksumAlgorithmList(httpChecksum));
    }

    private static boolean checksumNotApplicable(S3MetaRequestOptions.MetaRequestType requestType, HttpChecksum httpChecksum) {
        if (requestType != S3MetaRequestOptions.MetaRequestType.PUT_OBJECT &&
            requestType != S3MetaRequestOptions.MetaRequestType.GET_OBJECT) {
            return true;
        }

        return httpChecksum == null;
    }

    private static List<ChecksumAlgorithm> checksumAlgorithmList(HttpChecksum httpChecksum) {
        if (httpChecksum.responseAlgorithms() == null) {
            return null;
        }
        return httpChecksum.responseAlgorithms()
                           .stream()
                           .map(CrtChecksumUtils::toCrtChecksumAlgorithm)
                           .collect(Collectors.toList());
    }

    private static ChecksumAlgorithm crtChecksumAlgorithm(HttpChecksum httpChecksum,
                                                         S3MetaRequestOptions.MetaRequestType requestType,
                                                         boolean checksumValidationEnabled) {
        if (requestType != S3MetaRequestOptions.MetaRequestType.PUT_OBJECT) {
            return ChecksumAlgorithm.NONE;
        }

        if (httpChecksum.requestAlgorithm() == null) {
            return checksumValidationEnabled ? DEFAULT_CHECKSUM_ALGO : ChecksumAlgorithm.NONE;
        }

        return toCrtChecksumAlgorithm(httpChecksum.requestAlgorithm());
    }

    private static ChecksumAlgorithm toCrtChecksumAlgorithm(String sdkChecksum) {
        return ChecksumAlgorithm.valueOf(sdkChecksum.toUpperCase());
    }

    /**
     * Only validate response checksum if this is getObject operation AND it supports checksum validation AND if either of the
     * following applies: 1. checksum validation is enabled at request level via request validation mode OR 2. checksum validation
     * is enabled at client level
     */
    private static boolean validateResponseChecksum(HttpChecksum httpChecksum,
                                                   S3MetaRequestOptions.MetaRequestType requestType,
                                                   boolean checksumValidationEnabled) {
        if (requestType != S3MetaRequestOptions.MetaRequestType.GET_OBJECT) {
            return false;
        }

        return checksumValidationEnabled || httpChecksum.requestValidationMode() != null;
    }
}
