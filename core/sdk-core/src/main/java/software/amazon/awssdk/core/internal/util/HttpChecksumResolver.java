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

package software.amazon.awssdk.core.internal.util;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;

/**
 * Class to resolve the different Checksums specs from ExecutionAttributes.
 */
@SdkInternalApi
public final class HttpChecksumResolver {

    private HttpChecksumResolver() {
    }

    public static ChecksumSpecs getResolvedChecksumSpecs(ExecutionAttributes executionAttributes) {
        ChecksumSpecs checksumSpecs = executionAttributes.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS);
        if (checksumSpecs != null) {
            return checksumSpecs;
        }
        return resolveChecksumSpecs(executionAttributes);
    }

    public static ChecksumSpecs resolveChecksumSpecs(ExecutionAttributes executionAttributes) {
        HttpChecksum httpChecksumTraitInOperation = executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM);
        if (httpChecksumTraitInOperation == null) {
            return null;
        }
        boolean hasRequestValidation = hasRequestValidationMode(httpChecksumTraitInOperation);
        String requestAlgorithm = httpChecksumTraitInOperation.requestAlgorithm();
        String checksumHeaderName = requestAlgorithm != null ? HttpChecksumUtils.httpChecksumHeader(requestAlgorithm) : null;

        return ChecksumSpecs.builder()
                            .algorithmV2(DefaultChecksumAlgorithm.fromValue(requestAlgorithm))
                            .headerName(checksumHeaderName)
                            .responseValidationAlgorithmsV2(httpChecksumTraitInOperation.responseAlgorithmsV2())
                            .isValidationEnabled(hasRequestValidation)
                            .isRequestChecksumRequired(httpChecksumTraitInOperation.isRequestChecksumRequired())
                            .isRequestStreaming(httpChecksumTraitInOperation.isRequestStreaming())
                            .requestAlgorithmHeader(httpChecksumTraitInOperation.requestAlgorithmHeader())
                            .build();
    }

    private static boolean hasRequestValidationMode(HttpChecksum httpChecksum) {
        return httpChecksum.requestValidationMode() != null;
    }
}
