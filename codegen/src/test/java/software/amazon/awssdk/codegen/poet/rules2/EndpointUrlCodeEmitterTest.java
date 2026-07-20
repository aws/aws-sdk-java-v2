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

import com.squareup.javapoet.CodeBlock;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EndpointUrlCodeEmitter}.
 *
 * <p>Verifies that URL expressions are correctly decomposed into {@code EndpointUrl.fromComponents()}
 * calls when possible, and fall back to {@code EndpointUrl.fromString()} otherwise.</p>
 */
class EndpointUrlCodeEmitterTest {

    private static VariableReferenceExpression varRef(String name) {
        return new VariableReferenceExpression(name);
    }

    private static LiteralStringExpression literal(String value) {
        return new LiteralStringExpression(value);
    }

    private static MemberAccessExpression memberAccess(String source, String name) {
        return MemberAccessExpression.builder()
                                     .source(new VariableReferenceExpression(source))
                                     .name(name)
                                     .build();
    }

    /**
     * Creates a minimal CodeGeneratorVisitor suitable for testing expression emission.
     * Only the expression visiting methods (literal, varRef, memberAccess, stringConcat) are used.
     */
    private static String emitUrl(RuleExpression urlExpr) {
        CodeBlock.Builder builder = CodeBlock.builder();
        CodeGeneratorVisitor visitor = new CodeGeneratorVisitor(
            new RuleRuntimeTypeMirror("software.amazon.awssdk.test"),
            SymbolTable.builder().build(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            builder
        );
        EndpointUrlCodeEmitter.emit(urlExpr, builder, visitor);
        return builder.build().toString();
    }

    // --- Pre-parseable cases: emit fromComponents ---

    /**
     * "https://sts.{Region}.{PartitionResult#dnsSuffix}" — typical service URL.
     */
    @Test
    void emit_simpleHostConcat_emitsFromComponents() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://sts."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(memberAccess("partitionResult", "dnsSuffix"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("\"sts.\"");
        assertThat(code).contains("region");
        assertThat(code).contains("partitionResult.dnsSuffix()");
        assertThat(code).contains("-1");
        assertThat(code).contains("\"\""); // empty path
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://runtime.sagemaker.{Region}.{dnsSuffix}:8443" — URL with port.
     */
    @Test
    void emit_withPort_emitsFromComponentsWithPort() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://runtime.sagemaker."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal(":8443"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("8443");
        assertThat(code).contains("\"\""); // empty path
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://places.geo.{Region}.{dnsSuffix}/v2" — URL with static path.
     */
    @Test
    void emit_withStaticPath_emitsFromComponentsWithPath() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://places.geo."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal("/v2"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("-1");
        assertThat(code).contains("\"/v2\""); // static path emitted as literal
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://service.{Region}:8443/v2" — URL with port and path in same literal.
     */
    @Test
    void emit_withPortAndPath_emitsFromComponentsWithBoth() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://service."))
            .addExpression(varRef("region"))
            .addExpression(literal(":8443/v2"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("8443");
        assertThat(code).contains("\"/v2\"");
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://s3.{Region}.{dnsSuffix}/{uriEncodedBucket}" — URL with dynamic path variable.
     * This should now be pre-parseable (path contains dynamic expression).
     */
    @Test
    void emit_withDynamicPath_emitsFromComponentsWithPathExpression() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://s3."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .addExpression(literal("/"))
            .addExpression(varRef("uriEncodedBucket"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("region");
        assertThat(code).contains("dnsSuffix");
        // Path is dynamic: "/" + uriEncodedBucket
        assertThat(code).contains("\"/\"");
        assertThat(code).contains("uriEncodedBucket");
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://sts.amazonaws.com" — fully static literal URL.
     */
    @Test
    void emit_fullyStaticLiteral_emitsFromComponentsAllLiterals() {
        LiteralStringExpression urlExpr = literal("https://sts.amazonaws.com");

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("\"sts.amazonaws.com\"");
        assertThat(code).contains("-1");
        assertThat(code).contains("\"\"");
        assertThat(code).doesNotContain("fromString");
    }

    /**
     * "https://example.com:443/api/v1" — static URL with port and path.
     */
    @Test
    void emit_fullyStaticWithPortAndPath_emitsFromComponentsAllLiterals() {
        LiteralStringExpression urlExpr = literal("https://example.com:443/api/v1");

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("\"example.com\"");
        assertThat(code).contains("443");
        assertThat(code).contains("\"/api/v1\"");
        assertThat(code).doesNotContain("fromString");
    }

    // --- Fallback cases: emit fromString ---

    /**
     * "{url#scheme}://{Bucket}.{url#authority}{url#path}" — scheme is dynamic.
     */
    @Test
    void emit_dynamicScheme_emitsFromString() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(memberAccess("url", "scheme"))
            .addExpression(literal("://"))
            .addExpression(varRef("Bucket"))
            .addExpression(literal("."))
            .addExpression(memberAccess("url", "authority"))
            .addExpression(memberAccess("url", "path"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromString(");
        assertThat(code).doesNotContain("fromComponents");
    }

    /**
     * VariableReferenceExpression (e.g. just a variable) — not a string template or literal.
     */
    @Test
    void emit_variableReferenceExpression_emitsFromString() {
        VariableReferenceExpression urlExpr = varRef("computedUrl");

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromString(");
        assertThat(code).contains("computedUrl");
        assertThat(code).doesNotContain("fromComponents");
    }

    /**
     * region + ".amazonaws.com" — first expression is not a scheme literal.
     */
    @Test
    void emit_firstExpressionNotSchemeLiteral_emitsFromString() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(varRef("region"))
            .addExpression(literal(".amazonaws.com"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromString(");
        assertThat(code).doesNotContain("fromComponents");
    }

    // --- Edge cases ---

    /**
     * "http://localhost:8080/test" — http scheme (not https).
     */
    @Test
    void emit_httpScheme_emitsFromComponents() {
        LiteralStringExpression urlExpr = literal("http://localhost:8080/test");

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"http\"");
        assertThat(code).contains("\"localhost\"");
        assertThat(code).contains("8080");
        assertThat(code).contains("\"/test\"");
    }

    /**
     * "https://{Bucket}.s3.{Region}.{dnsSuffix}" — host starts with a dynamic expression
     * after the scheme prefix (the literal "https://" is followed by nothing before the first varRef).
     */
    @Test
    void emit_hostStartsWithVariable_emitsFromComponents() {
        StringConcatExpression urlExpr = StringConcatExpression.builder()
            .addExpression(literal("https://"))
            .addExpression(varRef("Bucket"))
            .addExpression(literal(".s3."))
            .addExpression(varRef("region"))
            .addExpression(literal("."))
            .addExpression(varRef("dnsSuffix"))
            .build();

        String code = emitUrl(urlExpr);

        assertThat(code).contains("EndpointUrl.fromComponents(");
        assertThat(code).contains("\"https\"");
        assertThat(code).contains("Bucket");
        assertThat(code).doesNotContain("fromString");
    }
}
