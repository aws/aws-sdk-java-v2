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

package software.amazon.awssdk.services.cloudhsm;

import org.junit.Test;
import software.amazon.awssdk.services.cloudhsm.model.ListAvailableZonesRequest;
import software.amazon.awssdk.services.cloudhsm.model.ListHapgsRequest;
import software.amazon.awssdk.services.cloudhsm.model.ListHsmsRequest;
import software.amazon.awssdk.services.cloudhsm.model.ListLunaClientsRequest;

public class ServiceIntegrationTest extends IntegrationTestBase {

    /**
     * Simple smoke test to make sure we fix the empty JSON payload issue.
     */
    @Test
    public void testOperations() {
        client.listHsms(ListHsmsRequest.builder().build()).hsmList();
        client.listAvailableZones(ListAvailableZonesRequest.builder().build());
        client.listHapgs(ListHapgsRequest.builder().build());
        client.listLunaClients(ListLunaClientsRequest.builder().build());
    }
}
