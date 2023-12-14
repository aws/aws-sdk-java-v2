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

import java.util.Map;

public class EndpointAuthSchemeConfig {

    private String authSchemeStrategyFactoryClass;
    private String knownEndpointProperties;

    private Map<String, KeyTypePair> endpointProviderTestKeys;

    public String getAuthSchemeStrategyFactoryClass() {
        return authSchemeStrategyFactoryClass;
    }

    public void setAuthSchemeStrategyFactoryClass(String authSchemeStrategyFactoryClass) {
        this.authSchemeStrategyFactoryClass = authSchemeStrategyFactoryClass;
    }

    public String getKnownEndpointProperties() {
        return knownEndpointProperties;
    }

    public void setKnownEndpointProperties(String knownEndpointProperties) {
        this.knownEndpointProperties = knownEndpointProperties;
    }

    public Map<String, KeyTypePair> getEndpointProviderTestKeys() {
        return endpointProviderTestKeys;
    }

    public void setEndpointProviderTestKeys(Map<String, KeyTypePair> endpointProviderTestKeys) {
        this.endpointProviderTestKeys = endpointProviderTestKeys;
    }
}
