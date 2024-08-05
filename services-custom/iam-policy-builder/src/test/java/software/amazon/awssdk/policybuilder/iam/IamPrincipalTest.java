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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IamPrincipalTest {
    private static final IamPrincipal FULL_PRINCIPAL =
        IamPrincipal.builder()
                    .type("Type")
                    .id("Id")
                    .build();

    @Test
    public void constructorsWork() {
        assertThat(IamPrincipal.create("Type", "Id")).isEqualTo(FULL_PRINCIPAL);
        assertThat(IamPrincipal.create(IamPrincipalType.create("Type"), "Id")).isEqualTo(FULL_PRINCIPAL);
        assertThat(IamPrincipal.createAll("Type", asList("Id1", "Id2")))
            .containsExactly(IamPrincipal.create("Type", "Id1"), IamPrincipal.create("Type", "Id2"));
        assertThat(IamPrincipal.createAll(IamPrincipalType.create("Type"), asList("Id1", "Id2")))
            .containsExactly(IamPrincipal.create("Type", "Id1"), IamPrincipal.create("Type", "Id2"));
    }

    @Test
    public void simpleGettersSettersWork() {
        assertThat(FULL_PRINCIPAL.id()).isEqualTo("Id");
        assertThat(FULL_PRINCIPAL.type().value()).isEqualTo("Type");
    }

    @Test
    public void toBuilderPreservesValues() {
        IamPrincipal principal = FULL_PRINCIPAL.toBuilder().build();
        assertThat(principal.id()).isEqualTo("Id");
        assertThat(principal.type().value()).isEqualTo("Type");
    }

    @Test
    public void typeSettersWork() {
        assertThat(IamPrincipal.builder().type("Type").id("Id").build().type().value()).isEqualTo("Type");
        assertThat(IamPrincipal.builder().type(IamPrincipalType.create("Type")).id("Id").build().type().value()).isEqualTo("Type");
    }
}