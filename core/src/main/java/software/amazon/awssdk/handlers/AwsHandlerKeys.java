/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.handlers;

import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.http.HandlerContextKey;

public class AwsHandlerKeys {

    /**
     * The key under which the request credentials are set.
     */
    public static final HandlerContextKey<AwsCredentials> AWS_CREDENTIALS =
            new HandlerContextKey<>(AwsCredentials.class, "AWSCredentials");

    /**
     * The key under which the request config is stored.
     */
    public static final HandlerContextKey<RequestConfig> REQUEST_CONFIG =
            new HandlerContextKey<>(RequestConfig.class, "RequestConfig");

    /**
     * The key under which the service name is stored.
     */
    public static final HandlerContextKey<String> SERVICE_NAME =
            new HandlerContextKey<>(String.class, "ServiceName");

    /**
     * The key under which the time offset (for clock skew correction) is stored.
     */
    public static final HandlerContextKey<Integer> TIME_OFFSET =
            new HandlerContextKey<>(Integer.class, "TimeOffset");

    /**
     * Handler context key for advanced configuration.
     */
    public static final HandlerContextKey<ServiceAdvancedConfiguration> SERVICE_ADVANCED_CONFIG =
            new HandlerContextKey<>(ServiceAdvancedConfiguration.class, "ServiceConfig");
}
