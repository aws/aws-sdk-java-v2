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

package software.amazon.awssdk.services.s3.internal.handlers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class LegacyMd5ExecutionInterceptor implements ExecutionInterceptor {

    public static final String HTTP_CHECKSUM_HEADER_PREFIX = "x-amz-checksum";

    private LegacyMd5ExecutionInterceptor() {
    }

    public static LegacyMd5ExecutionInterceptor create() {
        return new LegacyMd5ExecutionInterceptor();
    }

    public static boolean isMd5ChecksumRequired(ExecutionAttributes executionAttributes) {
        ChecksumSpecs resolvedChecksumSpecs = getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs == null) {
            return false;
        }
        return resolvedChecksumSpecs.algorithm() == null && resolvedChecksumSpecs.isRequestChecksumRequired();
    }

    public static ChecksumSpecs getResolvedChecksumSpecs(ExecutionAttributes executionAttributes) {
        ChecksumSpecs checksumSpecs = executionAttributes.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS);
        return checksumSpecs != null ? checksumSpecs : resolveChecksumSpecs(executionAttributes);
    }

    public static ChecksumSpecs resolveChecksumSpecs(ExecutionAttributes executionAttributes) {
        HttpChecksum httpChecksumTraitInOperation = executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM);
        if (httpChecksumTraitInOperation == null) {
            return null;
        }
        boolean hasRequestValidation = httpChecksumTraitInOperation.requestValidationMode() != null;
        String requestAlgorithm = httpChecksumTraitInOperation.requestAlgorithm();
        String checksumHeaderName = requestAlgorithm != null ? httpChecksumHeader(requestAlgorithm) : null;
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

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        boolean isHttpChecksumRequired = isHttpChecksumRequired(executionAttributes);
        boolean requestAlreadyHasMd5 = context.httpRequest().firstMatchingHeader(Header.CONTENT_MD5).isPresent();

        Optional<RequestBody> syncContent = context.requestBody();
        Optional<AsyncRequestBody> asyncContent = context.asyncRequestBody();

        if (!isHttpChecksumRequired || requestAlreadyHasMd5) {
            return context.httpRequest();
        }

        if (asyncContent.isPresent()) {
            throw new IllegalStateException("This operation requires a content-MD5 checksum, " +
                                            "but one cannot be calculated for non-blocking content.");
        }

        if (syncContent.isPresent()) {
            try {
                String payloadMd5 = Md5Utils.md5AsBase64(syncContent.get().contentStreamProvider().newStream());
                return context.httpRequest().copy(r -> r.putHeader(Header.CONTENT_MD5, payloadMd5));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return context.httpRequest();
    }

    private boolean isHttpChecksumRequired(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED) != null
               || isMd5ChecksumRequired(executionAttributes);
    }

    private static String httpChecksumHeader(String algorithmName) {
        return HTTP_CHECKSUM_HEADER_PREFIX + "-" + StringUtils.lowerCase(algorithmName);
    }
}