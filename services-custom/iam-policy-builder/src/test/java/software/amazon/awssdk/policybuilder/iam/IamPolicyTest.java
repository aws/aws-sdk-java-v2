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

package software.amazon.awssdk.policybuilder.iam;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.policybuilder.iam.IamEffect.ALLOW;
import static software.amazon.awssdk.policybuilder.iam.IamEffect.DENY;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class IamPolicyTest {
    private static final IamStatement ALLOW_STATEMENT = IamStatement.builder().effect(ALLOW).build();
    private static final IamStatement DENY_STATEMENT = IamStatement.builder().effect(DENY).build();
    private static final String SMALLEST_POLICY_JSON = "{\"Version\":\"2012-10-17\",\"Statement\":{\"Effect\":\"Allow\"}}";
    private static final IamPolicy SMALLEST_POLICY = IamPolicy.builder().addStatement(ALLOW_STATEMENT).build();


    private static final IamPolicy FULL_POLICY = IamPolicy.builder()
                                                          .id("Id")
                                                          .version("Version")
                                                          .statements(singletonList(ALLOW_STATEMENT))
                                                          .build();

    @Test
    public void fromJson_delegatesToIamPolicyReader() {
        IamPolicy iamPolicy = IamPolicy.fromJson(SMALLEST_POLICY_JSON);
        assertThat(iamPolicy.version()).isNotNull();
        assertThat(iamPolicy.statements()).containsExactly(ALLOW_STATEMENT);
    }

    @Test
    public void toJson_delegatesToIamPolicyWriter() {
        assertThat(SMALLEST_POLICY.toJson()).isEqualTo(SMALLEST_POLICY_JSON);
    }

    @Test
    public void simpleGettersSettersWork() {
        assertThat(FULL_POLICY.id()).isEqualTo("Id");
        assertThat(FULL_POLICY.version()).isEqualTo("Version");
        assertThat(FULL_POLICY.statements()).containsExactly(ALLOW_STATEMENT);
    }

    @Test
    public void toBuilderPreservesValues() {
        assertThat(FULL_POLICY.toBuilder().build()).isEqualTo(FULL_POLICY);
    }

    @Test
    public void toStringIncludesAllValues() {
        assertThat(FULL_POLICY.toString())
            .isEqualTo("IamPolicy(id=Id, version=Version, statements=[IamStatement(effect=IamEffect(value=Allow))])");
    }

    @Test
    public void statementGettersSettersWork() {
        assertThat(policy(p -> p.statements(asList(ALLOW_STATEMENT, DENY_STATEMENT))).statements())
            .containsExactly(ALLOW_STATEMENT, DENY_STATEMENT);
        assertThat(policy(p -> p.addStatement(ALLOW_STATEMENT).addStatement(DENY_STATEMENT)).statements())
            .containsExactly(ALLOW_STATEMENT, DENY_STATEMENT);
        assertThat(policy(p -> p.addStatement(s -> s.effect(ALLOW)).addStatement(s -> s.effect(DENY))).statements())
            .containsExactly(ALLOW_STATEMENT, DENY_STATEMENT);
    }

    @Test
    public void statementCollectionSettersResetsList() {
        assertThat(policy(p -> p.statements(asList(ALLOW_STATEMENT, DENY_STATEMENT))
                                .statements(singletonList(DENY_STATEMENT))).statements())
            .containsExactly(DENY_STATEMENT);
    }

    private IamPolicy policy(Consumer<IamPolicy.Builder> policy) {
        return IamPolicy.builder().applyMutation(policy).build();
    }
}