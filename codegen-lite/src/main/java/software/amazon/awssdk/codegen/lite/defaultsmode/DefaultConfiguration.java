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

package software.amazon.awssdk.codegen.lite.defaultsmode;

import java.util.Map;

/**
 * Container for default configuration
 */
public class DefaultConfiguration {
    /**
     * The transformed configuration values for each mode
     */
    private Map<String, Map<String, String>> modeDefaults;

    /**
     * The documentation for each mode
     */
    private Map<String, String> modesDocumentation;

    /*
     * The documentation for each configuration option
     */
    private Map<String, String> configurationDocumentation;

    public Map<String, Map<String, String>> modeDefaults() {
        return modeDefaults;
    }

    public DefaultConfiguration modeDefaults(Map<String, Map<String, String>> modeDefaults) {
        this.modeDefaults = modeDefaults;
        return this;
    }

    public Map<String, String> modesDocumentation() {
        return modesDocumentation;
    }

    public DefaultConfiguration modesDocumentation(Map<String, String> documentation) {
        this.modesDocumentation = documentation;
        return this;
    }

    public Map<String, String> configurationDocumentation() {
        return configurationDocumentation;
    }

    public DefaultConfiguration configurationDocumentation(Map<String, String> configurationDocumentation) {
        this.configurationDocumentation = configurationDocumentation;
        return this;
    }
}
