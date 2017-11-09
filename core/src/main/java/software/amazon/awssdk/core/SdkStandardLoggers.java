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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A centralized set of loggers that used across the SDK to log particular types of events. SDK users can then specifically enable
 * just these loggers to get the type of that they want instead of having to enable all logging.
 */
@SdkInternalApi
public class SdkStandardLoggers {
    /**
     * Logger providing detailed information on requests/responses. Users can enable this logger to get access to AWS request IDs
     * for responses, individual requests and parameters sent to AWS, etc.
     */
    public static final Logger REQUEST_LOGGER = Logger.loggerFor("software.amazon.awssdk.request");

    /**
     * Logger providing request ID information. Users can enable this logger to have just the request IDs logged.
     */
    public static final Logger REQUEST_ID_LOGGER = Logger.loggerFor("software.amazon.awssdk.requestId");

    private SdkStandardLoggers() {}
}
