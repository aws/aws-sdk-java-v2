/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.core.RequestOverrideConfig;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.runtime.auth.Signer;

/**
 * Contains attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 */
public class SdkExecutionAttributes {

    /**
     * The key under which the request config is stored.
     */
    public static final ExecutionAttribute<RequestOverrideConfig> REQUEST_CONFIG = new ExecutionAttribute<>("RequestConfig");

    /**
     * Handler context key for advanced configuration.
     */
    public static final ExecutionAttribute<ServiceAdvancedConfiguration> SERVICE_ADVANCED_CONFIG =
        new ExecutionAttribute<>("ServiceAdvancedConfig");

    /**
     * The key under which the service name is stored.
     */
    public static final ExecutionAttribute<String> SERVICE_NAME = new ExecutionAttribute<>("ServiceName");

    /**
     * The key under which the time offset (for clock skew correction) is stored.
     */
    public static final ExecutionAttribute<Integer> TIME_OFFSET = new ExecutionAttribute<>("TimeOffset");

    protected SdkExecutionAttributes() {
    }
}
