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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.javapoet.CodeBlock;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.poet.rules2.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.LetExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.VariableReferenceExpression;

/**
 * Covers the null-check elision in {@code visitLetExpression}. An assign condition is satisfied only
 * when the assigned value is non-null, so emitting {@code return true} instead of
 * {@code return reg != null} is only sound when the value is provably non-null.
 *
 * <p>{@code __ite} is emitted as a ternary between its two branches. Only string-literal branches
 * make it provably non-null: {@code BddPeepholeVisitor.simplifyIte} does not constrain the branches,
 * and the BDD model is produced by the endpoint compiler rather than by this repo, so a
 * {@code {"ref": ...}} branch is legal input and may be null at runtime.
 */
class ConditionFnCodeGeneratorVisitorTest {

    private static final String REGISTER = "suffix";
    private static final String NULLABLE_REF = "someNullableString";

    private RuleRuntimeTypeMirror typeMirror;
    private EndpointRulesSpecUtils specUtils;
    private Map<String, RegistryInfo> registers;

    @BeforeEach
    void setUp() {
        IntermediateModel model = ClientTestModels.queryServiceModelsWithSimpleBddEndpoints();
        typeMirror = new RuleRuntimeTypeMirror(model.getMetadata().getFullInternalEndpointRulesPackageName());
        specUtils = new EndpointRulesSpecUtils(model);

        registers = new HashMap<>();
        registers.put(REGISTER, register(REGISTER));
        registers.put(NULLABLE_REF, register(NULLABLE_REF));
    }

    @Test
    void visitLetExpression_whenIteBranchesAreStringLiterals_elidesNullCheck() {
        String generated = generate(ite(new LiteralStringExpression(".dualstack"), new LiteralStringExpression("")));

        assertThat(generated).contains("return true");
        assertThat(generated).doesNotContain(REGISTER + " != null");
    }

    @Test
    void visitLetExpression_whenIteBranchIsNullableReference_emitsNullCheck() {
        String generated = generate(ite(reference(NULLABLE_REF), new LiteralStringExpression("")));

        assertThat(generated)
            .as("a ref branch can be null at runtime, so the assign condition must check the register")
            .contains("return " + REGISTER + " != null");
        assertThat(generated).doesNotContain("return true");
    }

    @Test
    void visitLetExpression_whenIteFalseBranchIsNullableReference_emitsNullCheck() {
        String generated = generate(ite(new LiteralStringExpression(""), reference(NULLABLE_REF)));

        assertThat(generated).contains("return " + REGISTER + " != null");
        assertThat(generated).doesNotContain("return true");
    }

    @Test
    void visitLetExpression_whenValueIsNotIte_emitsNullCheck() {
        String generated = generate(reference(NULLABLE_REF));

        assertThat(generated).contains("return " + REGISTER + " != null");
        assertThat(generated).doesNotContain("return true");
    }

    /**
     * The single-evaluation form for a boolean coalesce. A ternary would emit the subject twice,
     * running any non-trivial operand twice per evaluation.
     */
    @Test
    void coalesceBoolean_emitsWrapperEqualityEvaluatingSubjectOnce() {
        String withFalseDefault = generate(coalesceBoolean(false));
        assertThat(withFalseDefault).contains("Boolean.TRUE.equals(" + NULLABLE_REF + ")");
        assertThat(withFalseDefault).doesNotContain("!= null ?");

        String withTrueDefault = generate(coalesceBoolean(true));
        assertThat(withTrueDefault).contains("!Boolean.FALSE.equals(" + NULLABLE_REF + ")");
        assertThat(withTrueDefault).doesNotContain("!= null ?");

        // Subject appears exactly once in each.
        assertThat(countOccurrences(withFalseDefault, NULLABLE_REF)).isEqualTo(1);
        assertThat(countOccurrences(withTrueDefault, NULLABLE_REF)).isEqualTo(1);
    }

    private String generate(RuleExpression boundValue) {
        CodeBlock.Builder code = CodeBlock.builder();
        LetExpression let = LetExpression.builder()
                                         .putBinding(REGISTER, boundValue)
                                         .build();
        let.accept(new ConditionFnCodeGeneratorVisitor(code, typeMirror, registers, specUtils));
        return code.build().toString();
    }

    private static RuleExpression ite(RuleExpression ifTrue, RuleExpression ifFalse) {
        return FunctionCallExpression.builder()
                                     .name(BddPeepholeVisitor.ITE)
                                     .type(RuleRuntimeTypeMirror.STRING)
                                     .addArgument(new LiteralBooleanExpression(true))
                                     .addArgument(ifTrue)
                                     .addArgument(ifFalse)
                                     .build();
    }

    private static RuleExpression coalesceBoolean(boolean defaultValue) {
        return FunctionCallExpression.builder()
                                     .name(BddPeepholeVisitor.COALESCE_BOOL)
                                     .type(RuleRuntimeTypeMirror.BOOLEAN)
                                     .addArgument(reference(NULLABLE_REF))
                                     .addArgument(new LiteralBooleanExpression(defaultValue))
                                     .build();
    }

    private static RuleExpression reference(String name) {
        return VariableReferenceExpression.builder().variableName(name).build();
    }

    private static RegistryInfo register(String name) {
        return new RegistryInfo(name, RuleRuntimeTypeMirror.STRING, null, true, null);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) {
            count++;
            i += needle.length();
        }
        return count;
    }
}
