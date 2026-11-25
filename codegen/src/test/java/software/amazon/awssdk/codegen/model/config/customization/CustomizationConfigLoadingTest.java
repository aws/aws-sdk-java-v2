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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

class CustomizationConfigLoadingTest {

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedDefaultNewRetries2026_loadsWhenUnknownPropertiesFail(@TempDir Path dir) throws IOException {
        Path customization = dir.resolve("customization.config");
        Files.write(customization, "{ \"defaultNewRetries2026\": true }".getBytes(StandardCharsets.UTF_8));

        CustomizationConfig config = ModelLoaderUtils.loadModel(CustomizationConfig.class, customization.toFile(), true);

        assertThat(config.getDefaultNewRetries2026()).isTrue();
    }
}
