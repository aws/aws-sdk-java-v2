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

package software.amazon.awssdk.core.internal.interceptor;

import static software.amazon.awssdk.core.ClientType.ASYNC;
import static software.amazon.awssdk.core.ClientType.SYNC;
import static software.amazon.awssdk.core.internal.util.HttpChecksumUtils.getAlgorithmChecksumValuePair;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.async.ChecksumValidatingPublisher;
import software.amazon.awssdk.core.internal.io.ChecksumValidatingInputStream;
import software.amazon.awssdk.core.internal.util.HttpChecksumResolver;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Interceptor to intercepts Sync and Async responses.
 * The Http Checksum is computed and validated with the one that is passed in the header of the response.
 */
@SdkInternalApi
public final class HttpChecksumValidationInterceptor implements ExecutionInterceptor {

    private static final Predicate<ExecutionAttributes> IS_FORCE_SKIPPED_VALIDATION =
        ex -> ChecksumValidation.FORCE_SKIP.equals(
            ex.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null));

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                           ExecutionAttributes executionAttributes) {

        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs != null &&
            isFlexibleChecksumValidationForResponse(executionAttributes, resolvedChecksumSpecs, SYNC)) {

            Pair<Algorithm, String> algorithmChecksumPair = getAlgorithmChecksumValuePair(
                context.httpResponse(), resolvedChecksumSpecs);
            updateContextWithChecksumValidationStatus(executionAttributes, algorithmChecksumPair);

            if (algorithmChecksumPair != null && context.responseBody().isPresent()) {
                return Optional.of(new ChecksumValidatingInputStream(
                    context.responseBody().get(), SdkChecksum.forAlgorithm(algorithmChecksumPair.left()),
                    algorithmChecksumPair.right()));
            }
        }
        return context.responseBody();
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                          ExecutionAttributes executionAttributes) {
        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs != null &&
            isFlexibleChecksumValidationForResponse(executionAttributes, resolvedChecksumSpecs, ASYNC)) {
            Pair<Algorithm, String> algorithmChecksumPair = getAlgorithmChecksumValuePair(context.httpResponse(),
                                                                                          resolvedChecksumSpecs);
            updateContextWithChecksumValidationStatus(executionAttributes, algorithmChecksumPair);
            if (algorithmChecksumPair != null && context.responsePublisher().isPresent()) {
                return Optional.of(new ChecksumValidatingPublisher(context.responsePublisher().get(),
                                                                   SdkChecksum.forAlgorithm(algorithmChecksumPair.left()),
                                                                   algorithmChecksumPair.right()));
            }
        }
        return context.responsePublisher();
    }

    private void updateContextWithChecksumValidationStatus(ExecutionAttributes executionAttributes,
                                                           Pair<Algorithm, String> algorithmChecksumPair) {
        if (algorithmChecksumPair == null || algorithmChecksumPair.left() == null) {
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                             ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        } else {
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                             ChecksumValidation.VALIDATED);
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM,
                                             algorithmChecksumPair.left());
        }
    }

    private boolean isFlexibleChecksumValidationForResponse(ExecutionAttributes executionAttributes,
                                                            ChecksumSpecs checksumSpecs,
                                                            ClientType clientType) {

        return HttpChecksumUtils.isHttpChecksumValidationEnabled(checksumSpecs) &&
               executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE).equals(clientType) &&
               !IS_FORCE_SKIPPED_VALIDATION.test(executionAttributes);
    }
}