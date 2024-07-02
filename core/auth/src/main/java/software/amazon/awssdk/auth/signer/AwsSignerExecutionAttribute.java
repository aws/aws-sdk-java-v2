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

import static software.amazon.awssdk.utils.CompletableFutureUtils.joinLikeSync;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * AWS-specific signing attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 *
 * @deprecated Signer execution attributes have been deprecated in favor of signer properties, set on the auth scheme's signer
 * option.
 */
@Deprecated
@SdkProtectedApi
public final class AwsSignerExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The key under which the request credentials are set.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the credential provider via the {@code SdkRequest}'s
     * {@code overrideConfiguration.credentialsProvider}. If you're using it to call the SDK's signers, you should migrate to a
     * subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<AwsCredentials> AWS_CREDENTIALS =
        ExecutionAttribute.derivedBuilder("AwsCredentials",
                                          AwsCredentials.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::awsCredentialsReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::awsCredentialsWriteMapping)
                          .build();

    /**
     * The AWS {@link Region} that is used for signing a request. This is not always same as the region configured on the client
     * for global services like IAM.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the signing region via the {@code AuthSchemeProvider} that
     * is configured on the SDK client builder. If you're using it to call the SDK's signers, you should migrate to a
     * subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Region> SIGNING_REGION =
        ExecutionAttribute.derivedBuilder("SigningRegion",
                                          Region.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::signingRegionReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::signingRegionWriteMapping)
                          .build();

    /**
     * The AWS {@link Region} that is used for signing a request. This is not always same as the region configured on the client
     * for global services like IAM.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the signing region scope via the {@code AuthSchemeProvider}
     * that is configured on the SDK client builder. If you're using it to call the SDK's signers, you should migrate to a
     * subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<RegionScope> SIGNING_REGION_SCOPE =
        ExecutionAttribute.derivedBuilder("SigningRegionScope",
                                          RegionScope.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::signingRegionScopeReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::signingRegionScopeWriteMapping)
                          .build();

    /**
     * The signing name of the service to be using in SigV4 signing
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the signing region name via the {@code AuthSchemeProvider}
     * that is configured on the SDK client builder. If you're using it to call the SDK's signers, you should migrate to a
     * subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<String> SERVICE_SIGNING_NAME =
            ExecutionAttribute.derivedBuilder("ServiceSigningName",
                                              String.class,
                                              SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                              .readMapping(AwsSignerExecutionAttribute::serviceSigningNameReadMapping)
                              .writeMapping(AwsSignerExecutionAttribute::serviceSigningNameWriteMapping)
                              .build();

    /**
     * The key to specify whether to use double url encoding during signing.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the double-url-encode setting via the {@code
     * AuthSchemeProvider} that is configured on the SDK client builder. If you're using it to call the SDK's signers, you
     * should migrate to a subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Boolean> SIGNER_DOUBLE_URL_ENCODE =
        ExecutionAttribute.derivedBuilder("DoubleUrlEncode",
                                          Boolean.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::signerDoubleUrlEncodeReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::signerDoubleUrlEncodeWriteMapping)
                          .build();

    /**
     * The key to specify whether to normalize the resource path during signing.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the normalize-path setting via the {@code
     * AuthSchemeProvider} that is configured on the SDK client builder. If you're using it to call the SDK's signers, you
     * should migrate to a subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Boolean> SIGNER_NORMALIZE_PATH =
        ExecutionAttribute.derivedBuilder("NormalizePath",
                                          Boolean.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::signerNormalizePathReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::signerNormalizePathWriteMapping)
                          .build();


    /**
     * An override clock to use during signing.
     * @see Aws4SignerParams.Builder#signingClockOverride(Clock)
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the clock setting via the {@code
     * AuthSchemeProvider} that is configured on the SDK client builder. If you're using it to call the SDK's signers, you
     * should migrate to a subtype of {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Clock> SIGNING_CLOCK =
        ExecutionAttribute.derivedBuilder("Clock",
                                          Clock.class,
                                          SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(AwsSignerExecutionAttribute::signingClockReadMapping)
                          .writeMapping(AwsSignerExecutionAttribute::signingClockWriteMapping)
                          .build();

    /**
     * The key to specify the expiration time when pre-signing aws requests.
     *
     * @deprecated This is a protected class that is internal to the SDK, so you shouldn't be using it. If you are using it
     * from execution interceptors, you should instead be overriding the expiration via the {@code AuthSchemeProvider} that is
     * configured on the SDK client builder. If you're using it to call the SDK's signers, you should migrate to a subtype of
     * {@code HttpSigner}.
     */
    @Deprecated
    public static final ExecutionAttribute<Instant> PRESIGNER_EXPIRATION = new ExecutionAttribute<>("PresignerExpiration");

    private AwsSignerExecutionAttribute() {
    }

    private static AwsCredentials awsCredentialsReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        Identity identity = joinLikeSync(authScheme.identity());
        if (!(identity instanceof AwsCredentialsIdentity)) {
            return null;
        }
        return CredentialUtils.toCredentials((AwsCredentialsIdentity) identity);
    }

    private static <T extends Identity> SelectedAuthScheme<?> awsCredentialsWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                         AwsCredentials awsCredentials) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting the credentials so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the credentials.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(awsCredentials),
                                            AwsV4HttpSigner.create(),
                                            AuthSchemeOption.builder()
                                                            .schemeId(AwsV4AuthScheme.SCHEME_ID)
                                                            .build());
        }

        return new SelectedAuthScheme<>(CompletableFuture.completedFuture((T) awsCredentials),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption());
    }

    private static Region signingRegionReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        String regionName = authScheme.authSchemeOption().signerProperty(AwsV4HttpSigner.REGION_NAME);
        if (regionName == null) {
            return null;
        }
        return Region.of(regionName);
    }

    private static <T extends Identity> SelectedAuthScheme<?> signingRegionWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                        Region region) {
        String regionString = region == null ? null : region.id();

        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting the region so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the region.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME,
                                                                               regionString)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption().copy(o -> o.putSignerProperty(AwsV4HttpSigner.REGION_NAME,
                                                                                                    regionString)));
    }

    private static RegionScope signingRegionScopeReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        RegionSet regionSet = authScheme.authSchemeOption().signerProperty(AwsV4aHttpSigner.REGION_SET);
        if (regionSet == null || regionSet.asString().isEmpty()) {
            return null;
        }

        return RegionScope.create(regionSet.asString());
    }

    private static <T extends Identity> SelectedAuthScheme<?> signingRegionScopeWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                             RegionScope regionScope) {
        RegionSet regionSet = regionScope != null ? RegionSet.create(regionScope.id()) : null;

        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting the region scope so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the region scope.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption().copy(o -> o.putSignerProperty(AwsV4aHttpSigner.REGION_SET,
                                                                                                    regionSet)));
    }

    private static String serviceSigningNameReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        return authScheme.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME);
    }

    private static <T extends Identity> SelectedAuthScheme<?> serviceSigningNameWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                             String signingName) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting the signing name so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the signing name.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME,
                                                                               signingName)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME,
                                                                                 signingName)));
    }

    private static Boolean signerDoubleUrlEncodeReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        AuthSchemeOption authOption = authScheme.authSchemeOption();
        return authOption.signerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE);
    }

    private static <T extends Identity> SelectedAuthScheme<?> signerDoubleUrlEncodeWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                                Boolean doubleUrlEncode) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting double-url-encode so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than double-url-encode.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE,
                                                                               doubleUrlEncode)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE,
                                                                                 doubleUrlEncode)));
    }

    private static Boolean signerNormalizePathReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        AuthSchemeOption authOption = authScheme.authSchemeOption();
        return authOption.signerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH);
    }

    private static <T extends Identity> SelectedAuthScheme<?> signerNormalizePathWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                              Boolean normalizePath) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting normalize-path so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than normalize-path.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH,
                                                                               normalizePath)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH,
                                                                                 normalizePath)));
    }

    private static Clock signingClockReadMapping(SelectedAuthScheme<?> authScheme) {
        if (authScheme == null) {
            return null;
        }
        return authScheme.authSchemeOption().signerProperty(HttpSigner.SIGNING_CLOCK);
    }

    private static <T extends Identity> SelectedAuthScheme<?> signingClockWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                       Clock clock) {
        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting signing clock so that they can call the signer directly. If that's true, then it
            // doesn't really matter what we store other than the signing clock.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(HttpSigner.SIGNING_CLOCK, clock)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(HttpSigner.SIGNING_CLOCK, clock)));
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
