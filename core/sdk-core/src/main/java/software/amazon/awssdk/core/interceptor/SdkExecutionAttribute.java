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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;

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
    public static final ExecutionAttribute<Boolean> ENDPOINT_OVERRIDDEN = new ExecutionAttribute<>("EndpointOverride");
    
    protected SdkExecutionAttribute() {
    }
}
