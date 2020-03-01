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

package software.amazon.awssdk.core.auth.policy.internal;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Condition;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Serializes an AWS policy object to a JSON string, suitable for sending to an
 * AWS service.
 */
public class JsonPolicyWriter {
    private static final Logger log = LoggerFactory.getLogger(JsonPolicyWriter.class);

    /** The JSON Generator to generator a JSON string.*/
    private JsonGenerator generator = null;
    /** The output writer to which the JSON String is written.*/
    private Writer writer;

    /**
     * Constructs a new instance of JSONPolicyWriter.
     */
    public JsonPolicyWriter() {
        writer = new StringWriter();
        generator = invokeSafely(() -> JacksonUtils.jsonGeneratorOf(writer));
    }

    /**
     * Converts the specified AWS policy object to a JSON string, suitable for
     * passing to an AWS service.
     *
     * @param policy
     *            The AWS policy object to convert to a JSON string.
     *
     * @return The JSON string representation of the specified policy object.
     *
     * @throws IllegalArgumentException
     *             If the specified policy is null or invalid and cannot be
     *             serialized to a JSON string.
     */
    public String writePolicyToString(Policy policy) {

        if (!isNotNull(policy)) {
            throw new IllegalArgumentException("Policy cannot be null");
        }

        try {
            return jsonStringOf(policy);
        } catch (Exception e) {
            String message = "Unable to serialize policy to JSON string: "
                             + e.getMessage();
            throw new IllegalArgumentException(message, e);
        } finally {
            IoUtils.closeQuietly(writer, log);
        }
    }

    /**
     * Converts the given <code>Policy</code> into a JSON String.
     *
     * @param policy
     *            the policy to be converted.
     * @return a JSON String of the specified policy object.
     */
    private String jsonStringOf(Policy policy) throws IOException {
        generator.writeStartObject();

        writeJsonKeyValue(JsonDocumentField.VERSION, policy.getVersion());

        if (isNotNull(policy.getId())) {
            writeJsonKeyValue(JsonDocumentField.POLICY_ID, policy.getId());
        }

        writeJsonArrayStart(JsonDocumentField.STATEMENT);

        for (Statement statement : policy.getStatements()) {
            generator.writeStartObject();

            if (isNotNull(statement.getId())) {
                writeJsonKeyValue(JsonDocumentField.STATEMENT_ID, statement.getId());
            }
            writeJsonKeyValue(JsonDocumentField.STATEMENT_EFFECT, statement
                    .getEffect().toString());

            List<Principal> principals = statement.getPrincipals();
            if (isNotNull(principals) && !principals.isEmpty()) {
                writePrincipals(principals);
            }

            List<Action> actions = statement.getActions();
            if (isNotNull(actions) && !actions.isEmpty()) {
                writeActions(actions);
            }

            List<Resource> resources = statement.getResources();
            if (isNotNull(resources) && !resources.isEmpty()) {
                writeResources(resources);
            }

            List<Condition> conditions = statement.getConditions();
            if (isNotNull(conditions) && !conditions.isEmpty()) {
                writeConditions(conditions);
            }

            generator.writeEndObject();
        }

        writeJsonArrayEnd();

        generator.writeEndObject();

        generator.flush();

        return writer.toString();

    }

    /**
     * Writes the list of conditions to the JSONGenerator.
     *
     * @param conditions
     *            the conditions to be written.
     */
    private void writeConditions(List<Condition> conditions) throws IOException {
        Map<String, ConditionsByKey> conditionsByType = groupConditionsByTypeAndKey(conditions);

        writeJsonObjectStart(JsonDocumentField.CONDITION);

        ConditionsByKey conditionsByKey;
        for (Map.Entry<String, ConditionsByKey> entry : conditionsByType
                .entrySet()) {
            conditionsByKey = conditionsByType.get(entry.getKey());

            writeJsonObjectStart(entry.getKey());
            for (String key : conditionsByKey.keySet()) {
                writeJsonArray(key, conditionsByKey.getConditionsByKey(key));
            }
            writeJsonObjectEnd();
        }
        writeJsonObjectEnd();
    }

    /**
     * Writes the list of <code>Resource</code>s to the JSONGenerator.
     *
     * @param resources
     *            the list of resources to be written.
     */
    private void writeResources(List<Resource> resources) throws IOException {

        List<String> resourceStrings = new ArrayList<>();

        for (Resource resource : resources) {
            resourceStrings.add(resource.getId());
        }
        writeJsonArray(JsonDocumentField.RESOURCE, resourceStrings);
    }

    /**
     * Writes the list of <code>Action</code>s to the JSONGenerator.
     *
     * @param actions
     *            the list of the actions to be written.
     */
    private void writeActions(List<Action> actions) throws IOException {
        List<String> actionStrings = new ArrayList<>();

        for (Action action : actions) {
            actionStrings.add(action.getActionName());
        }
        writeJsonArray(JsonDocumentField.ACTION, actionStrings);
    }

