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

package software.amazon.awssdk.services.rds.internal;

import java.time.Duration;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class RdsPresignInterceptorIntegrationTest extends AwsIntegrationTestBase {
    private static final RdsClient US_WEST_2_CLIENT = RdsClient.builder()
                                                               .region(Region.US_WEST_2)
                                                               .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                               .build();

    @Test
    public void createDbClusterReplicaPresigningWorks() {
        US_WEST_2_CLIENT.createDBCluster(r -> r.dbClusterIdentifier("database-2-replica")
                                               .sourceRegion("us-east-1")
                                               .engine("aurora")
                                               .storageEncrypted(true)
                                               .kmsKeyId("aws/rds")
                                               .replicationSourceIdentifier("arn:aws:rds:us-east-1:295513896279:cluster:database-2"));

        US_WEST_2_CLIENT.deleteDBCluster(r -> r.dbClusterIdentifier("database-2-replica")
                                               .skipFinalSnapshot(true));
    }
}
