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

package software.amazon.awssdk.core.auth.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.auth.policy.Principal.Service;
import software.amazon.awssdk.core.auth.policy.Principal.WebIdentityProvider;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.core.auth.policy.conditions.IpAddressCondition;
import software.amazon.awssdk.core.auth.policy.conditions.IpAddressCondition.IpAddressComparisonType;
import software.amazon.awssdk.core.auth.policy.conditions.StringCondition;
import software.amazon.awssdk.core.auth.policy.conditions.StringCondition.StringComparisonType;
import software.amazon.awssdk.core.auth.policy.internal.JacksonUtils;

/**
 * Unit tests for constructing policy objects and serializing them to JSON.
 */
public class PolicyTest {

    @Test
    public void testPrincipals() {
        Policy policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(new Principal("accountId1"),
                                                      new Principal("accountId2"))
                                      .withActions(new Action("action")));

        JsonNode jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        JsonNode statementArray = jsonPolicyNode.get("Statement");

        Assertions.assertTrue(statementArray.isArray());
        Assertions.assertTrue(statementArray.size() == 1);

        JsonNode statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        JsonNode users = statement.get("Principal").get("AWS");
        Assertions.assertEquals(2, users.size());
        Assertions.assertEquals(users.get(0).asText(), "accountId1");
        Assertions.assertEquals(users.get(1).asText(), "accountId2");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(new Principal(Principal.Service.AmazonEC2),
                                                      new Principal(Principal.Service.AmazonElasticTranscoder))
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        JsonNode services = statement.get("Principal").get("Service");
        Assertions.assertTrue(services.isArray());
        Assertions.assertTrue(services.size() == 2);
        Assertions.assertEquals(Service.AmazonEC2.getServiceId(), services.get(0)
                                                                          .asText());
        Assertions.assertEquals(Principal.Service.AmazonElasticTranscoder.getServiceId(), services
                .get(1).asText());

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(Principal.ALL_USERS)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        Assertions.assertEquals(users.asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(Principal.ALL_SERVICES, Principal.ALL_USERS)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        services = statement.get("Principal").get("Service");
        Assertions.assertEquals(users.asText(), "*");
        Assertions.assertEquals(services.asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(Principal.ALL_SERVICES, Principal.ALL_USERS,
                                                      Principal.ALL_WEB_PROVIDERS)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        services = statement.get("Principal").get("Service");
        JsonNode webProviders = statement.get("Principal").get("Federated");

        Assertions.assertEquals(users.asText(), "*");
        Assertions.assertEquals(services.asText(), "*");
        Assertions.assertEquals(webProviders.asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(Principal.ALL_SERVICES, Principal.ALL_USERS,
                                                      Principal.ALL_WEB_PROVIDERS, Principal.ALL)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        services = statement.get("Principal").get("Service");
        webProviders = statement.get("Principal").get("Federated");
        JsonNode allUsers = statement.get("Principal").get("*");

        Assertions.assertEquals(users.asText(), "*");
        Assertions.assertEquals(services.asText(), "*");
        Assertions.assertEquals(webProviders.asText(), "*");
        Assertions.assertEquals(allUsers.asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(new Principal("accountId1"), Principal.ALL_USERS)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        Assertions.assertTrue(users.isArray());
        Assertions.assertEquals(users.get(0).asText(), "accountId1");
        Assertions.assertEquals(users.get(1).asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(new Principal(Principal.Service.AmazonEC2),
                                                      Principal.ALL_SERVICES, new Principal("accountId1"))
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        services = statement.get("Principal").get("Service");

        Assertions.assertEquals(users.asText(), "accountId1");
        Assertions.assertEquals(services.get(0).asText(),
                                Service.AmazonEC2.getServiceId());
        Assertions.assertEquals(services.get(1).asText(), "*");

        policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(new Resource("resource"))
                                      .withPrincipals(new Principal(Service.AmazonEC2),
                                                      Principal.ALL_SERVICES, new Principal("accountId1"),
                                                      new Principal(WebIdentityProvider.Amazon),
                                                      Principal.ALL_WEB_PROVIDERS)
                                      .withActions(new Action("action")));

        jsonPolicyNode = JacksonUtils.jsonNodeOf(policy.toJson());
        statementArray = jsonPolicyNode.get("Statement");
        Assertions.assertTrue(statementArray.size() == 1);

        statement = statementArray.get(0);
        Assertions.assertTrue(statement.has("Resource"));
        Assertions.assertTrue(statement.has("Principal"));
        Assertions.assertTrue(statement.has("Action"));
        Assertions.assertTrue(statement.has("Effect"));

        users = statement.get("Principal").get("AWS");
        services = statement.get("Principal").get("Service");
        webProviders = statement.get("Principal").get("Federated");

        Assertions.assertEquals(services.get(0).asText(),
                                Service.AmazonEC2.getServiceId());
        Assertions.assertEquals(services.get(1).asText(), "*");
        Assertions.assertEquals(users.asText(), "accountId1");
        Assertions.assertEquals(webProviders.get(0).asText(),
                                WebIdentityProvider.Amazon.getWebIdentityProvider());
        Assertions.assertEquals(webProviders.get(1).asText(), "*");
    }

    /**
     * Policies with multiple conditions that use the same comparison type must
     * be merged together in the JSON format, otherwise there will be two keys
     * with the same name and one will override the other.
     */
    @Test
    public void testMultipleConditionKeysForConditionType() throws Exception {
        Policy policy = new Policy();
        policy.withStatements(new Statement(Effect.Allow)
                                      .withResources(
                                              new Resource(
                                                      "arn:aws:sqs:us-east-1:987654321000:MyQueue"))
                                      .withPrincipals(Principal.ALL_USERS)
                                      .withActions(new Action("foo"))
                                      .withConditions(
                                              new StringCondition(StringComparisonType.StringNotLike,
                                                                  "key1", "foo"),
                                              new StringCondition(StringComparisonType.StringNotLike,
                                                                  "key1", "bar")));

        JsonNode jsonPolicy = JacksonUtils.jsonNodeOf(policy.toJson());

        JsonNode statementArray = jsonPolicy.get("Statement");
        Assertions.assertEquals(statementArray.size(), 1);
        JsonNode conditions = statementArray.get(0).get("Condition");
        Assertions.assertEquals(conditions.size(), 1);

        JsonNode stringLikeCondition = conditions.get(StringComparisonType.StringNotLike.toString());
        Assertions.assertTrue(stringLikeCondition.has("key1"));
        Assertions.assertFalse(stringLikeCondition.has("key2"));
        assertValidStatementIds(policy);
    }

    /**
     * Tests serializing a more complex policy object with multiple statements.
     */
    @Test
    public void testMultipleStatements() throws Exception {
        Policy policy = new Policy("S3PolicyId1");
        policy.withStatements(
                new Statement(Effect.Allow)
                        .withPrincipals(Principal.ALL_USERS)
                        .withActions(new Action("action1"))
                        .withResources(new Resource("resource"))
                        .withConditions(
                                new IpAddressCondition("192.168.143.0/24"),
                                new IpAddressCondition(IpAddressComparisonType.NotIpAddress, "192.168.143.188/32")),
                new Statement(Effect.Deny).withPrincipals(Principal.ALL_USERS)
                                          .withActions(new Action("action2"))
                                          .withResources(new Resource("resource"))
                                          .withConditions(new IpAddressCondition("10.1.2.0/24")));

        JsonNode jsonPolicy = JacksonUtils.jsonNodeOf(policy.toJson());
        Assertions.assertTrue(jsonPolicy.has("Id"));

        JsonNode statementArray = jsonPolicy.get("Statement");
        Assertions.assertEquals(statementArray.size(), 2);
        assertValidStatementIds(policy);

        JsonNode statement;
        for (int i = 0; i < statementArray.size(); i++) {
            statement = statementArray.get(i);
            Assertions.assertTrue(statement.has("Sid"));
            Assertions.assertTrue(statement.has("Effect"));
            Assertions.assertTrue(statement.has("Principal"));
            Assertions.assertTrue(statement.has("Action"));
            Assertions.assertTrue(statement.has("Resource"));
            Assertions.assertTrue(statement.has("Condition"));
        }
    }

    /**
     * Tests that a policy correctly assigns unique statement IDs to any added
     * statements without IDs yet.
     */
    @Test
    public void testStatementIdAssignment() throws Exception {
        Policy policy = new Policy("S3PolicyId1");
        policy.withStatements(
                new Statement(Effect.Allow).withId("0")
                                           .withPrincipals(Principal.ALL_USERS)
                                           .withActions(new Action("action1")),
                new Statement(Effect.Allow).withId("1")
                                           .withPrincipals(Principal.ALL_USERS)
                                           .withActions(new Action("action1")), new Statement(
                        Effect.Deny).withPrincipals(Principal.ALL_USERS)
                                    .withActions(new Action("action2")));

        assertValidStatementIds(policy);
    }

    /**
     * Asserts that each statement in the specified policy has a unique ID
     * assigned to it.
     */
    private void assertValidStatementIds(Policy policy) {
        Set<String> statementIds = new HashSet<String>();
        for (Statement statement : policy.getStatements()) {
            Assertions.assertNotNull(statement.getId());
            Assertions.assertFalse(statementIds.contains(statement.getId()));
            statementIds.add(statement.getId());
        }
    }

    @Test
    public void testPrincipalAccountId() {
        String ID_WITH_HYPHEN = "a-b-c-d-e-f-g";
        String ID_WITHOUT_HYPHEN = "abcdefg";

        Assertions.assertEquals(ID_WITHOUT_HYPHEN,
                                new Principal(ID_WITH_HYPHEN).getId());
        Assertions.assertEquals(ID_WITHOUT_HYPHEN,
                                new Principal("AWS", ID_WITH_HYPHEN).getId());

        Assertions.assertEquals(ID_WITH_HYPHEN,
                                new Principal("Federated", ID_WITH_HYPHEN).getId());
        Assertions.assertEquals(ID_WITH_HYPHEN,
                                new Principal("AWS", ID_WITH_HYPHEN, false).getId());
    }
}
