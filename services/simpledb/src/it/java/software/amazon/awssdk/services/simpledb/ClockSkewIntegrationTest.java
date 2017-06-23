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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;

public class ClockSkewIntegrationTest extends IntegrationTestBase {

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkewSdb() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        SimpleDBClient clockSkewClient = SimpleDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        clockSkewClient.listDomains(ListDomainsRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkewAsync() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        SimpleDBAsyncClient clockSkewClient = SimpleDBAsyncClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();

        clockSkewClient.listDomains(ListDomainsRequest.builder().build()).join();
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}
