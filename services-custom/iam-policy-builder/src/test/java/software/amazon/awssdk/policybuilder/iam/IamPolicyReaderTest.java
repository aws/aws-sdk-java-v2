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
    private static final IamPrincipal PRINCIPAL_1 = IamPrincipal.ALL;
    private static final IamPrincipal PRINCIPAL_2 = IamPrincipal.create("2", "*");
    private static final IamResource RESOURCE_1 = IamResource.create("1");
    private static final IamResource RESOURCE_2 = IamResource.create("2");
    private static final IamAction ACTION_1 = IamAction.create("1");
    private static final IamAction ACTION_2 = IamAction.create("2");
    private static final IamCondition CONDITION_1 = IamCondition.create("1", "K1", "V1");
    private static final IamCondition CONDITION_2 = IamCondition.create("1", "K2", "V1");
    private static final IamCondition CONDITION_3 = IamCondition.create("1", "K2", "V2");
    private static final IamCondition CONDITION_4 = IamCondition.create("2", "K1", "V1");

    private static final IamStatement FULL_STATEMENT =
        IamStatement.builder()
                    .effect(ALLOW)
                    .sid("Sid")
                    .principals(asList(PRINCIPAL_1, PRINCIPAL_2))
                    .notPrincipals(asList(PRINCIPAL_1, PRINCIPAL_2))
                    .resources(asList(RESOURCE_1, RESOURCE_2))
                    .notResources(asList(RESOURCE_1, RESOURCE_2))
                    .actions(asList(ACTION_1, ACTION_2))
                    .notActions(asList(ACTION_1, ACTION_2))
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
                    .principals(singletonList(PRINCIPAL_1))
                    .notPrincipals(singletonList(PRINCIPAL_1))
                    .resources(singletonList(RESOURCE_1))
                    .notResources(singletonList(RESOURCE_1))
                    .actions(singletonList(ACTION_1))
                    .notActions(singletonList(ACTION_1))
                    .conditions(singletonList(CONDITION_1))
                    .build();

    private static final IamPolicy ONE_ELEMENT_LISTS_POLICY =
        IamPolicy.builder()
                 .version("Version")
                 .statements(singletonList(ONE_ELEMENT_LISTS_STATEMENT))
                 .build();

    private static final IamPolicyReader READER = IamPolicyReader.create();

    @Test
    public void readFullPolicyWorks() {
        assertThat(READER.read("{\"Version\":\"Version\","
                               + "\"Id\":\"Id\","
                               + "\"Statement\":["
                               + "{\"Sid\":\"Sid\",\"Effect\":\"Allow\",\"Principal\":{\"*\":\"*\",\"2\":\"*\"},\"NotPrincipal\":{\"*\":\"*\",\"2\":\"*\"},\"Action\":[\"1\",\"2\"],\"NotAction\":[\"1\",\"2\"],\"Resource\":[\"1\",\"2\"],\"NotResource\":[\"1\",\"2\"],\"Condition\":{\"1\":{\"K1\":\"V1\",\"K2\":[\"V1\",\"V2\"]},\"2\":{\"K1\":\"V1\"}}},"
                               + "{\"Sid\":\"Sid\",\"Effect\":\"Allow\",\"Principal\":{\"*\":\"*\",\"2\":\"*\"},\"NotPrincipal\":{\"*\":\"*\",\"2\":\"*\"},\"Action\":[\"1\",\"2\"],\"NotAction\":[\"1\",\"2\"],\"Resource\":[\"1\",\"2\"],\"NotResource\":[\"1\",\"2\"],\"Condition\":{\"1\":{\"K1\":\"V1\",\"K2\":[\"V1\",\"V2\"]},\"2\":{\"K1\":\"V1\"}}}"
                               + "]}"))
            .isEqualTo(FULL_POLICY);
    }

    @Test
    public void prettyWriteFullPolicyWorks() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Id\" : \"Id\",\n"
                               + "  \"Statement\" : [ {\n"
                               + "    \"Sid\" : \"Sid\",\n"
                               + "    \"Effect\" : \"Allow\",\n"
                               + "    \"Principal\" : {\n"
                               + "      \"*\" : \"*\",\n"
                               + "      \"2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"NotPrincipal\" : {\n"
                               + "      \"*\" : \"*\",\n"
                               + "      \"2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"Action\" : [ \"1\", \"2\" ],\n"
                               + "    \"NotAction\" : [ \"1\", \"2\" ],\n"
                               + "    \"Resource\" : [ \"1\", \"2\" ],\n"
                               + "    \"NotResource\" : [ \"1\", \"2\" ],\n"
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
                               + "      \"*\" : \"*\",\n"
                               + "      \"2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"NotPrincipal\" : {\n"
                               + "      \"*\" : \"*\",\n"
                               + "      \"2\" : \"*\"\n"
                               + "    },\n"
                               + "    \"Action\" : [ \"1\", \"2\" ],\n"
                               + "    \"NotAction\" : [ \"1\", \"2\" ],\n"
                               + "    \"Resource\" : [ \"1\", \"2\" ],\n"
                               + "    \"NotResource\" : [ \"1\", \"2\" ],\n"
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
    public void writeMinimalPolicyWorks() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Statement\" : {\n"
                               + "    \"Effect\" : \"Allow\"\n"
                               + "  }\n"
                               + "}"))
            .isEqualTo(MINIMAL_POLICY);
    }

    @Test
    public void singleElementListsAreWrittenAsNonArrays() {
        assertThat(READER.read("{\n"
                               + "  \"Version\" : \"Version\",\n"
                               + "  \"Statement\" : {\n"
                               + "    \"Sid\" : \"Sid\",\n"
                               + "    \"Effect\" : \"Allow\",\n"
                               + "    \"Principal\" : \"*\",\n"
                               + "    \"NotPrincipal\" : \"*\",\n"
                               + "    \"Action\" : \"1\",\n"
                               + "    \"NotAction\" : \"1\",\n"
                               + "    \"Resource\" : \"1\",\n"
                               + "    \"NotResource\" : \"1\",\n"
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