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

import java.time.Duration;
import java.time.Instant;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.emr.model.ListClustersRequest;
import software.amazon.awssdk.services.emr.model.TerminateJobFlowsRequest;


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
                                            .createdAfter(Instant.now().minus(Duration.ofDays(1)))
                                            .build());
    }
}
