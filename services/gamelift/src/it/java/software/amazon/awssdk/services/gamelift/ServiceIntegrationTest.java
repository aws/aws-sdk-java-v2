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

package software.amazon.awssdk.services.gamelift;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.gamelift.model.Alias;
import software.amazon.awssdk.services.gamelift.model.CreateAliasRequest;
import software.amazon.awssdk.services.gamelift.model.CreateAliasResponse;
import software.amazon.awssdk.services.gamelift.model.DeleteAliasRequest;
import software.amazon.awssdk.services.gamelift.model.DescribeAliasRequest;
import software.amazon.awssdk.services.gamelift.model.DescribeAliasResponse;
import software.amazon.awssdk.services.gamelift.model.RoutingStrategy;
import software.amazon.awssdk.services.gamelift.model.RoutingStrategyType;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static GameLiftClient gameLift;

    private static String aliasId = null;

    @BeforeClass
    public static void setUp() throws IOException {
        gameLift = GameLiftClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void cleanUp() {
        if (aliasId != null) {
            gameLift.deleteAlias(DeleteAliasRequest.builder().aliasId(aliasId).build());
        }
    }

    @Test
    public void aliasOperations() {
        String aliasName = "alias-foo";
        String fleetId = "fleet-foo";

        CreateAliasResponse createAliasResult = gameLift
                .createAlias(CreateAliasRequest.builder()
                                               .name(aliasName)
                                               .routingStrategy(RoutingStrategy.builder()
                                                                               .type(RoutingStrategyType.SIMPLE)
                                                                               .fleetId(fleetId).build()).build());

        Alias createdAlias = createAliasResult.alias();
        aliasId = createdAlias.aliasId();
        RoutingStrategy strategy = createdAlias.routingStrategy();

        Assert.assertNotNull(createAliasResult);
        Assert.assertNotNull(createAliasResult.alias());
        Assert.assertEquals(createdAlias.name(), aliasName);
        Assert.assertEquals(strategy.type(), RoutingStrategyType.SIMPLE);
        Assert.assertEquals(strategy.fleetId(), fleetId);

        DescribeAliasResponse describeAliasResult = gameLift
                .describeAlias(DescribeAliasRequest.builder().aliasId(aliasId).build());
        Assert.assertNotNull(describeAliasResult);
        Alias describedAlias = describeAliasResult.alias();
        Assert.assertEquals(createdAlias, describedAlias);
    }
}
