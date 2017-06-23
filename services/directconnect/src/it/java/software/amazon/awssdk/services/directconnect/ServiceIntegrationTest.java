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

package software.amazon.awssdk.services.directconnect;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.directconnect.model.CreateConnectionRequest;
import software.amazon.awssdk.services.directconnect.model.CreateConnectionResponse;
import software.amazon.awssdk.services.directconnect.model.DeleteConnectionRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeConnectionsRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeConnectionsResponse;
import software.amazon.awssdk.services.directconnect.model.DescribeLocationsRequest;
import software.amazon.awssdk.services.directconnect.model.DescribeLocationsResponse;
import software.amazon.awssdk.services.directconnect.model.Location;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String CONNECTION_NAME = "test-connection-name";
    private static final String EXPECTED_CONNECTION_STATUS = "requested";

    private static String connectionId;

    @BeforeClass
    public static void setup() {
        CreateConnectionResponse result = dc.createConnection(CreateConnectionRequest.builder()
                .connectionName(CONNECTION_NAME)
                .bandwidth("1Gbps")
                .location("EqSV5")
                .build());
        connectionId = result.connectionId();
    }

    @AfterClass
    public static void tearDown() {
        dc.deleteConnection(DeleteConnectionRequest.builder().connectionId(connectionId).build());
    }

    @Test
    public void describeLocations_ReturnsNonEmptyList() {
        DescribeLocationsResponse describeLocations = dc.describeLocations(DescribeLocationsRequest.builder().build());
        assertTrue(describeLocations.locations().size() > 0);
        for (Location location : describeLocations.locations()) {
            assertNotNull(location.locationCode());
            assertNotNull(location.locationName());
        }
    }

    @Test
    public void describeConnections_ReturnsNonEmptyList() {
        DescribeConnectionsResponse describeConnectionsResult = dc.describeConnections(DescribeConnectionsRequest.builder().build());
        assertTrue(describeConnectionsResult.connections().size() > 0);
        assertNotNull(describeConnectionsResult.connections().get(0).connectionId());
        assertNotNull(describeConnectionsResult.connections().get(0).connectionName());
        assertNotNull(describeConnectionsResult.connections().get(0).connectionState());
        assertNotNull(describeConnectionsResult.connections().get(0).location());
        assertNotNull(describeConnectionsResult.connections().get(0).region());
    }

    @Test
    public void describeConnections_FilteredByCollectionId_ReturnsOnlyOneConnection() {
        DescribeConnectionsResponse describeConnectionsResult = dc.describeConnections(DescribeConnectionsRequest.builder()
                .connectionId(connectionId)
                .build());
        assertThat(describeConnectionsResult.connections(), hasSize(1));
        assertEquals(connectionId, describeConnectionsResult.connections().get(0).connectionId());
        assertEquals(EXPECTED_CONNECTION_STATUS, describeConnectionsResult.connections().get(0).connectionState());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        DirectConnectClient clockSkewClient = DirectConnectClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(credentials))
                .build();

        clockSkewClient.describeConnections(DescribeConnectionsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }

}
