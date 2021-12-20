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

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.lite.PoetMatchers.generatesTo;

import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultsModeGenerationTest {

    private static final String DEFAULT_CONFIGURATION = "/software/amazon/awssdk/codegen/lite/test-sdk-default-configuration.json";
    private static final String DEFAULTS_MODE_BASE = "software.amazon.awssdk.defaultsmode";

    private File file;
    private DefaultConfiguration defaultConfiguration;

    @BeforeEach
    public void before() throws Exception {
        this.file = Paths.get(getClass().getResource(DEFAULT_CONFIGURATION).toURI()).toFile();
        this.defaultConfiguration = DefaultsLoader.load(file);
    }

    @Test
    public void defaultsModeEnum() {
        DefaultsModeGenerator generator = new DefaultsModeGenerator(DEFAULTS_MODE_BASE, defaultConfiguration);
        assertThat(generator, generatesTo("defaults-mode.java"));
    }

    @Test
    public void defaultsModeConfigurationClass() {
        DefaultsModeConfigurationGenerator generator = new DefaultsModeConfigurationGenerator(DEFAULTS_MODE_BASE, DEFAULTS_MODE_BASE, defaultConfiguration);
        assertThat(generator, generatesTo("defaults-mode-configuration.java"));
    }

}
