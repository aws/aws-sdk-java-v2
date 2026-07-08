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

package software.amazon.awssdk.codegen.poet.rules2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EndpointUrlCodegenAnalyzer}.
 *
 * <p><b>Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5</b></p>
 *
 * <p>Property 12: Static component extraction correctness — verify that common URL patterns
 * (scheme + host concat, no port, no path) are classified as pre-parseable with correct components.</p>
 */
class EndpointUrlCodegenAnalyzerTest {

    private static VariableReferenceExpression varRef(String name) {
        return new VariableReferenceExpression(name);
    }

    private static LiteralStringExpression literal(String value) {
        return new LiteralStringExpression(value);
    }

    /**
     * Simulates: "https://query." + region + "." + partitionResult.dnsSuffix()
     */
    @Test
    void analyze_withSimpleHostConcat_shouldBePreParseable() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://query."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isTrue();
        assertThat(result.scheme()).isEqualTo("https");
        assertThat(result.port()).isEqualTo(-1);
        assertThat(result.encodedPath()).isEqualTo("");
        assertThat(result.hostExpr()).isNotNull();
    }

    /**
     * Simulates: "https://runtime.sagemaker." + region + "." + dnsSuffix + ":8443"
     */
    @Test
    void analyze_withPort_shouldBePreParseableWithCorrectPort() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://runtime.sagemaker."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal(":8443"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isTrue();
        assertThat(result.scheme()).isEqualTo("https");
        assertThat(result.port()).isEqualTo(8443);
        assertThat(result.encodedPath()).isEqualTo("");
        assertThat(result.hostExpr()).isNotNull();
    }

    /**
     * Simulates: "https://places.geo." + region + "." + dnsSuffix + "/v2"
     */
    @Test
    void analyze_withPath_shouldBePreParseableWithCorrectPath() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://places.geo."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal("/v2"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isTrue();
        assertThat(result.scheme()).isEqualTo("https");
        assertThat(result.port()).isEqualTo(-1);
        assertThat(result.encodedPath()).isEqualTo("/v2");
        assertThat(result.hostExpr()).isNotNull();
    }

    /**
     * Simulates: "https://service." + region + ":8443/v2"
     */
    @Test
    void analyze_withPortAndPath_shouldBePreParseableWithBoth() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://service."))
            .addExpression(varRef("region"))
            .addExpression(literal(":8443/v2"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isTrue();
        assertThat(result.scheme()).isEqualTo("https");
        assertThat(result.port()).isEqualTo(8443);
        assertThat(result.encodedPath()).isEqualTo("/v2");
        assertThat(result.hostExpr()).isNotNull();
    }

    /**
     * Simulates: "https://s3." + region + "." + dnsSuffix + "/" + uriEncodedBucket
     */
    @Test
    void analyze_withRuntimeVariableInPath_shouldNotBePreParseable() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://s3."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal("/"))
            .addExpression(varRef("uriEncodedBucket"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isFalse();
    }

    @Test
    void analyze_withFunctionCallExpression_shouldNotBePreParseable() {
        FunctionCallExpression urlExpr = FunctionCallExpression.builder()
            .name("getUrl")
            .addArgument(varRef("endpoint"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isFalse();
    }

    /**
     * Simulates: region + ".amazonaws.com" (first expression is a variable, not a scheme literal)
     */
    @Test
    void analyze_withFirstExpressionNotSchemeLiteral_shouldNotBePreParseable() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(varRef("region"))
            .addExpression(literal(".amazonaws.com"))
            .build();

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isFalse();
    }

    @Test
    void analyze_withFullyStaticLiteralUrl_shouldBePreParseableWithCorrectComponents() {
        LiteralStringExpression urlExpr = literal("https://example.com");

        EndpointUrlCodegenAnalyzer.AnalysisResult result = EndpointUrlCodegenAnalyzer.analyze(urlExpr);

        assertThat(result.isPreParseable()).isTrue();
        assertThat(result.scheme()).isEqualTo("https");
        assertThat(result.port()).isEqualTo(-1);
        assertThat(result.encodedPath()).isEqualTo("");
        assertThat(result.hostExpr()).isInstanceOf(LiteralStringExpression.class);
        assertThat(((LiteralStringExpression) result.hostExpr()).value()).isEqualTo("example.com");
    }
}
