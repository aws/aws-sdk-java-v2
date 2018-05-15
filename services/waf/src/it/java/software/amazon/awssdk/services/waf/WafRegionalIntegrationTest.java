/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.waf;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.waf.model.ListResourcesForWebACLRequest;
import software.amazon.awssdk.services.waf.model.WAFNonexistentItemException;
import software.amazon.awssdk.services.wafregional.WAFRegionalClient;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class WafRegionalIntegrationTest extends AwsIntegrationTestBase {

    /**
     * Calls an operation specific to WAF Regional. If we get a modeled exception back then we called the
     * right service.
     */
    @Test(expected = WAFNonexistentItemException.class)
    public void smokeTest() {
        final WAFRegionalClient client = WAFRegionalClient.builder()
                                                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                                 .region(Region.US_WEST_2)
                                                                 .build();

        client.listResourcesForWebACL(ListResourcesForWebACLRequest.builder().webACLId("foo").build());
    }
}
