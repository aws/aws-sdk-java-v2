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

package software.amazon.awssdk.s3.wiretap.testarchive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.s3.wiretap.client.ClientConfig;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.s3.wiretap.client.feature.Feature;

public final class PairwiseCombiner {

    private PairwiseCombiner() {
    }

    public record Axis(Feature<?> feature, List<Category<?>> categories, Category<?> defaultCategory) { }

    public record PartialAssignment(Map<Feature<?>, Category<?>> chosen) { }

    public static List<ClientConfig> cartesian(List<Axis> axes) {
        List<Map<Feature<?>, Category<?>>> combos = new ArrayList<>();
        combos.add(new LinkedHashMap<>());
        for (Axis axis : axes) {
            List<Map<Feature<?>, Category<?>>> next = new ArrayList<>();
            for (Map<Feature<?>, Category<?>> base : combos) {
                for (Category<?> category : axis.categories()) {
                    Map<Feature<?>, Category<?>> extended = new LinkedHashMap<>(base);
                    extended.put(axis.feature(), category);
                    next.add(extended);
                }
            }
            combos = next;
        }
        return combos.stream().map(ClientConfig::new).toList();
    }

    public static List<ClientConfig> combine(List<Axis> crossed, List<Axis> pairwiseAxes) {
        List<ClientConfig> crossedConfigs = cartesian(crossed);
        List<PartialAssignment> rows = pairwise(pairwiseAxes);

        List<ClientConfig> out = new ArrayList<>(crossedConfigs.size() * rows.size());
        for (ClientConfig crossedConfig : crossedConfigs) {
            for (PartialAssignment row : rows) {
                Map<Feature<?>, Category<?>> merged = new LinkedHashMap<>(crossedConfig.entries());
                merged.putAll(row.chosen());
                out.add(new ClientConfig(merged));
            }
        }
        return out;
    }

    public static List<PartialAssignment> pairwise(List<Axis> pairwiseAxes) {
        List<Axis> varying = pairwiseAxes.stream()
                .filter(a -> !nonDefaultCategories(a).isEmpty())
                .toList();
        if (varying.size() < 2) {
            return cartesian(pairwiseAxes).stream()
                    .map(c -> new PartialAssignment(new LinkedHashMap<>(c.entries())))
                    .toList();
        }

        List<PartialAssignment> rows = new ArrayList<>();
        for (int i = 0; i < varying.size(); i++) {
            for (int j = i + 1; j < varying.size(); j++) {
                for (Category<?> a : nonDefaultCategories(varying.get(i))) {
                    for (Category<?> b : nonDefaultCategories(varying.get(j))) {
                        Map<Feature<?>, Category<?>> chosen = allDefaults(pairwiseAxes);
                        chosen.put(varying.get(i).feature(), a);
                        chosen.put(varying.get(j).feature(), b);
                        rows.add(new PartialAssignment(chosen));
                    }
                }
            }
        }
        return rows;
    }

    private static Map<Feature<?>, Category<?>> allDefaults(List<Axis> axes) {
        Map<Feature<?>, Category<?>> defaults = new LinkedHashMap<>();
        for (Axis axis : axes) {
            defaults.put(axis.feature(), axis.defaultCategory());
        }
        return defaults;
    }

    private static List<Category<?>> nonDefaultCategories(Axis axis) {
        Class<?> defaultClass = axis.defaultCategory().getClass();
        return axis.categories().stream()
                .filter(c -> c.getClass() != defaultClass)
                .toList();
    }
}
