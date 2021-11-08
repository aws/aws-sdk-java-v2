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

package software.amazon.awssdk.codegen.model.config.customization;

public class ServiceConfig {
    /**
     * Specifies the name of the client configuration class to use if a service
     * has a specific advanced client configuration class. Null if the service
     * does not have advanced configuration.
     */
    private String className;

    /**
     * Whether the service config has a property used to manage dualstack (should be deprecated in favor of
     * AwsClientBuilder#dualstackEnabled).
     */
    private boolean hasDualstackProperty = false;

    /**
     * Whether the service config has a property used to manage FIPS (should be deprecated in favor of
     * AwsClientBuilder#fipsEnabled).
     */
    private boolean hasFipsProperty = false;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean hasDualstackProperty() {
        return hasDualstackProperty;
    }

    public void setHasDualstackProperty(boolean hasDualstackProperty) {
        this.hasDualstackProperty = hasDualstackProperty;
    }

    public boolean hasFipsProperty() {
        return hasFipsProperty;
    }

    public void setHasFipsProperty(boolean hasFipsProperty) {
        this.hasFipsProperty = hasFipsProperty;
    }
}
