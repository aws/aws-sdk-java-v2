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

package software.amazon.awssdk.services.elasticbeanstalk;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.testutils.SdkAsserts.assertNotEmpty;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListAvailableSolutionStacksRequest;

/**
 * Integration test to bring up a new ElasticBeanstalk environment and run through as
 * many operations as possible.
 */
public class ElasticBeanstalkIntegrationTest extends ElasticBeanstalkIntegrationTestBase {

    /** Tests that we can describe the available solution stacks. */
    @Test
    public void testListAvailableSolutionStacks() throws Exception {
        List<String> solutionStacks =
                elasticbeanstalk.listAvailableSolutionStacks(ListAvailableSolutionStacksRequest.builder().build())
                                .solutionStacks();
        assertNotNull(solutionStacks);
        assertTrue(solutionStacks.size() > 1);
        for (String stack : solutionStacks) {
            assertNotEmpty(stack);
        }
    }

}
