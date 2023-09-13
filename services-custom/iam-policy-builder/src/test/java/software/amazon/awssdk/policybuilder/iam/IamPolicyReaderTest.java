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

import org.junit.jupiter.api.Test;

class IamPolicyReaderTest {
    private static final IamPrincipal PRINCIPAL_1 = IamPrincipal.create("P1", "*");
    private static final IamPrincipal PRINCIPAL_1B = IamPrincipal.create("P1", "B");
    private static final IamPrincipal PRINCIPAL_2 = IamPrincipal.create("P2", "*");
    private static final IamPrincipal NOT_PRINCIPAL_1 = IamPrincipal.create("NP1", "*");
    private static final IamPrincipal NOT_PRINCIPAL_1B = IamPrincipal.create("NP1", "B");
    private static final IamPrincipal NOT_PRINCIPAL_2 = IamPrincipal.create("NP2", "*");
    private static final IamResource RESOURCE_1 = IamResource.create("R1");
    private static final IamResource RESOURCE_2 = IamResource.create("R2");
    private static final IamResource NOT_RESOURCE_1 = IamResource.create("NR1");
    private static final IamResource NOT_RESOURCE_2 = IamResource.create("NR2");
    private static final IamAction ACTION_1 = IamAction.create("A1");
    private static final IamAction ACTION_2 = IamAction.create("A2");
    private static final IamAction NOT_ACTION_1 = IamAction.create("NA1");
    private static final IamAction NOT_ACTION_2 = IamAction.create("NA2");
    private static final IamCondition CONDITION_1 = IamCondition.create("1", "K1", "V1");
    private static final IamCondition CONDITION_2 = IamCondition.create("1", "K2", "V1");
    private static final IamCondition CONDITION_3 = IamCondition.create("1", "K2", "V2");
    private static final IamCondition CONDITION_4 = IamCondition.create("2", "K1", "V1");

    private static final IamStatement FULL_STATEMENT =
        IamStatement.builder()
                    .effect(ALLOW)
                    .sid("Sid")
                    .principals(asList(PRINCIPAL_1, PRINCIPAL_2))
                    .notPrincipals(asList(NOT_PRINCIPAL_1, NOT_PRINCIPAL_2))
                    .resources(asList(RESOURCE_1, RESOURCE_2))
                    .notResources(asList(NOT_RESOURCE_1, NOT_RESOURCE_2))
                    .actions(asList(ACTION_1, ACTION_2))
                    .notActions(asList(NOT_ACTION_1, NOT_ACTION_2))
                    .conditions(asList(CONDITION_1, CONDITION_2, CONDITION_3, CONDITION_4))
                    .build();

    private static final IamPolicy FULL_POLICY =
        IamPolicy.builder()
                 .id("Id")
                 .version("Version")
                 .statements(asList(FULL_STATEMENT, FULL_STATEMENT))
                 .build();

    private static final IamStatement MINIMAL_STATEMENT = IamStatement.builder().effect(ALLOW).build();

    private static final IamPolicy MINIMAL_POLICY =
        IamPolicy.builder()
                 .version("Version")
                 .statements(singletonList(MINIMAL_STATEMENT))
                 .build();

    private static final IamStatement ONE_ELEMENT_LISTS_STATEMENT =
        IamStatement.builder()
                    .effect(ALLOW)
                    .sid("Sid")
                    .principals(singletonList(IamPrincipal.ALL))
                    .notPrincipals(singletonList(IamPrincipal.ALL))
                    .resources(singletonList(RESOURCE_1))
                    .notResources(singletonList(NOT_RESOURCE_1))
                    .actions(singletonList(ACTION_1))
                    .notActions(singletonList(NOT_ACTION_1))
                    .conditions(singletonList(CONDITION_1))
                    .build();

    private static final IamPolicy ONE_ELEMENT_LISTS_POLICY =
        IamPolicy.builder()
                 .version("Version")
                 .statements(singletonList(ONE_ELEMENT_LISTS_STATEMENT))
                 .build();

    private static final IamStatement COMPOUND_PRINCIPAL_STATEMENT =
        IamStatement.builder()
                    .effect(ALLOW)
                    .sid("Sid")
                    .principals(asList(PRINCIPAL_1, PRINCIPAL_1B))
                    .notPrincipals(asList(NOT_PRINCIPAL_1, NOT_PRINCIPAL_1B))
                    .build();

    private static final IamPolicy COMPOUND_PRINCIPAL_POLICY =
        IamPolicy.builder()
                 .version("Version")
                 .statements(singletonList(COMPOUND_PRINCIPAL_STATEMENT))
                 .build();

