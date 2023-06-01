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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class AuthSchemeSpecTest {

    @ParameterizedTest
    @MethodSource("parameters")
    void testCase(TestCase testCase) {
        ClassSpec classSpec = testCase.classSpecProvider.apply(testCase.modelProvider.get());
        String expectedFileName = testCase.caseName + "-auth-scheme-" + testCase.outputFileSuffix + ".java";
        assertThat("Generated class must match " + expectedFileName,
            classSpec, generatesTo(expectedFileName));
    }

    static List<TestCase> parameters() {
        return Arrays.asList(
            // query
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModels)
                    .classSpecProvider(AuthSchemeProviderSpec::new)
                    .caseName("query")
                    .outputFileSuffix("provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModels)
                    .classSpecProvider(AuthSchemeParamsSpec::new)
                    .caseName("query")
                    .outputFileSuffix("params")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModels)
                    .classSpecProvider(DefaultAuthSchemeProviderSpec::new)
                    .caseName("query")
                    .outputFileSuffix("default-provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModels)
                    .classSpecProvider(DefaultAuthSchemeParamsSpec::new)
                    .caseName("query")
                    .outputFileSuffix("default-params")
                    .build(),
            // query-endpoint-auth-params
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParams)
                    .classSpecProvider(AuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParams)
                    .classSpecProvider(AuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("params")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParams)
                    .classSpecProvider(DefaultAuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("default-provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParams)
                    .classSpecProvider(DefaultAuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("default-params")
                    .build()
        );
    }

    static class TestCase {
        private final Supplier<IntermediateModel> modelProvider;
        private final Function<IntermediateModel, ClassSpec> classSpecProvider;
        private final String outputFileSuffix;
        private final String caseName;

        TestCase(Builder builder) {
            this.modelProvider = builder.modelProvider;
            this.classSpecProvider = builder.classSpecProvider;
            this.outputFileSuffix = builder.outputFileSuffix;
            this.caseName = builder.caseName;
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private Supplier<IntermediateModel> modelProvider;
            private Function<IntermediateModel, ClassSpec> classSpecProvider;
            private String outputFileSuffix;
            private String caseName;

            Builder modelProvider(Supplier<IntermediateModel> modelProvider) {
                this.modelProvider = modelProvider;
                return this;
            }

            Builder classSpecProvider(Function<IntermediateModel, ClassSpec> classSpecProvider) {
                this.classSpecProvider = classSpecProvider;
                return this;
            }

            Builder outputFileSuffix(String outputFileSuffix) {
                this.outputFileSuffix = outputFileSuffix;
                return this;
            }

            Builder caseName(String caseName) {
                this.caseName = caseName;
                return this;
            }

            TestCase build() {
                return new TestCase(this);
            }
        }
    }
}
