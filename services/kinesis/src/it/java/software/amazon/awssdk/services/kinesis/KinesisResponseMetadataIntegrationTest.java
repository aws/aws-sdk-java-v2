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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.kinesis.model.KinesisResponse;

public class KinesisResponseMetadataIntegrationTest extends AbstractTestCase {

    @Test
    public void sync_shouldContainResponseMetadata() {
        DescribeLimitsResponse response = client.describeLimits();
        verifyResponseMetadata(response);
    }

    @Test
    public void async_shouldContainResponseMetadata() {
        DescribeLimitsResponse response = asyncClient.describeLimits().join();
        verifyResponseMetadata(response);
    }

    private void verifyResponseMetadata(KinesisResponse response) {
        assertThat(response.responseMetadata().requestId()).isNotEqualTo("UNKNOWN");
        assertThat(response.responseMetadata().extendedRequestId()).isNotEqualTo("UNKNOWN");
    }
}
