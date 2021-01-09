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
import java.time.Instant;
import org.junit.Test;

public class SignerKeyTest {

    @Test
    public void isValidForDate_dayBefore_false() {
        Instant signerDate = Instant.parse("2020-03-03T23:59:59Z");
        SignerKey key = new SignerKey(signerDate, new byte[0]);
        Instant dayBefore = Instant.parse("2020-03-02T23:59:59Z");

        assertThat(key.isValidForDate(dayBefore)).isFalse();
    }

    @Test
    public void isValidForDate_sameDay_true() {
        Instant signerDate = Instant.parse("2020-03-03T23:59:59Z");
        SignerKey key = new SignerKey(signerDate, new byte[0]);
        Instant sameDay = Instant.parse("2020-03-03T01:02:03Z");

        assertThat(key.isValidForDate(sameDay)).isTrue();
    }

    @Test
    public void isValidForDate_dayAfter_false() {
        Instant signerDate = Instant.parse("2020-03-03T23:59:59Z");
        SignerKey key = new SignerKey(signerDate, new byte[0]);
        Instant dayAfter = Instant.parse("2020-03-04T00:00:00Z");

        assertThat(key.isValidForDate(dayAfter)).isFalse();
    }
}
