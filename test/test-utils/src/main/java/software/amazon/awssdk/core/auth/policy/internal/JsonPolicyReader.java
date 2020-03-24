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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Condition;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;

/**
 * Generate an AWS policy object by parsing the given JSON string.
 */
public class JsonPolicyReader {

    private static final String PRINCIPAL_SCHEMA_USER = "AWS";

    private static final String PRINCIPAL_SCHEMA_SERVICE = "Service";

    private static final String PRINCIPAL_SCHEMA_FEDERATED = "Federated";

    /**
     * Converts the specified JSON string to an AWS policy object.
     *
     * For more information see, @see
     * http://docs.aws.amazon.com/AWSSdkDocsJava/latest
     * /DeveloperGuide/java-dg-access-control.html
     *
     * @param jsonString
     *            the specified JSON string representation of this AWS access
     *            control policy.
     *
     * @return An AWS policy object.
     *
     * @throws IllegalArgumentException
     *             If the specified JSON string is null or invalid and cannot be
     *             converted to an AWS policy object.
     */
    public Policy createPolicyFromJsonString(String jsonString) {

        if (jsonString == null) {
            throw new IllegalArgumentException("JSON string cannot be null");
        }

        JsonNode policyNode;
        JsonNode idNode;
        JsonNode statementNodes;
        Policy policy = new Policy();
        List<Statement> statements = new LinkedList<>();

        try {
            policyNode = JacksonUtils.jsonNodeOf(jsonString);

            idNode = policyNode.get(JsonDocumentField.POLICY_ID);
            if (isNotNull(idNode)) {
                policy.setId(idNode.asText());
            }

            statementNodes = policyNode.get(JsonDocumentField.STATEMENT);
            if (isNotNull(statementNodes)) {
                for (JsonNode node : statementNodes) {
                    statements.add(statementOf(node));
                }
            }

        } catch (Exception e) {
            String message = "Unable to generate policy object fron JSON string "
                             + e.getMessage();
            throw new IllegalArgumentException(message, e);
        }
        policy.setStatements(statements);
        return policy;
    }

    /**
     * Creates a <code>Statement</code> instance from the statement node.
     *
     * A statement consists of an Effect, id (optional), principal, action, resource,
     * and conditions.
     * <p>
     * principal is the AWS account that is making a request to access or modify one of your AWS resources.
     * <p>
     * action is the way in which your AWS resource is being accessed or modified, such as sending a message to an Amazon
     * SQS queue, or storing an object in an Amazon S3 bucket.
     * <p>
     * resource is the AWS entity that the principal wants to access, such as an Amazon SQS queue, or an object stored in
     * Amazon S3.
     * <p>
     * conditions are the optional constraints that specify when to allow or deny access for the principal to access your
     * resource. Many expressive conditions are available, some specific to each service. For example, you can use date
     * conditions to allow access to your resources only after or before a specific time.
     *
     * @param jStatement
     *            JsonNode representing the statement.
     * @return a reference to the statement instance created.
     */
    private Statement statementOf(JsonNode jStatement) {

        JsonNode effectNode = jStatement.get(JsonDocumentField.STATEMENT_EFFECT);

        Statement.Effect effect = isNotNull(effectNode)
                                        ? Statement.Effect.valueOf(effectNode.asText())
                                        : Statement.Effect.Deny;

        Statement statement = new Statement(effect);

        JsonNode id = jStatement.get(JsonDocumentField.STATEMENT_ID);
        if (isNotNull(id)) {
            statement.setId(id.asText());
        }

        JsonNode actionNodes = jStatement.get(JsonDocumentField.ACTION);
        if (isNotNull(actionNodes)) {
            statement.setActions(actionsOf(actionNodes));
        }

        JsonNode resourceNodes = jStatement.get(JsonDocumentField.RESOURCE);
        if (isNotNull(resourceNodes)) {
            statement.setResources(resourcesOf(resourceNodes));
        }

        JsonNode conditionNodes = jStatement.get(JsonDocumentField.CONDITION);
        if (isNotNull(conditionNodes)) {
            statement.setConditions(conditionsOf(conditionNodes));
        }

        JsonNode principalNodes = jStatement.get(JsonDocumentField.PRINCIPAL);
        if (isNotNull(principalNodes)) {
            statement.setPrincipals(principalOf(principalNodes));
        }

        return statement;
    }

    /**
     * Generates a list of actions from the Action Json Node.
     *
     * @param actionNodes
     *            the action Json node to be parsed.
     * @return the list of actions.
     */
    private List<Action> actionsOf(JsonNode actionNodes) {
        List<Action> actions = new LinkedList<>();

        if (actionNodes.isArray()) {
            for (JsonNode action : actionNodes) {
                actions.add(new Action(action.asText()));
            }
        } else {
            actions.add(new Action(actionNodes.asText()));
        }
        return actions;
    }

