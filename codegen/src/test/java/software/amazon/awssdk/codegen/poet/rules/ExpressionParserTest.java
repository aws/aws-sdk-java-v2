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

package software.amazon.awssdk.codegen.poet.rules;

import java.util.Arrays;
import java.util.Collection;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpressionParserTest {

    @ParameterizedTest
    @MethodSource("testCases")
    public void validateTestCaseData(TestCase testCase) {
        AssertionsForClassTypes.assertThat(ExpressionParser.parseStringValue(testCase.source)).isEqualTo(testCase.expected);
    }

    public static Collection<TestCase> testCases() {
        return Arrays.asList(
            TestCase.builder("{accessPointArn#region}")
                    .expected(MemberAccessExpression
                                  .builder()
                                  .name("region")
                                  .source(new VariableReferenceExpression("accessPointArn"))
                                  .build())
                    .build(),
            TestCase.builder("resourceId[0]")
                    .expected(IndexedAccessExpression
                                  .builder()
                                  .source(new VariableReferenceExpression("resourceId"))
                                  .index(0)
                                  .build())
                    .build(),
            TestCase.builder("{url#scheme}://{url#authority}")
                    .expected(StringConcatExpression
                                  .builder()
                                  .addExpression(MemberAccessExpression
                                                     .builder()
                                                     .source(new VariableReferenceExpression("url"))
                                                     .name("scheme")
                                                     .build())
                                  .addExpression(new LiteralStringExpression("://"))
                                  .addExpression(MemberAccessExpression
                                                     .builder()
                                                     .source(new VariableReferenceExpression("url"))
                                                     .name("authority")
                                                     .build())
                                  .build())
                    .build()
        );
    }

    static class TestCase {
        private final String source;
        private final RuleExpression expected;

        public TestCase(Builder builder) {
            this.source = builder.source;
            this.expected = builder.expected;
        }

        static Builder builder(String source) {
            return new Builder()
                .source(source);
        }

        static class Builder {
            String source;
            RuleExpression expected;

            public Builder source(String source) {
                this.source = source;
                return this;
            }

            public Builder expected(RuleExpression expected) {
                this.expected = expected;
                return this;
            }

            public TestCase build() {
                return new TestCase(this);
            }
        }
    }

}