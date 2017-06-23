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

package software.amazon.awssdk.services.servicecatalog;

import java.util.List;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.servicecatalog.model.ListRecordHistoryRequest;
import software.amazon.awssdk.services.servicecatalog.model.RecordDetail;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class ServiceCatalogIntegrationTest extends AwsIntegrationTestBase {

    private static ServiceCatalogClient serviceCatalog;

    @Before
    public void setUp() throws Exception {
        serviceCatalog = ServiceCatalogClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @Test
    public void testList() {
        List<RecordDetail> recordDetails = serviceCatalog.listRecordHistory(ListRecordHistoryRequest.builder().build())
                                                         .recordDetails();
        Assert.assertNotNull(recordDetails);
        Assert.assertTrue(recordDetails.isEmpty());
    }

}