    /**
     * Generates a list of resources from the Resource Json Node.
     *
     * @param resourceNodes
     *            the resource Json node to be parsed.
     * @return the list of resources.
     */
    private List<Resource> resourcesOf(JsonNode resourceNodes) {
        List<Resource> resources = new LinkedList<>();

        if (resourceNodes.isArray()) {
            for (JsonNode resource : resourceNodes) {
                resources.add(new Resource(resource.asText()));
            }
        } else {
            resources.add(new Resource(resourceNodes.asText()));
        }

        return resources;
    }

    /**
     * Generates a list of principals from the Principal Json Node
     *
     * @param principalNodes
     *            the principal Json to be parsed
     * @return a list of principals
     */
    private List<Principal> principalOf(JsonNode principalNodes) {
        List<Principal> principals = new LinkedList<>();

        if (principalNodes.asText().equals("*")) {
            principals.add(Principal.ALL);
            return principals;
        }

        Iterator<Map.Entry<String, JsonNode>> mapOfPrincipals = principalNodes
                .fields();
        String schema;
        JsonNode principalNode;
        Entry<String, JsonNode> principal;
        Iterator<JsonNode> elements;
        while (mapOfPrincipals.hasNext()) {
            principal = mapOfPrincipals.next();
            schema = principal.getKey();
            principalNode = principal.getValue();

            if (principalNode.isArray()) {
                elements = principalNode.elements();
                while (elements.hasNext()) {
                    principals.add(createPrincipal(schema, elements.next()));
                }
            } else {
                principals.add(createPrincipal(schema, principalNode));
            }
        }

        return principals;
    }

    /**
     * Creates a new principal instance for the given schema and the Json node.
     *
     * @param schema
     *            the schema for the principal instance being created.
     * @param principalNode
     *            the node indicating the AWS account that is making the
     *            request.
     * @return a principal instance.
     */
    private Principal createPrincipal(String schema, JsonNode principalNode) {
        if (schema.equalsIgnoreCase(PRINCIPAL_SCHEMA_USER)) {
            return new Principal(principalNode.asText());
        } else if (schema.equalsIgnoreCase(PRINCIPAL_SCHEMA_SERVICE)) {
            return new Principal(schema, principalNode.asText());
        } else if (schema.equalsIgnoreCase(PRINCIPAL_SCHEMA_FEDERATED)) {
            if (Principal.WebIdentityProvider.fromString(principalNode.asText()) != null) {
                return new Principal(
                        Principal.WebIdentityProvider.fromString(principalNode.asText()));
            } else {
                return new Principal(PRINCIPAL_SCHEMA_FEDERATED, principalNode.asText());
            }
        }
        throw new IllegalArgumentException("Schema " + schema + " is not a valid value for the principal.");
    }

    /**
     * Generates a list of condition from the Json node.
     *
     * @param conditionNodes
     *            the condition Json node to be parsed.
     * @return the list of conditions.
     */
    private List<Condition> conditionsOf(JsonNode conditionNodes) {

        List<Condition> conditionList = new LinkedList<>();
        Iterator<Map.Entry<String, JsonNode>> mapOfConditions = conditionNodes
                .fields();

        Entry<String, JsonNode> condition;
        while (mapOfConditions.hasNext()) {
            condition = mapOfConditions.next();
            convertConditionRecord(conditionList, condition.getKey(),
                                   condition.getValue());
        }

        return conditionList;
    }

    /**
     * Generates a condition instance for each condition type under the
     * Condition Json node.
     *
     * @param conditions
     *            the complete list of conditions
     * @param conditionType
     *            the condition type for the condition being created.
     * @param conditionNode
     *            each condition node to be parsed.
     */
    private void convertConditionRecord(List<Condition> conditions,
                                        String conditionType, JsonNode conditionNode) {

        Iterator<Map.Entry<String, JsonNode>> mapOfFields = conditionNode
                .fields();
        List<String> values;
        Entry<String, JsonNode> field;
        JsonNode fieldValue;
        Iterator<JsonNode> elements;

        while (mapOfFields.hasNext()) {
            values = new LinkedList<>();
            field = mapOfFields.next();
            fieldValue = field.getValue();

            if (fieldValue.isArray()) {
                elements = fieldValue.elements();
                while (elements.hasNext()) {
                    values.add(elements.next().asText());
                }
            } else {
                values.add(fieldValue.asText());
            }
            conditions.add(new Condition().withType(conditionType)
                                          .withConditionKey(field.getKey()).withValues(values));
        }
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

}
