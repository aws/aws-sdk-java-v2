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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class S3ChecksumUtils {
    private static final Logger log = Logger.loggerFor(S3ChecksumUtils.class);

    private S3ChecksumUtils() {

    }

    public static boolean checksumValueSpecified(SdkRequest request) {
        for (ChecksumAlgorithm algorithm : ChecksumAlgorithm.knownValues()) {
            String s = "Checksum" + algorithm;
            if (request.getValueForField(s, String.class).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<ChecksumAlgorithm> checksumAlgorithmFromPutObjectRequest(PutObjectRequest request) {

        if (request.checksumAlgorithm() != null) {
            return Optional.of(request.checksumAlgorithm());
        }

        try {
            // Remove prefix "Checksum"
            for (SdkField<?> field : request.sdkFields()) {
                if (field.memberName().startsWith("Checksum")) {
                    if (request.getValueForField(field.memberName(), String.class).isPresent()) {
                        String substring = field.memberName().substring(8);
                        return Optional.of(ChecksumAlgorithm.fromValue(substring));
                    }
                }
            }
        } catch (Exception e) {
            log.debug(() -> "Failed to retrieve checksum algorithm from PutObjectRequest, will use the SDK default checksum "
                            + "algorithm", e);
            return Optional.empty();
        }

        return Optional.empty();
    }
}