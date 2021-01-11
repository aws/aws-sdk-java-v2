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

package software.amazon.awssdk.services.ecs;

import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.CreateClusterRequest;
import software.amazon.awssdk.services.ecs.model.CreateClusterResponse;
import software.amazon.awssdk.services.ecs.model.DeleteClusterRequest;
import software.amazon.awssdk.services.ecs.model.ListClustersRequest;
import software.amazon.awssdk.services.ecs.model.ListTaskDefinitionsRequest;
import software.amazon.awssdk.services.ecs.model.PortMapping;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionResponse;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class EC2ContainerServiceIntegrationTest extends AwsTestBase {

    private static final String CLUSTER_NAME =
            "java-sdk-test-cluster-" + System.currentTimeMillis();

    private static EcsClient client;
    private static String clusterArn;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();

        client = EcsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        CreateClusterResponse result = client.createCluster(CreateClusterRequest.builder()
                .clusterName(CLUSTER_NAME)
                .build());

        Assert.assertEquals(CLUSTER_NAME, result.cluster().clusterName());
        Assert.assertNotNull(result.cluster().clusterArn());
        Assert.assertNotNull(result.cluster().status());

        clusterArn = result.cluster().clusterArn();

        Waiter.run(() -> client.describeClusters(r -> r.clusters(CLUSTER_NAME)))
              .until(resp -> resp.clusters().stream().findFirst().filter(c -> c.status().equals("ACTIVE")).isPresent())
              .orFail();
    }

    @AfterClass
    public static void cleanup() {
        if (client != null) {
            client.deleteCluster(DeleteClusterRequest.builder().cluster(CLUSTER_NAME).build());
        }
    }

    @Test
    public void basicTest() {
        List<String> arns = client.listClusters(ListClustersRequest.builder().build()).clusterArns();
        Assert.assertNotNull(arns);
        Assert.assertTrue(arns.contains(clusterArn));

        RegisterTaskDefinitionResponse result =
                client.registerTaskDefinition(RegisterTaskDefinitionRequest.builder()
                                .family("test")
                                .containerDefinitions(ContainerDefinition.builder()
                                                .command("command", "command", "command")
                                                .cpu(1)
                                                .entryPoint("entryPoint", "entryPoint")
                                                .image("image")
                                                .memory(1)
                                                .name("test")
                                                .portMappings(PortMapping.builder()
                                                                .hostPort(12345)
                                                                .containerPort(6789).build()
                                                ).build()
                                ).build()
                );

        Assert.assertEquals("test", result.taskDefinition().family());
        Assert.assertNotNull(result.taskDefinition().revision());
        Assert.assertNotNull(result.taskDefinition().taskDefinitionArn());

        ContainerDefinition def = result.taskDefinition()
                                        .containerDefinitions()
                                        .get(0);

        Assert.assertEquals("image", def.image());
        Assert.assertEquals(
                Arrays.asList("entryPoint", "entryPoint"),
                def.entryPoint());

        Assert.assertEquals(
                Arrays.asList("command", "command", "command"),
                def.command());

        // Can't deregister task definitions yet... :(

        List<String> taskArns = client.listTaskDefinitions(ListTaskDefinitionsRequest.builder().build())
                                      .taskDefinitionArns();

        Assert.assertNotNull(taskArns);
        Assert.assertFalse(taskArns.isEmpty());
    }
}
