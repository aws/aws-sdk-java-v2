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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.smithy.traits.PaginatorOverrideTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Builds a service's paginator definitions, keyed by operation name as in {@code paginators-1.json}. Smithy
 * counterpart to loading that file.
 *
 * <p>An operation's {@link PaginatorOverrideTrait} wins. Otherwise its {@code @paginated} trait is translated, with the
 * service's {@code @paginated} filling the members the operation leaves out, as Smithy's {@code PaginatedIndex} does.
 */
final class AddSmithyPaginators {

    private AddSmithyPaginators() {
    }

    static Map<String, PaginatorDefinition> paginators(Model model, ServiceShape service) {
        PaginatedTrait serviceDefaults = service.getTrait(PaginatedTrait.class).orElse(null);
        Map<String, PaginatorDefinition> paginators = new TreeMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            String name = operation.getId().getName();
            Optional<PaginatorOverrideTrait> override = operation.getTrait(PaginatorOverrideTrait.class);
            if (override.isPresent()) {
                if (!override.get().isSkip()) {
                    paginators.put(name, fromOverride(override.get()));
                }
                continue;
            }
            operation.getTrait(PaginatedTrait.class)
                     .ifPresent(trait -> paginators.put(name, fromPaginated(trait.merge(serviceDefaults))));
        }
        return paginators;
    }

    /**
     * {@code @paginated} has no {@code moreResults} and one member per field, so each list has at most one element.
     */
    private static PaginatorDefinition fromPaginated(PaginatedTrait trait) {
        PaginatorDefinition definition = new PaginatorDefinition();
        definition.setInputToken(trait.getInputToken().map(Collections::singletonList).orElse(null));
        definition.setOutputToken(trait.getOutputToken().map(Collections::singletonList).orElse(null));
        definition.setResultKey(trait.getItems().map(Collections::singletonList).orElse(null));
        definition.setLimitKey(trait.getPageSize().orElse(null));
        return definition;
    }

    private static PaginatorDefinition fromOverride(PaginatorOverrideTrait override) {
        PaginatorDefinition definition = new PaginatorDefinition();
        definition.setInputToken(override.getInputToken());
        definition.setOutputToken(override.getOutputToken());
        definition.setResultKey(override.getResultKey().orElse(null));
        definition.setMoreResults(override.getMoreResults().orElse(null));
        definition.setLimitKey(override.getLimitKey().orElse(null));
        return definition;
    }
}