    private static final IamPolicyReader READER = IamPolicyReader.create();

    @Test
    public void readFullPolicyWorks() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Id\" : \"Id\",\n"
                               + "  \"Statement\" : [ {\n"
                               + "    \"Sid\" : \"Sid\",\n"
                               + "    \"Effect\" : \"Allow\",\n"
                               + "    \"Principal\" : {\n"
                               + "      \"P1\" : \"*\",\n"
                               + "      \"P2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"NotPrincipal\" : {\n"
                               + "      \"NP1\" : \"*\",\n"
                               + "      \"NP2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"Action\" : [ \"A1\", \"A2\" ],\n"
                               + "    \"NotAction\" : [ \"NA1\", \"NA2\" ],\n"
                               + "    \"Resource\" : [ \"R1\", \"R2\" ],\n"
                               + "    \"NotResource\" : [ \"NR1\", \"NR2\" ],\n"
                               + "    \"Condition\" : {\n"
                               + "      \"1\" : {\n"
                               + "        \"K1\" : \"V1\",\n"
                               + "        \"K2\" : [ \"V1\", \"V2\" ]\n"
                               + "      },\n"
                               + "      \"2\" : {\n"
                               + "        \"K1\" : \"V1\"\n"
                               + "      }\n"
                               + "    }\n"
                               + "  }, {\n"
                               + "    \"Sid\" : \"Sid\",\n"
                               + "    \"Effect\" : \"Allow\",\n"
                               + "    \"Principal\" : {\n"
                               + "      \"P1\" : \"*\",\n"
                               + "      \"P2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"NotPrincipal\" : {\n"
                               + "      \"NP1\" : \"*\",\n"
                               + "      \"NP2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"Action\" : [ \"A1\", \"A2\" ],\n"
                               + "    \"NotAction\" : [ \"NA1\", \"NA2\" ],\n"
                               + "    \"Resource\" : [ \"R1\", \"R2\" ],\n"
                               + "    \"NotResource\" : [ \"NR1\", \"NR2\" ],\n"
                               + "    \"Condition\" : {\n"
                               + "      \"1\" : {\n"
                               + "        \"K1\" : \"V1\",\n"
                               + "        \"K2\" : [ \"V1\", \"V2\" ]\n"
                               + "      },\n"
                               + "      \"2\" : {\n"
                               + "        \"K1\" : \"V1\"\n"
                               + "      }\n"
                               + "    }\n"
                               + "  } ]\n"
                               + "}"))
            .isEqualTo(FULL_POLICY);
    }

    @Test
    public void readMinimalPolicyWorks() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Statement\" : {\n"
                               + "    \"Effect\" : \"Allow\"\n"
                               + "  }\n"
                               + "}"))
            .isEqualTo(MINIMAL_POLICY);
    }

    @Test
    public void readCompoundPrincipalsWorks() {
        assertThat(READER.read("{\n" +
                           "    \"Version\": \"Version\",\n" +
                           "    \"Statement\": [\n" +
                           "        {\n" +
                           "            \"Sid\": \"Sid\",\n" +
                           "            \"Effect\": \"Allow\",\n" +
                           "            \"Principal\": {\n" +
                           "                \"P1\": [\n" +
                           "                    \"*\",\n" +
                           "                    \"B\"\n" +
                           "                ]\n" +
                           "            },\n" +
                           "            \"NotPrincipal\": {\n" +
                           "                \"NP1\": [\n" +
                           "                    \"*\",\n" +
                           "                    \"B\"\n" +
                           "                ]\n" +
                           "            }\n" +
                           "        }\n" +
                           "    ]\n" +
                           "}")).isEqualTo(COMPOUND_PRINCIPAL_POLICY);
    }

    @Test
    public void singleElementListsAreSupported() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Statement\" : {\n"
                               + "    \"Sid\" : \"Sid\",\n"
                               + "    \"Effect\" : \"Allow\",\n"
                               + "    \"Principal\" : \"*\",\n"
                               + "    \"NotPrincipal\" : \"*\",\n"
                               + "    \"Action\" : \"A1\",\n"
                               + "    \"NotAction\" : \"NA1\",\n"
                               + "    \"Resource\" : \"R1\",\n"
                               + "    \"NotResource\" : \"NR1\",\n"
                               + "    \"Condition\" : {\n"
                               + "      \"1\" : {\n"
                               + "        \"K1\" : \"V1\"\n"
                               + "      }\n"
                               + "    }\n"
                               + "  }\n"
                               + "}"))
            .isEqualTo(ONE_ELEMENT_LISTS_POLICY);
    }
}