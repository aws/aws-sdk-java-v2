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
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
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
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(AuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("provider")
                    .build(),
            // Endpoint based AuthScheme provider WITH allow list
            // - Endpoint Provider
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(EndpointBasedAuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("endpoint-provider")
                    .build(),
            // - Modeled provider
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("modeled-provider")
                    .build(),
            // Endpoint based AuthScheme provider WITHOUT allow list
            // - Endpoint Provider
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithoutAllowList)
                    .classSpecProvider(EndpointBasedAuthSchemeProviderSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("endpoint-provider-without-allowlist")
                    .build(),
            // Auth scheme params from endpoint WITH allow list, MUST include ONLY the params in the allow list.
            // - Interface
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(AuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("params-with-allowlist")
                    .build(),
            // - Implementation
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(DefaultAuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("default-params-with-allowlist")
                    .build(),
            // Auth scheme params from endpoint WITHOUT allow list, it MUST include all the endpoint params except for the
            // default params.
            // - Interface
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithoutAllowList)
                    .classSpecProvider(AuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("params-without-allowlist")
                    .build(),
            // - Implementation
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithoutAllowList)
                    .classSpecProvider(DefaultAuthSchemeParamsSpec::new)
                    .caseName("query-endpoint-auth-params")
                    .outputFileSuffix("default-params-without-allowlist")
                    .build(),
            // Granular auth
            TestCase.builder()
                    .modelProvider(ClientTestModels::granularAuthProvidersServiceModels)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("granular")
                    .outputFileSuffix("default-provider")
                    .build(),
            // All operations with auth with the same values
            TestCase.builder()
                    .modelProvider(ClientTestModels::allOperationsWithAuthSameValueServiceModels)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("all-ops-auth-same-value")
                    .outputFileSuffix("default-provider")
                    .build(),
            // All operations with auth with different values
            TestCase.builder()
                    .modelProvider(ClientTestModels::allOperationsWithAuthDifferentValueServiceModels)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("all-ops-auth-different-value")
                    .outputFileSuffix("default-provider")
                    .build(),
            // Service with operations with auth none
            TestCase.builder()
                    .modelProvider(ClientTestModels::operationWithNoAuth)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("ops-with-no-auth")
                    .outputFileSuffix("default-provider")
                    .build(),
            // Service with signature version with the same value as S3
            TestCase.builder()
                    .modelProvider(ClientTestModels::serviceMiniS3)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("mini-s3")
                    .outputFileSuffix("default-provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::granularAuthWithLegacyTraitServiceModels)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("auth-with-legacy-trait")
                    .outputFileSuffix("default-provider")
                    .build(),
            // Interceptors
            // - Normal case
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModels)
                    .classSpecProvider(AuthSchemeInterceptorSpec::new)
                    .caseName("query")
                    .outputFileSuffix("interceptor")
                    .build(),
            // - Endpoints based params with allow list
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithAllowList)
                    .classSpecProvider(AuthSchemeInterceptorSpec::new)
                    .caseName("query-endpoint-auth-params-with-allowlist")
                    .outputFileSuffix("interceptor")
                    .build(),
            // - Endpoints based params without allow list
            TestCase.builder()
                    .modelProvider(ClientTestModels::queryServiceModelsEndpointAuthParamsWithoutAllowList)
                    .classSpecProvider(AuthSchemeInterceptorSpec::new)
                    .caseName("query-endpoint-auth-params-without-allowlist")
                    .outputFileSuffix("interceptor")
                    .build(),
            // Service with auth trait with Sigv4a
            TestCase.builder()
                    .modelProvider(ClientTestModels::opsWithSigv4a)
                    .classSpecProvider(ModelBasedAuthSchemeProviderSpec::new)
                    .caseName("ops-auth-sigv4a-value")
                    .outputFileSuffix("default-provider")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::opsWithSigv4a)
                    .classSpecProvider(AuthSchemeParamsSpec::new)
                    .caseName("ops-auth-sigv4a-value")
                    .outputFileSuffix("params")
                    .build(),
            TestCase.builder()
                    .modelProvider(ClientTestModels::opsWithSigv4a)
                    .classSpecProvider(DefaultAuthSchemeParamsSpec::new)
                    .caseName("ops-auth-sigv4a-value")
                    .outputFileSuffix("default-params")
                    .build()
        );
    }

    static class TestCase {
        private final Supplier<IntermediateModel> modelProvider;
        private final Function<IntermediateModel, ClassSpec> classSpecProvider;
        private final String outputFileSuffix;
        private final String caseName;


        @Override
        public String toString() {
            return "TestCase{" +
                   "caseName='" + caseName + "-" + outputFileSuffix + '\'' +
                   '}';
        }

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
