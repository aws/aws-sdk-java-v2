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

package software.amazon.awssdk.core.interceptor;

import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32C;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA1;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * Contains attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 */
@SdkPublicApi
public class SdkExecutionAttribute {

    /**
     * Handler context key for advanced configuration.
     */
    public static final ExecutionAttribute<ServiceConfiguration> SERVICE_CONFIG = new ExecutionAttribute<>("ServiceConfig");

    /**
     * The key under which the service name is stored.
     */
    public static final ExecutionAttribute<String> SERVICE_NAME = new ExecutionAttribute<>("ServiceName");

    /**
     * The key under which the time offset (for clock skew correction) is stored.
     */
    public static final ExecutionAttribute<Integer> TIME_OFFSET = new ExecutionAttribute<>("TimeOffset");

    public static final ExecutionAttribute<ClientType> CLIENT_TYPE = new ExecutionAttribute<>("ClientType");

    public static final ExecutionAttribute<String> OPERATION_NAME = new ExecutionAttribute<>("OperationName");

    /**
     * The {@link MetricCollector} associated with the overall API call.
     */
    public static final ExecutionAttribute<MetricCollector> API_CALL_METRIC_COLLECTOR = new ExecutionAttribute<>(
        "ApiCallMetricCollector");

    /**
     * The {@link MetricCollector} associated with the current, ongoing API call attempt. This is not set until the actual
     * internal API call attempt starts.
     */
    public static final ExecutionAttribute<MetricCollector> API_CALL_ATTEMPT_METRIC_COLLECTOR =
        new ExecutionAttribute<>("ApiCallAttemptMetricCollector");

    /**
     * If true indicates that the configured endpoint of the client is a value that was supplied as an override and not
     * generated from regional metadata.
     */
    public static final ExecutionAttribute<Boolean> ENDPOINT_OVERRIDDEN = new ExecutionAttribute<>("EndpointOverridden");

    /**
     * This is the endpointOverride (if {@link #ENDPOINT_OVERRIDDEN} is true), otherwise the endpoint generated from regional
     * metadata.
     */
    public static final ExecutionAttribute<URI> CLIENT_ENDPOINT = new ExecutionAttribute<>("EndpointOverride");

    /**
     * If the client signer value has been overridden.
     */
    public static final ExecutionAttribute<Boolean> SIGNER_OVERRIDDEN = new ExecutionAttribute<>("SignerOverridden");

    /**
     * @deprecated This attribute is used for:
     *             - Set profile file of service endpoint builder docdb, nepture, rds
     * This has been replaced with {@code PROFILE_FILE_SUPPLIER.get()}.
     */
    @Deprecated
    public static final ExecutionAttribute<ProfileFile> PROFILE_FILE = new ExecutionAttribute<>("ProfileFile");

    public static final ExecutionAttribute<Supplier<ProfileFile>> PROFILE_FILE_SUPPLIER =
        new ExecutionAttribute<>("ProfileFileSupplier");

    public static final ExecutionAttribute<String> PROFILE_NAME = new ExecutionAttribute<>("ProfileName");

    /**
     * The checksum algorithm is resolved based on the Request member.
     * The RESOLVED_CHECKSUM_SPECS holds the final checksum which will be used for checksum computation.
     */
    public static final ExecutionAttribute<ChecksumSpecs> RESOLVED_CHECKSUM_SPECS =
        ExecutionAttribute.mappedBuilder("ResolvedChecksumSpecs",
                                         ChecksumSpecs.class,
                                         () -> SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                          .readMapping(SdkExecutionAttribute::signerChecksumReadMapping)
                          .writeMapping(SdkExecutionAttribute::signerChecksumWriteMapping)
                          .build();

    /**
     * The Algorithm used for checksum validation of a response.
     */
    public static final ExecutionAttribute<Algorithm> HTTP_CHECKSUM_VALIDATION_ALGORITHM = new ExecutionAttribute<>(
        "HttpChecksumValidationAlgorithm");

    /**
     * Provides the status of {@link ChecksumValidation} performed on the  response.
     */
    public static final ExecutionAttribute<ChecksumValidation> HTTP_RESPONSE_CHECKSUM_VALIDATION = new ExecutionAttribute<>(
        "HttpResponseChecksumValidation");

    private static final ImmutableMap<Algorithm, ChecksumAlgorithm> CHECKSUM_ALGORITHM_MAP = ImmutableMap.of(
        Algorithm.SHA256, SHA256,
        Algorithm.SHA1, SHA1,
        Algorithm.CRC32, CRC32,
        Algorithm.CRC32C, CRC32C
    );

    private static final ImmutableMap<ChecksumAlgorithm, Algorithm> ALGORITHM_MAP = ImmutableMap.of(
        SHA256, Algorithm.SHA256,
        SHA1, Algorithm.SHA1,
        CRC32, Algorithm.CRC32,
        CRC32C, Algorithm.CRC32C
    );

    protected SdkExecutionAttribute() {
    }

    private static <T extends Identity> ChecksumSpecs signerChecksumReadMapping(ChecksumSpecs checksumSpecs,
                                                                                SelectedAuthScheme<T> authScheme) {
        if (authScheme == null) {
            return null;
        }

        ChecksumAlgorithm checksumAlgorithm =
            authScheme.authSchemeOption().signerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM);

        if (checksumAlgorithm == null) {
            return null;
        }


        return ChecksumSpecs.builder()
                            .algorithm(ALGORITHM_MAP.getOrDefault(checksumAlgorithm, null))
                            .isRequestStreaming(checksumSpecs != null && checksumSpecs.isRequestStreaming())
                            .isRequestChecksumRequired(checksumSpecs != null && checksumSpecs.isRequestChecksumRequired())
                            .isValidationEnabled(checksumSpecs != null && checksumSpecs.isValidationEnabled())
                            .headerName(checksumSpecs != null ? checksumSpecs.headerName() : null)
                            .responseValidationAlgorithms(checksumSpecs != null ? checksumSpecs.responseValidationAlgorithms()
                                                                                : null)
                            .build();
    }

    private static <T extends Identity> SelectedAuthScheme<?> signerChecksumWriteMapping(SelectedAuthScheme<T> authScheme,
                                                                                         ChecksumSpecs checksumSpecs) {
        ChecksumAlgorithm checksumAlgorithm =
            checksumSpecs == null ? null
                                  : CHECKSUM_ALGORITHM_MAP.getOrDefault(checksumSpecs.algorithm(), null);

        if (authScheme == null) {
            // This is an unusual use-case.
            // Let's assume they're setting the checksum-algorithm so that they can call the signer directly. If that's true,
            // then it doesn't really matter what we store other than the checksum-algorithm.
            return new SelectedAuthScheme<>(CompletableFuture.completedFuture(new UnsetIdentity()),
                                            new UnsetHttpSigner(),
                                            AuthSchemeOption.builder()
                                                            .schemeId("unset")
                                                            .putSignerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM,
                                                                               checksumAlgorithm)
                                                            .build());
        }

        return new SelectedAuthScheme<>(authScheme.identity(),
                                        authScheme.signer(),
                                        authScheme.authSchemeOption()
                                                  .copy(o -> o.putSignerProperty(AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM,
                                                                                 checksumAlgorithm)));
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