    /**
     * Writes the list of <code>Principal</code>s to the JSONGenerator.
     *
     * @param principals
     *            the list of principals to be written.
     */
    private void writePrincipals(List<Principal> principals) throws IOException {
        if (principals.size() == 1 && principals.get(0).equals(Principal.ALL)) {
            writeJsonKeyValue(JsonDocumentField.PRINCIPAL, Principal.ALL.getId());
        } else {
            writeJsonObjectStart(JsonDocumentField.PRINCIPAL);

            Map<String, List<String>> principalsByScheme = groupPrincipalByScheme(principals);

            List<String> principalValues;
            for (Map.Entry<String, List<String>> entry : principalsByScheme.entrySet()) {
                principalValues = principalsByScheme.get(entry.getKey());

                if (principalValues.size() == 1) {
                    writeJsonKeyValue(entry.getKey(), principalValues.get(0));
                } else {
                    writeJsonArray(entry.getKey(), principalValues);
                }

            }
            writeJsonObjectEnd();
        }
    }

    /**
     * Groups the list of <code>Principal</code>s by the Scheme.
     *
     * @param principals
     *            the list of <code>Principal</code>s
     * @return a map grouped by scheme of the principal.
     */
    private Map<String, List<String>> groupPrincipalByScheme(
            List<Principal> principals) {
        Map<String, List<String>> principalsByScheme = new LinkedHashMap<>();

        String provider;
        List<String> principalValues;
        for (Principal principal : principals) {
            provider = principal.getProvider();
            if (!principalsByScheme.containsKey(provider)) {
                principalsByScheme.put(provider, new ArrayList<>());
            }
            principalValues = principalsByScheme.get(provider);
            principalValues.add(principal.getId());
        }

        return principalsByScheme;
    }

    /**
     * Groups the list of <code>Condition</code>s by the condition type and
     * condition key.
     *
     * @param conditions
     *            the list of conditions to be grouped
     * @return a map of conditions grouped by type and then key.
     */
    private Map<String, ConditionsByKey> groupConditionsByTypeAndKey(
            List<Condition> conditions) {
        Map<String, ConditionsByKey> conditionsByType = new LinkedHashMap<>();

        String type;
        String key;
        ConditionsByKey conditionsByKey;
        for (Condition condition : conditions) {
            type = condition.getType();
            key = condition.getConditionKey();

            if (!(conditionsByType.containsKey(type))) {
                conditionsByType.put(type, new ConditionsByKey());
            }

            conditionsByKey = conditionsByType.get(type);
            conditionsByKey.addValuesToKey(key, condition.getValues());
        }
        return conditionsByType;
    }

    /**
     * Writes an array along with its values to the JSONGenerator.
     *
     * @param arrayName
     *            name of the JSON array.
     * @param values
     *            values of the JSON array.
     */
    private void writeJsonArray(String arrayName, List<String> values) throws IOException {
        writeJsonArrayStart(arrayName);
        for (String value : values) {
            generator.writeString(value);
        }
        writeJsonArrayEnd();
    }

    /**
     * Writes the Start of Object String to the JSONGenerator along with Object
     * Name.
     *
     * @param fieldName
     *            name of the JSON Object.
     */
    private void writeJsonObjectStart(String fieldName) throws IOException {
        generator.writeObjectFieldStart(fieldName);
    }

    /**
     * Writes the End of Object String to the JSONGenerator.
     */
    private void writeJsonObjectEnd() throws IOException {
        generator.writeEndObject();
    }

    /**
     * Writes the Start of Array String to the JSONGenerator along with Array
     * Name.
     *
     * @param fieldName
     *            name of the JSON array
     */
    private void writeJsonArrayStart(String fieldName) throws IOException {
        generator.writeArrayFieldStart(fieldName);
    }

    /**
     * Writes the End of Array String to the JSONGenerator.
     */
    private void writeJsonArrayEnd() throws IOException {
        generator.writeEndArray();
    }

    /**
     * Writes the given field and the value to the JsonGenerator
     *
     * @param fieldName
     *            the JSON field name
     * @param value
     *            value for the field
     */
    private void writeJsonKeyValue(String fieldName, String value) throws IOException {
        generator.writeStringField(fieldName, value);
    }

    /**
     * Checks if the given object is not null.
     *
     * @param object
     *            the object compared to null.
     * @return true if the object is not null else false
     */
    private boolean isNotNull(Object object) {
        return null != object;
    }

    /**
     * Inner class to hold condition values for each key under a condition type.
     */
    static class ConditionsByKey {
        private Map<String, List<String>> conditionsByKey;

        ConditionsByKey() {
            conditionsByKey = new LinkedHashMap<>();
        }

        public Map<String, List<String>> getConditionsByKey() {
            return conditionsByKey;
        }

        public void setConditionsByKey(Map<String, List<String>> conditionsByKey) {
            this.conditionsByKey = conditionsByKey;
        }

        public boolean containsKey(String key) {
            return conditionsByKey.containsKey(key);
        }

        public List<String> getConditionsByKey(String key) {
            return conditionsByKey.get(key);
        }

        public Set<String> keySet() {
            return conditionsByKey.keySet();
        }

        public void addValuesToKey(String key, List<String> values) {

            List<String> conditionValues = getConditionsByKey(key);
            if (conditionValues == null) {
                conditionsByKey.put(key, new ArrayList<>(values));
            } else {
                conditionValues.addAll(values);
            }
        }
    }
}
