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

package software.amazon.awssdk.auth.signer;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * S3-specific signing attributes attached to the execution.
 *
 * @deprecated Signer execution attributes have been deprecated in favor of signer properties, set on the auth scheme's signer
 * option.
 */
@SdkProtectedApi
@Deprecated
public final class S3SignerExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The key to specify whether to enable chunked encoding or not
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the chunk encoding setting via the {@code AuthSchemeProvider}
     * that is configured on the SDK client builder. If you're using it to call the SDK's signers, you should migrate to a
     * subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Boolean> ENABLE_CHUNKED_ENCODING =
        ExecutionAttribute.derivedBuilder("ChunkedEncoding",
                                          Boolean.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(S3SignerExecutionAttribute::enableChunkedEncodingReadMapping)
                          .writeMapping(S3SignerExecutionAttribute::enableChunkedEncodingWriteMapping)
                          .build();


    /**
     * The key to specify whether to enable payload signing or not
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the payload signing setting via the {@code
     * AuthSchemeProvider} that is configured on the SDK client builder. If you're using it to call the SDK's signers, you
     * should migrate to a subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Boolean> ENABLE_PAYLOAD_SIGNING =
        ExecutionAttribute.derivedBuilder("PayloadSigning",
                                          Boolean.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(S3SignerExecutionAttribute::enablePayloadSigningReadMapping)
                          .writeMapping(S3SignerExecutionAttribute::enablePayloadSigningWriteMapping)
                          .build();

    private S3SignerExecutionAttribute() {
    }

    private static Boolean enableChunkedEncodingReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        AuthSchemeOption authOption = authScheme.authSchemeOption();
        return authOption.signerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED);
    }

    private static <T extends Identity> SelectedAuthScheme<?> enableChunkedEncodingWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                                Boolean enableChunkedEncoding) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting normalize-path so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than normalize-path.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED,
                                                                               enableChunkedEncoding)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED,
                                                                                 enableChunkedEncoding)));
    }

    private static Boolean enablePayloadSigningReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        return authScheme.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED);
    }

    private static <T extends Identity> SelectedAuthScheme<?> enablePayloadSigningWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                               Boolean payloadSigningEnabled) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're configuring payload signing so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the payload signing setting.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED,
                                                                               payloadSigningEnabled)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED,
                                                                                 payloadSigningEnabled))
        );
    }

    private static class UnsetIdentity implements Identity {
    }

    private static class UnsetHttpSigner implements HttpSigner<UnsetIdentity> {
        @Override
        public SignedRequest sign(SignRequest<? extends UnsetIdentity> request) {
            throw new IllegalStateException("A signer was not configured.");
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends UnsetIdentity> request) {
            return CompletableFutureUtils.failedFuture(new IllegalStateException("A signer was not configured."));
        }
    }
}
