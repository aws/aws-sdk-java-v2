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

package software.amazon.awssdk.codegen.parity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Compares two {@link IntermediateModel}s by serializing both to JSON and
 * walking the trees. Emits one {@link ParityDiff} per leaf-level mismatch.
 * Arrays are sorted by content key before comparison so member ordering does
 * not produce false positives.
 */
final class IntermediateModelParityChecker {

    private static final Set<String> GLOBAL_IGNORE_PATHS;

    static {
        Set<String> g = new LinkedHashSet<>();
        GLOBAL_IGNORE_PATHS = Collections.unmodifiableSet(g);
    }

    private static final String[] SORT_KEY_FIELDS = {
        "c2jName", "exceptionName", "name", "value"
    };

    private final ObjectMapper mapper;
    private final List<ParityAllowlistEntry> globalIgnoreEntries;

    IntermediateModelParityChecker() {
        this.mapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .registerModule(new Jdk8Module());
        this.globalIgnoreEntries = new ArrayList<>(GLOBAL_IGNORE_PATHS.size());
        for (String path : GLOBAL_IGNORE_PATHS) {
            globalIgnoreEntries.add(new ParityAllowlistEntry(path, "global ignore"));
        }
    }

    ParityResult compare(String service, IntermediateModel c2j, IntermediateModel smithy) {
        return compare(service, c2j, smithy, Collections.emptyList());
    }

    ParityResult compare(String service,
                         IntermediateModel c2j,
                         IntermediateModel smithy,
                         List<ParityAllowlistEntry> serviceAllowlist) {
        return compareTrees(service, mapper.valueToTree(c2j), mapper.valueToTree(smithy), serviceAllowlist);
    }

    ParityResult compareTrees(String service,
                              JsonNode c2jTree,
                              JsonNode smithyTree,
                              List<ParityAllowlistEntry> serviceAllowlist) {
        List<ParityDiff> all = new ArrayList<>();
        computeDiffs("", c2jTree, smithyTree, all);

        List<ParityAllowlistEntry> combined = new ArrayList<>(globalIgnoreEntries);
        combined.addAll(serviceAllowlist);

        List<ParityDiff> unexpected = new ArrayList<>(all.size());
        for (ParityDiff diff : all) {
            if (!isAllowed(diff, combined)) {
                unexpected.add(diff);
            }
        }
        return new ParityResult(service, all, unexpected);
    }

    /**
     * Load a per-service allowlist from a JSON file.
     *
     * <p>Format: a flat JSON object mapping diff path to reason.
     *
     * <pre>{@code
     * {
     *   "metadata.apiVersion": "Smithy uses compact date format"
     * }
     * }</pre>
     */
    List<ParityAllowlistEntry> loadAllowlist(InputStream in) throws IOException {
        if (in == null) {
            return Collections.emptyList();
        }
        Map<String, String> raw = mapper.readValue(in, new TypeReference<LinkedHashMap<String, String>>() { });
        List<ParityAllowlistEntry> entries = new ArrayList<>(raw.size());
        for (Map.Entry<String, String> e : raw.entrySet()) {
            entries.add(new ParityAllowlistEntry(e.getKey(), e.getValue()));
        }
        return entries;
    }

    private static boolean isAllowed(ParityDiff diff, List<ParityAllowlistEntry> allowlist) {
        for (ParityAllowlistEntry entry : allowlist) {
            if (entry.matches(diff.path())) {
                return true;
            }
        }
        return false;
    }

    private void computeDiffs(String path, JsonNode c2j, JsonNode smithy, List<ParityDiff> out) {
        if (c2j == null && smithy == null) {
            return;
        }
        if (c2j == null || c2j.isNull()) {
            if (smithy != null && !smithy.isNull()) {
                out.add(new ParityDiff(path, ParityDiff.Type.ADDED, null, truncate(smithy.toString())));
            }
            return;
        }
        if (smithy == null || smithy.isNull()) {
            out.add(new ParityDiff(path, ParityDiff.Type.MISSING, truncate(c2j.toString()), null));
            return;
        }
        if (c2j.getNodeType() != smithy.getNodeType()) {
            out.add(new ParityDiff(path, ParityDiff.Type.TYPE_MISMATCH,
                                   c2j.getNodeType().name(),
                                   smithy.getNodeType().name()));
            return;
        }

        if (c2j.isObject()) {
            compareObjects(path, (ObjectNode) c2j, (ObjectNode) smithy, out);
        } else if (c2j.isArray()) {
            compareArrays(path, c2j, smithy, out);
        } else {
            if (!c2j.equals(smithy)) {
                out.add(new ParityDiff(path, ParityDiff.Type.CHANGED,
                                       truncate(c2j.toString()),
                                       truncate(smithy.toString())));
            }
        }
    }

    private void compareObjects(String path, ObjectNode c2j, ObjectNode smithy, List<ParityDiff> out) {
        Set<String> fields = new LinkedHashSet<>();
        Iterator<String> c2jFields = c2j.fieldNames();
        while (c2jFields.hasNext()) {
            fields.add(c2jFields.next());
        }
        Iterator<String> smithyFields = smithy.fieldNames();
        while (smithyFields.hasNext()) {
            fields.add(smithyFields.next());
        }
        for (String field : fields) {
            String childPath = path.isEmpty() ? field : path + "." + field;
            computeDiffs(childPath, c2j.get(field), smithy.get(field), out);
        }
    }

    private void compareArrays(String path, JsonNode c2j, JsonNode smithy, List<ParityDiff> out) {
        List<JsonNode> c2jSorted = sortArray(c2j);
        List<JsonNode> smithySorted = sortArray(smithy);
        int maxLen = Math.max(c2jSorted.size(), smithySorted.size());
        for (int i = 0; i < maxLen; i++) {
            String childPath = path + "[" + i + "]";
            JsonNode c2jElem = i < c2jSorted.size() ? c2jSorted.get(i) : null;
            JsonNode smithyElem = i < smithySorted.size() ? smithySorted.get(i) : null;
            computeDiffs(childPath, c2jElem, smithyElem, out);
        }
    }

    private static List<JsonNode> sortArray(JsonNode array) {
        List<JsonNode> sorted = new ArrayList<>();
        for (JsonNode element : array) {
            sorted.add(element);
        }
        sorted.sort((a, b) -> sortKey(a).compareTo(sortKey(b)));
        return sorted;
    }

    private static String sortKey(JsonNode node) {
        if (node.isObject()) {
            for (String field : SORT_KEY_FIELDS) {
                JsonNode candidate = node.get(field);
                if (candidate != null && candidate.isTextual()) {
                    return candidate.asText();
                }
            }
            return node.toString();
        }
        return node.asText();
    }

    private static String truncate(String value) {
        int max = 120;
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...(" + value.length() + " chars)";
    }

    static Set<String> globalIgnorePathsForTest() {
        return GLOBAL_IGNORE_PATHS;
    }
}
