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

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3Express has different checksum requirements compared to standard S3 calls. This interceptor modifies checksums only
 * for S3Express calls.
 * <p>
 * Checksums can be configured through model traits on operations as follows
 * <ol>
 *     <li><i>httpChecksumRequired</i> - older setting used in S3Control -> not allowed</li>
 *     <li><i>httpChecksum</i> is set and required -> always add CRC32 checksum even if algorithm is not specified.</li>
 *     <li><i>httpChecksum</i> is set but not required -> if algorithm is not specified, behavior differs</li>
 * </ol>
 * <p>Note that, if <i>httpChecksum</i> is not present, no checksum may be calculated. PutBucketPolicy, DeleteObjects are examples
 * of operations that require checksums. PutObject, UploadPart are examples of operations that do not require checksums.
 * <p>
 * Special cases
 * <ul>
 *     <li>PutObject -> always calculate CRC32</li>
 *     <li>UploadPart -> do not calculate CRC32 if algorithm is missing, unless TM is used</li>
 * </ul>
*/
@SdkInternalApi
public final class S3ExpressChecksumInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();
        if (!S3ExpressUtils.useS3Express(executionAttributes)) {
            return request;
        }
        Optional<ChecksumSpecs> resolvedChecksumSpecs =
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS);
        HttpChecksum httpChecksumTraitInOperation = executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM);
        if (!resolvedChecksumSpecs.isPresent()) {
            if (httpChecksumTraitInOperation != null) {
                throw new IllegalStateException("S3Express: illegal checksum parameter combination");
            }
            return request;
        }
        ChecksumSpecs checksumSpecs = resolvedChecksumSpecs.get();
        if (checksumSpecs.algorithm() != null || requestContainsUserCalculatedChecksum(request)) {
            return request;
        }
        if (shouldAlwaysAddChecksum(checksumSpecs, request)) {
            SelectedAuthScheme<Identity> authScheme =
                (SelectedAuthScheme<Identity>) getAuthScheme(executionAttributes);
            AuthSchemeOption authSchemeOption =
                authScheme.authSchemeOption().copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM,
                                                                            DefaultChecksumAlgorithm.CRC32));
            SelectedAuthScheme<Identity> authSchemeWithCrc32 = new SelectedAuthScheme<>(authScheme.identity(),
                                                                                        authScheme.signer(),
                                                                                        authSchemeOption);
            executionAttributes.putAttribute(SELECTED_AUTH_SCHEME, authSchemeWithCrc32);
        }
        return request;
    }

    private boolean requestContainsUserCalculatedChecksum(SdkRequest request) {
        return request.getValueForField("ChecksumCRC32", String.class).isPresent()
               || request.getValueForField("ChecksumCRC32C", String.class).isPresent()
               || request.getValueForField("ChecksumSHA1", String.class).isPresent()
               || request.getValueForField("ChecksumSHA256", String.class).isPresent();
    }

    private boolean shouldAlwaysAddChecksum(ChecksumSpecs checksumSpecs, SdkRequest request) {
        return checksumSpecs.isRequestChecksumRequired() || request instanceof PutObjectRequest;
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        // TODO (s3express) - possibly migrate elsewhere
        // if algorithm is set in modifyRequest(), the marshaller won't add this header, since it checks in the request object
        SdkHttpRequest sdkHttpRequest = context.httpRequest();
        SelectedAuthScheme<?> selectedAuthScheme = getAuthScheme(executionAttributes);
        ChecksumAlgorithm algorithm =
            selectedAuthScheme.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM);
        if (Objects.equals(algorithm, DefaultChecksumAlgorithm.CRC32)) {
            Optional<String> headerValue = getFirstNestedValue(sdkHttpRequest.headers(), "x-amz-sdk-checksum-algorithm");
            if (!headerValue.isPresent()) {
                return sdkHttpRequest.toBuilder().appendHeader("x-amz-sdk-checksum-algorithm", Algorithm.CRC32.name()).build();
            }
        }
        return sdkHttpRequest;
    }

    private SelectedAuthScheme<?> getAuthScheme(ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme == null) {
            throw new IllegalStateException("Auth scheme should not be null");
        }
        return selectedAuthScheme;
    }

    private Optional<String> getFirstNestedValue(Map<String, List<String>> map, String key) {
        List<String> value = map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        String firstValue = value.get(0);
        return firstValue.isEmpty() ? Optional.empty() : Optional.of(firstValue);
    }
}
