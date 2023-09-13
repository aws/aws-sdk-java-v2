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

package software.amazon.awssdk.policybuilder.iam.internal;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamPolicyReader}.
 *
 * @see IamPolicyReader#create
 */
@SdkInternalApi
public final class DefaultIamPolicyReader implements IamPolicyReader {
    private static final JsonNodeParser JSON_NODE_PARSER = JsonNodeParser.create();

    @Override
    public IamPolicy read(String policy) {
        return read(policy.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IamPolicy read(byte[] policy) {
        return read(new ByteArrayInputStream(policy));
    }

    @Override
    public IamPolicy read(InputStream policy) {
        return readPolicy(JSON_NODE_PARSER.parse(policy));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    private IamPolicy readPolicy(JsonNode policyNode) {
        Map<String, JsonNode> policyObject = expectObject(policyNode, "Policy did not start with {");

        return IamPolicy.builder()
                        .version(getString(policyObject, "Version"))
                        .id(getString(policyObject, "Id"))
                        .statements(readStatements(policyObject.get("Statement")))
                        .build();
    }

    private List<IamStatement> readStatements(JsonNode statementsNode) {
        if (statementsNode == null) {
            return null;
        }

        if (statementsNode.isArray()) {
            return statementsNode.asArray()
                                 .stream()
                                 .map(n -> expectObject(n, "Statement entry"))
                                 .map(this::readStatement)
                                 .collect(toList());
        }

        if (statementsNode.isObject()) {
            return singletonList(readStatement(statementsNode.asObject()));
        }

        throw new IllegalArgumentException("Statement was not an array or object.");
    }

    private IamStatement readStatement(Map<String, JsonNode> statementObject) {
        return IamStatement.builder()
                           .sid(getString(statementObject, "Sid"))
                           .effect(getString(statementObject, "Effect"))
                           .principals(readPrincipals(statementObject, "Principal"))
                           .notPrincipals(readPrincipals(statementObject, "NotPrincipal"))
                           .actionIds(readStringOrArrayAsList(statementObject, "Action"))
                           .notActionIds(readStringOrArrayAsList(statementObject, "NotAction"))
                           .resourceIds(readStringOrArrayAsList(statementObject, "Resource"))
                           .notResourceIds(readStringOrArrayAsList(statementObject, "NotResource"))
                           .conditions(readConditions(statementObject.get("Condition")))
                           .build();
    }

    private List<IamPrincipal> readPrincipals(Map<String, JsonNode> statementObject, String name) {
        JsonNode principalsNode = statementObject.get(name);

        if (principalsNode == null) {
            return null;
        }

        if (principalsNode.isString() && principalsNode.asString().equals(IamPrincipal.ALL.id())) {
            return singletonList(IamPrincipal.ALL);
        }

        if (principalsNode.isObject()) {
            List<IamPrincipal> result = new ArrayList<>();
            Map<String, JsonNode> principalsNodeObject = principalsNode.asObject();
            principalsNodeObject.keySet().forEach(
                k -> result.addAll(IamPrincipal.createAll(k, readStringOrArrayAsList(principalsNodeObject, k)))
            );
            return result;
        }

        throw new IllegalArgumentException(name + " was not \"" + IamPrincipal.ALL.id() + "\" or an object");
    }

    private List<IamCondition> readConditions(JsonNode conditionNode) {
        if (conditionNode == null) {
            return null;
        }

        Map<String, JsonNode> conditionObject = expectObject(conditionNode, "Condition");

        List<IamCondition> result = new ArrayList<>();

        conditionObject.forEach((operator, keyValueNode) -> {
            Map<String, JsonNode> keyValueObject = expectObject(keyValueNode, "Condition key");
            keyValueObject.forEach((key, value) -> {
                if (value.isString()) {
                    result.add(IamCondition.create(operator, key, value.asString()));
                } else if (value.isArray()) {
                    List<String> values =
                        value.asArray()
                             .stream()
                             .map(valueNode -> expectString(valueNode, "Condition values entry"))
                             .collect(toList());
                    result.addAll(IamCondition.createAll(operator, key, values));
                }
            });

        });

        return result;
    }

    private List<String> readStringOrArrayAsList(Map<String, JsonNode> statementObject, String nodeKey) {
        JsonNode node = statementObject.get(nodeKey);

        if (node == null) {
            return null;
        }

        if (node.isString()) {
            return singletonList(node.asString());
        }

        if (node.isArray()) {
            return node.asArray()
                       .stream()
                       .map(n -> expectString(n, nodeKey + " entry"))
                       .collect(toList());
        }

        throw new IllegalArgumentException(nodeKey + " was not an array or string");
    }

    private String getString(Map<String, JsonNode> object, String key) {
        JsonNode node = object.get(key);
        if (node == null) {
            return null;
        }

        return expectString(node, key);
    }

    private String expectString(JsonNode node, String name) {
        Validate.isTrue(node.isString(), "%s was not a string", name);
        return node.asString();
    }

    private Map<String, JsonNode> expectObject(JsonNode node, String name) {
        Validate.isTrue(node.isObject(), "%s was not an object", name);
        return node.asObject();
    }
}
