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

package software.amazon.awssdk.services.sqs;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;

/**
 * Integration tests for the SQS Java client.
 */
public class SqsIntegrationTest extends IntegrationTestBase {

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void clockSkewFailure_CorrectsGlobalTimeOffset() throws Exception {
        final int originalOffset = SdkGlobalTime.getGlobalTimeOffset();
        final int skew = 3600;

        SdkGlobalTime.setGlobalTimeOffset(skew);
        assertEquals(skew, SdkGlobalTime.getGlobalTimeOffset());
        SQSAsyncClient sqsClient = createSqsAyncClient();
        sqsClient.listQueues(ListQueuesRequest.builder().build()).join();
        assertThat("Clockskew is fixed!", SdkGlobalTime.getGlobalTimeOffset(), lessThan(skew));
        // subsequent changes to the global time offset won't affect existing client
        SdkGlobalTime.setGlobalTimeOffset(skew);
        sqsClient.listQueues(ListQueuesRequest.builder().build());
        assertEquals(skew, SdkGlobalTime.getGlobalTimeOffset());
        sqsClient.close();

        SdkGlobalTime.setGlobalTimeOffset(originalOffset);
    }
}
