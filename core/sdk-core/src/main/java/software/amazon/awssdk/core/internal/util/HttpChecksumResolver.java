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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Class to resolve the different Checksums specs from ExecutionAttributes.
 */
@SdkInternalApi
public final class HttpChecksumResolver {

    private HttpChecksumResolver() {
    }

    public static ChecksumSpecs getResolvedChecksumSpecs(ExecutionAttributes executionAttributes) {
        return Optional.ofNullable(executionAttributes.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS))
                       .orElseGet(() -> resolveChecksumSpecs(executionAttributes));
    }

    public static ChecksumSpecs resolveChecksumSpecs(ExecutionAttributes executionAttributes) {
        HttpChecksum httpChecksumTraitInOperation = executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM);
        if (httpChecksumTraitInOperation == null) {
            return null;
        }
        boolean hasRequestValidation = hasRequestValidationMode(httpChecksumTraitInOperation);
        String checksumHeaderName = httpChecksumTraitInOperation.requestAlgorithm() != null ?
            HttpChecksumUtils.httpChecksumHeader(httpChecksumTraitInOperation.requestAlgorithm())
                                    : null;
        List<Algorithm> responseValidationAlgorithms = getResponseValidationAlgorithms(httpChecksumTraitInOperation);

        return ChecksumSpecs.builder()
                            .algorithm(Algorithm.fromValue(httpChecksumTraitInOperation.requestAlgorithm()))
                            .headerName(checksumHeaderName)
                            .responseValidationAlgorithms(responseValidationAlgorithms)
                            .isValidationEnabled(hasRequestValidation)
                            .isRequestChecksumRequired(httpChecksumTraitInOperation.isRequestChecksumRequired())
                            .isRequestStreaming(httpChecksumTraitInOperation.isRequestStreaming())
                            .build();
    }

    private static boolean hasRequestValidationMode(HttpChecksum httpChecksum) {
        return httpChecksum.requestValidationMode() != null;
    }

    private static List<Algorithm> getResponseValidationAlgorithms(HttpChecksum httpChecksumTraitInOperation) {
        List<Algorithm> responseValidationAlgorithms = null;

        if (httpChecksumTraitInOperation.responseAlgorithms() != null &&
            !httpChecksumTraitInOperation.responseAlgorithms().isEmpty()) {
            responseValidationAlgorithms =
                httpChecksumTraitInOperation.responseAlgorithms().stream().filter(StringUtils::isNotBlank)
                                            .map(StringUtils::trim)
                                            .map(Algorithm::fromValue)
                                            .collect(Collectors.toList());
        }
        return responseValidationAlgorithms;
    }
}
