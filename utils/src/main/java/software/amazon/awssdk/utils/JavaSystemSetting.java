/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * The system properties usually provided by the Java runtime.
 */
@SdkProtectedApi
public enum JavaSystemSetting implements SystemSetting {
    JAVA_VERSION("java.version"),
    JAVA_VENDOR("java.vendor"),
    TEMP_DIRECTORY("java.io.tmpdir"),
    JAVA_VM_NAME("java.vm.name"),
    JAVA_VM_VERSION("java.vm.version"),

    OS_NAME("os.name"),
    OS_VERSION("os.version"),

    USER_HOME("user.home"),
    USER_LANGUAGE("user.language"),
    USER_REGION("user.region"),
    USER_NAME("user.name");

    private final String systemProperty;

    JavaSystemSetting(String systemProperty) {
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
