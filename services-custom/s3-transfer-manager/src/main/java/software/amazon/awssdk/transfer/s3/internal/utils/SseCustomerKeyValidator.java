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

package software.amazon.awssdk.transfer.s3.internal.utils;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Checks that a request about to be resumed carries its SSE-C customer key. The key is not persisted when a resumable
 * transfer is serialized, so a deserialized token that uses SSE-C needs the key supplied again.
 */
@SdkInternalApi
public final class SseCustomerKeyValidator {

    private static final String SSE_CUSTOMER_KEY_HEADER = "x-amz-server-side-encryption-customer-key";

    private SseCustomerKeyValidator() {
    }

    public static void validateUpload(PutObjectRequest putObjectRequest) {
        if (putObjectRequest == null) {
            return;
        }
        validate(putObjectRequest.sseCustomerAlgorithm(),
                 putObjectRequest.sseCustomerKey(),
                 putObjectRequest.overrideConfiguration(),
                 "PutObjectRequest",
                 "ResumableFileUpload");
    }

    public static void validateDownload(GetObjectRequest getObjectRequest) {
        if (getObjectRequest == null) {
            return;
        }
        validate(getObjectRequest.sseCustomerAlgorithm(),
                 getObjectRequest.sseCustomerKey(),
                 getObjectRequest.overrideConfiguration(),
                 "GetObjectRequest",
                 "ResumableFileDownload");
    }

    private static void validate(String sseCustomerAlgorithm,
                                 String sseCustomerKey,
                                 Optional<AwsRequestOverrideConfiguration> overrideConfiguration,
                                 String requestName,
                                 String resumableName) {
        if (sseCustomerAlgorithm == null || sseCustomerKey != null || hasKeyHeader(overrideConfiguration)) {
            return;
        }
        throw new IllegalArgumentException(
            String.format("The %s specifies sseCustomerAlgorithm but no sseCustomerKey. The SSE-C customer key is not "
                          + "persisted when a %s is serialized; set it on the request again before resuming. See %s.",
                          requestName, resumableName, resumableName));
    }

    private static boolean hasKeyHeader(Optional<AwsRequestOverrideConfiguration> overrideConfiguration) {
        return overrideConfiguration.map(c -> c.headers()
                                               .keySet()
                                               .stream()
                                               .anyMatch(SSE_CUSTOMER_KEY_HEADER::equalsIgnoreCase))
                                    .orElse(false);
    }
}
