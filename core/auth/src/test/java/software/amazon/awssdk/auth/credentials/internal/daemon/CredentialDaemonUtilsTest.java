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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialDaemonUtilsTest {
    @ParameterizedTest
    @CsvSource({
        "2024-05-06T07:08:09Z, 2024-05-06T07:08:09Z",
        "2024-05-06T07:08:09.1Z, 2024-05-06T07:08:09.100Z",
        "2024-05-06T07:08:09.12Z, 2024-05-06T07:08:09.120Z",
        "2024-05-06T07:08:09.123456Z, 2024-05-06T07:08:09.123456Z",
        "2024-05-06T07:08:09.123456789Z, 2024-05-06T07:08:09.123456789Z",
        "2024-02-29T23:59:59.000000001Z, 2024-02-29T23:59:59.000000001Z"
    })
    void parseRfc3339_withZeroToNineFractionDigits_returnsInstant(String value, String expected) {
        assertThat(CredentialDaemonUtils.parseRfc3339(value)).isEqualTo(Instant.parse(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2024-05-06T07:08:09.1234567890Z",
        "2024-05-06T07:08:09z",
        "2024-05-06t07:08:09Z",
        "2024-05-06T07:08:09+00:00",
        "2024-05-06T07:08:09",
        "2024-05-06T07:08:09.Z",
        "2023-02-29T00:00:00Z",
        "2024-13-01T00:00:00Z",
        "2024-05-06T24:00:00Z",
        "2024-05-06T07:08:60Z",
        "2024-05-06 07:08:09Z",
        "2024-05-06T07:08:09Z\n",
        ""
    })
    void parseRfc3339_withInvalidTimestamp_raisesIllegalArgument(String value) {
        assertThatThrownBy(() -> CredentialDaemonUtils.parseRfc3339(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
