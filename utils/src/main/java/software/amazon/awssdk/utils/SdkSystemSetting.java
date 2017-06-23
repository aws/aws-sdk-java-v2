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

package software.amazon.awssdk.utils;

/**
 * System properties to configure the SDK runtime.
 */
public enum SdkSystemSetting implements SystemSetting {

    /**
     * Explicitly identify the default synchronous HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    SYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.service.impl"),

    /**
     * Explicitly identify the default Async HTTP implementation the SDK will use. Useful
     * when there are multiple implementations on the classpath or as a performance optimization
     * since implementation discovery requires classpath scanning.
     */
    ASYNC_HTTP_SERVICE_IMPL("software.amazon.awssdk.http.async.service.impl");

    private final String systemProperty;

    SdkSystemSetting(String systemProperty) {
        this.systemProperty = systemProperty;
    }

    @Override
    public String property() {
        return systemProperty;
    }

    @Override
    public String environmentVariable() {
        return null;
    }

    @Override
    public String defaultValue() {
        return null;
    }
}
