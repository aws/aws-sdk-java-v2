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


package software.amazon.awssdk.codegen.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.utils.StringUtils;

public class BasicCodeGenConfig {

    private static final String PACKAGE_PREFIX = "software.amazon.awssdk.";

    private final String interfaceName;
    private final String packageName;
    private final String endPoint;
    private final String defaultRegion;

    public BasicCodeGenConfig(
            @JsonProperty(value = "serviceInterfaceName", required = true) String interfaceName,
            @Deprecated @JsonProperty(value = "sourcePackageName") String packageName,
            @JsonProperty(value = "packageSuffix") String packageSuffix,
            @JsonProperty(value = "defaultEndpoint") String endpoint,
            @JsonProperty(value = "defaultRegion") String defaultRegion) {

        this.interfaceName = interfaceName;

        if (packageName != null) {
            if (packageSuffix != null) {
                throw new IllegalArgumentException(
                        "Both packageName and packageSuffix supplied!");
            }
            this.packageName = packageName;
        } else if (packageSuffix != null) {
            this.packageName = PACKAGE_PREFIX + packageSuffix;
        } else {
            String name = interfaceName;
            if (name.startsWith("Amazon")) {
                name = name.substring(6);
            } else if (name.startsWith("AWS")) {
                name = name.substring(3);
            }
            this.packageName = PACKAGE_PREFIX + StringUtils.lowerCase(name);
        }

        this.endPoint = endpoint;
        this.defaultRegion = defaultRegion;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getEndpoint() {
        return endPoint;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }
}
