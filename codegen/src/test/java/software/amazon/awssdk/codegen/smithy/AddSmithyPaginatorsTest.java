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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class AddSmithyPaginatorsTest {

    private static final ClassLoader LOADER = AddSmithyPaginatorsTest.class.getClassLoader();

    private static Map<String, PaginatorDefinition> paginators(String serviceTraits, String operationTraits) {
        String src = "$version: \"2.0\"\nnamespace demo\n\n"
                     + "use software.amazon.awssdk.codegen#paginatorOverride\n\n"
                     + serviceTraits
                     + "service DemoService { version: \"2024-01-01\", operations: [Op, Other] }\n\n"
                     + operationTraits
                     + "operation Op { input: OpRequest, output: OpResponse }\n"
                     + "operation Other { input: OpRequest, output: OpResponse }\n"
                     + "structure OpRequest { nextToken: String, maxResults: Integer }\n"
                     + "structure OpResponse { nextToken: String, items: StringList, result: Result }\n"
                     + "structure Result { next: String, items: StringList }\n"
                     + "list StringList { member: String }\n";
        Model model = Model.assembler(LOADER).discoverModels(LOADER).addUnparsedModel("test.smithy", src)
                           .assemble().unwrap();
        return AddSmithyPaginators.paginators(model, model.expectShape(ShapeId.from("demo#DemoService"),
                                                                       ServiceShape.class));
    }

    @Test
    void paginatedTrait_translatesEachMember() {
        Map<String, PaginatorDefinition> paginators = paginators(
            "", "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"items\", "
                + "pageSize: \"maxResults\")\n");

        assertThat(paginators).containsOnlyKeys("Op");
        PaginatorDefinition definition = paginators.get("Op");
        assertThat(definition.getInputToken()).containsExactly("nextToken");
        assertThat(definition.getOutputToken()).containsExactly("nextToken");
        assertThat(definition.getResultKey()).containsExactly("items");
        assertThat(definition.getLimitKey()).isEqualTo("maxResults");
        assertThat(definition.getMoreResults()).isNull();
    }

    @Test
    void absentItemsAndPageSize_leaveResultKeyAndLimitKeyNull() {
        PaginatorDefinition definition =
            paginators("", "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\")\n").get("Op");

        assertThat(definition.getResultKey()).isNull();
        assertThat(definition.getLimitKey()).isNull();
    }

    @Test
    void nestedOutputTokenAndItems_keepTheirPaths() {
        PaginatorDefinition definition = paginators(
            "", "@paginated(inputToken: \"nextToken\", outputToken: \"result.next\", items: \"result.items\")\n")
            .get("Op");

        assertThat(definition.getOutputToken()).containsExactly("result.next");
        assertThat(definition.getResultKey()).containsExactly("result.items");
    }

    @Test
    void servicePaginatedTrait_fillsTheMembersTheOperationLeavesOut() {
        PaginatorDefinition definition = paginators(
            "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", pageSize: \"maxResults\")\n",
            "@paginated(items: \"items\")\n").get("Op");

        assertThat(definition.getInputToken()).containsExactly("nextToken");
        assertThat(definition.getOutputToken()).containsExactly("nextToken");
        assertThat(definition.getResultKey()).containsExactly("items");
        assertThat(definition.getLimitKey()).isEqualTo("maxResults");
    }

    @Test
    void override_replacesThePaginatedTrait() {
        PaginatorDefinition definition = paginators(
            "", "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"items\")\n"
                + "@paginatorOverride(inputToken: [\"nextToken\", \"maxResults\"], outputToken: [\"nextToken\", "
                + "\"result.next\"], resultKey: [\"items\", \"result.items\"], moreResults: \"truncated\", "
                + "limitKey: \"maxResults\")\n").get("Op");

        assertThat(definition.getInputToken()).containsExactly("nextToken", "maxResults");
        assertThat(definition.getOutputToken()).containsExactly("nextToken", "result.next");
        assertThat(definition.getResultKey()).containsExactly("items", "result.items");
        assertThat(definition.getMoreResults()).isEqualTo("truncated");
        assertThat(definition.getLimitKey()).isEqualTo("maxResults");
    }

    @Test
    void overrideWithoutPaginatedTrait_isUsed() {
        Map<String, PaginatorDefinition> paginators =
            paginators("", "@paginatorOverride(inputToken: [\"nextToken\"], outputToken: [\"nextToken\"])\n");

        assertThat(paginators).containsOnlyKeys("Op");
        assertThat(paginators.get("Op").getInputToken()).isEqualTo(Collections.singletonList("nextToken"));
        assertThat(paginators.get("Op").getResultKey()).isNull();
    }

    @Test
    void skipOverride_dropsThePaginatedTrait() {
        Map<String, PaginatorDefinition> paginators = paginators(
            "", "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\")\n"
                + "@paginatorOverride(skip: true)\n");

        assertThat(paginators).isEmpty();
    }

    @Test
    void overrideWithAnExpressionToken_isKeptAsWritten() {
        PaginatorDefinition definition = paginators(
            "", "@paginatorOverride(inputToken: [\"nextToken\"], outputToken: [\"nextToken || items[-1]\"])\n")
            .get("Op");

        assertThat(definition.getOutputToken()).isEqualTo(Arrays.asList("nextToken || items[-1]"));
        assertThat(definition.hasAllRequiredFields()).isFalse();
    }
}
