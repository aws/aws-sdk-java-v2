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

import java.net.URI;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.profiles.ProfileFile;

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
        new ExecutionAttribute<>("ResolvedChecksumSpecs");

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

    protected SdkExecutionAttribute() {
    }
}
