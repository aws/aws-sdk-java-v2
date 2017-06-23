/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.emr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.emr.model.ActionOnFailure;
import software.amazon.awssdk.services.emr.model.AddTagsRequest;
import software.amazon.awssdk.services.emr.model.Cluster;
import software.amazon.awssdk.services.emr.model.ClusterSummary;
import software.amazon.awssdk.services.emr.model.DescribeClusterRequest;
import software.amazon.awssdk.services.emr.model.JobFlowInstancesConfig;
import software.amazon.awssdk.services.emr.model.ListClustersRequest;
import software.amazon.awssdk.services.emr.model.RemoveTagsRequest;
import software.amazon.awssdk.services.emr.model.RunJobFlowRequest;
import software.amazon.awssdk.services.emr.model.StepConfig;
import software.amazon.awssdk.services.emr.model.Tag;
import software.amazon.awssdk.services.emr.model.TerminateJobFlowsRequest;
import software.amazon.awssdk.services.emr.util.StepFactory;


/** Integration test for basic service operations. */
public class EMRIntegrationTest extends IntegrationTestBase {

    private String jobFlowId;

    /**
     * Cleans up any created tests resources.
     */
    @After
    public void tearDown() throws Exception {
        try {
            if (jobFlowId != null) {
                emr.terminateJobFlows(TerminateJobFlowsRequest.builder().jobFlowIds(jobFlowId).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // See https://forums.aws.amazon.com/thread.jspa?threadID=158756
    @Test
    public void testListCluster() {
        emr.listClusters(ListClustersRequest.builder()
                                            .createdAfter(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                                            .build());
    }

    @Test
    public void testServiceOperation() throws Exception {
        jobFlowId = runTestJobFlow();

        emr.addTags(AddTagsRequest.builder().resourceId(jobFlowId).tags(Tag.builder().key("foo").value("bar").build()).build());
        assertTrue(doesTagExist(jobFlowId, "foo", "bar"));

        emr.removeTags(RemoveTagsRequest.builder().resourceId(jobFlowId).tagKeys("foo").build());
        assertFalse(doesTagExist(jobFlowId, "foo", "bar"));

        for (ClusterSummary cluster : emr.listClusters(ListClustersRequest.builder().build()).clusters()) {
            assertNotNull(cluster.id());
            assertNotNull(cluster.name());
            assertNotNull(cluster.status());
        }
    }

    private boolean doesTagExist(String jobFlowId, String tagKey, String tagValue) {
        Cluster cluster = emr.describeCluster(DescribeClusterRequest.builder().clusterId(jobFlowId).build()).cluster();

        for (Tag tag : cluster.tags()) {
            if (tag.key().equals(tagKey) && tag.value().equals(tagValue)) {
                return true;
            }
        }

        return false;
    }

    private String runTestJobFlow() {
        StepFactory stepFactory = new StepFactory();

        StepConfig enabledebugging = StepConfig.builder()
                                               .name("Enable debugging")
                                               .actionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
                                               .hadoopJarStep(stepFactory.newEnableDebuggingStep()).build();

        StepConfig installHive = StepConfig.builder()
                                           .name("Install Hive")
                                           .actionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
                                           .hadoopJarStep(stepFactory.newInstallHiveStep()).build();

        RunJobFlowRequest request = RunJobFlowRequest.builder()
                                                     .name("Hive Interactive")
                                                     .amiVersion("3.8.0")
                                                     .steps(enabledebugging, installHive)
                                                     .serviceRole("EMR_DefaultRole")
                                                     .jobFlowRole("EMR_EC2_DefaultRole")
                                                     .instances(JobFlowInstancesConfig.builder()
                                                                                      .hadoopVersion("2.4.0")
                                                                                      .instanceCount(2)
                                                                                      .keepJobFlowAliveWhenNoSteps(false)
                                                                                      .masterInstanceType("m1.medium")
                                                                                      .slaveInstanceType("m1.medium").build())
                                                     .build();

        return emr.runJobFlow(request).jobFlowId();
    }
}
