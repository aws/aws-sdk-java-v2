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

package software.amazon.awssdk.auth.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.regions.Region;

/**
 * Tests for {@link Aws4SignerRequestParams}.
 */
public class Aws4SignerRequestParamsTest {

    @Test
    public void appliesOffset_PositiveOffset() {
        offsetTest(5);
    }

    @Test
    public void appliesOffset_NegativeOffset() {
        offsetTest(-5);
    }

    private void offsetTest(int offsetSeconds) {
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(AwsBasicCredentials.create("akid", "skid"))
                .doubleUrlEncode(false)
                .signingName("test-service")
                .signingRegion(Region.US_WEST_2)
                .timeOffset(offsetSeconds)
                .build();

        Instant now = Instant.now();
        Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signerParams);

        Instant requestSigningInstant = Instant.ofEpochMilli(requestParams.getRequestSigningDateTimeMilli());

        // The offset is subtracted from the current time
        if (offsetSeconds > 0) {
            assertThat(requestSigningInstant).isBefore(now);
        } else {
            assertThat(requestSigningInstant).isAfter(now);
        }

        Duration diff = Duration.between(requestSigningInstant, now.minusSeconds(offsetSeconds));
        // Allow some wiggle room in the difference since we can't use a clock
        // override for complete accuracy as it doesn't apply the offset when
        // using a clock override
        assertThat(diff).isLessThanOrEqualTo(Duration.ofMillis(100));
    }
}
