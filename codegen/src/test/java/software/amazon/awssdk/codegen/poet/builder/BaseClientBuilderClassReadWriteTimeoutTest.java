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

package software.amazon.awssdk.codegen.poet.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetUtils.buildJavaFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

/**
 * Emission cases for the codegen-baked read/write timeout tier.
 */
class BaseClientBuilderClassReadWriteTimeoutTest {

    @Test
    void serviceHttpConfig_fullyExemptTier_bakesDurationZero() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getMetadata().setDefaultReadWriteTimeoutMillis(-1L);

        assertThat(generate(model))
            .contains(".put(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT, Duration.ZERO)");
    }

    @Test
    void serviceHttpConfig_partialTier_bakesDurationMillis() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getMetadata().setDefaultReadWriteTimeoutMillis(900000L);

        assertThat(generate(model))
            .contains(".put(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT, Duration.ofMillis(900000L))");
    }

    @Test
    void serviceHttpConfig_noTierBaked_omitsFallbackTimeout() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getMetadata().setDefaultReadWriteTimeoutMillis(null);

        assertThat(generate(model)).doesNotContain("SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT");
    }

    private static String generate(IntermediateModel model) {
        StringBuilder output = new StringBuilder();
        try {
            buildJavaFile(new BaseClientBuilderClass(model)).writeTo(output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toString();
    }
}
