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

package software.amazon.awssdk.codegen.emitters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

public class GeneratorPathProviderTest {

    @Test
    public void warmUpProviderDirectory_isUnderSourceDirInCracInternalPackage() {
        Metadata metadata = new Metadata();
        metadata.setRootPackageName("software.amazon.awssdk.services");
        metadata.setClientPackageName("query");

        IntermediateModel model = new IntermediateModel();
        model.setMetadata(metadata);

        GeneratorPathProvider provider =
            new GeneratorPathProvider(model, "/src", "/test", "/resources");

        assertThat(provider.getWarmUpProviderDirectory())
            .isEqualTo("/src/software/amazon/awssdk/services/query/internal/crac");
    }
}
