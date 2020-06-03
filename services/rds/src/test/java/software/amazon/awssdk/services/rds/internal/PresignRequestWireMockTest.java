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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;

@RunWith(MockitoJUnitRunner.class)
public class PresignRequestWireMockTest {
    @ClassRule
    public static final WireMockRule WIRE_MOCK = new WireMockRule(0);

    public static RdsClient client;

    @BeforeClass
    public static void setup() {
        client = RdsClient.builder()
                          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                          .region(Region.US_EAST_1)
                          .endpointOverride(URI.create("http://localhost:" + WIRE_MOCK.port()))
                          .build();
    }

    @Before
    public void reset() {
        WIRE_MOCK.resetAll();
    }

    @Test
    public void copyDbClusterSnapshotWithSourceRegionSendsPresignedUrl() {
        verifyMethodCallSendsPresignedUrl(() -> client.copyDBClusterSnapshot(r -> r.sourceRegion("us-west-2")),
                                          "CopyDBClusterSnapshot");
    }

    @Test
    public void copyDBSnapshotWithSourceRegionSendsPresignedUrl() {
        verifyMethodCallSendsPresignedUrl(() -> client.copyDBSnapshot(r -> r.sourceRegion("us-west-2")),
                                          "CopyDBSnapshot");
    }

    @Test
    public void createDbClusterWithSourceRegionSendsPresignedUrl() {
        verifyMethodCallSendsPresignedUrl(() -> client.createDBCluster(r -> r.sourceRegion("us-west-2")),
                                          "CreateDBCluster");
    }

    @Test
    public void createDBInstanceReadReplicaWithSourceRegionSendsPresignedUrl() {
        verifyMethodCallSendsPresignedUrl(() -> client.createDBInstanceReadReplica(r -> r.sourceRegion("us-west-2")),
                                          "CreateDBInstanceReadReplica");
    }

    public void verifyMethodCallSendsPresignedUrl(Runnable methodCall, String actionName) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("<body/>")));

        methodCall.run();

        List<LoggedRequest> requests = findAll(anyRequestedFor(anyUrl()));

        assertThat(requests).isNotEmpty();

        LoggedRequest lastRequest = requests.get(0);
        String lastRequestBody = new String(lastRequest.getBody(), UTF_8);
        assertThat(lastRequestBody).contains("PreSignedUrl=https%3A%2F%2Frds.us-west-2.amazonaws.com%3FAction%3D" + actionName
                                             + "%26Version%3D2014-10-31%26DestinationRegion%3Dus-east-1%26");
    }
}
