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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;

class V4RequestSigningResultTest {
    @Test
    void getSigningKey_whenCallerMutatesReturnedArray_doesNotChangeStoredKey() {
        byte[] signingKey = {1, 2, 3};
        V4RequestSigningResult result =
            new V4RequestSigningResult("hash", signingKey, "signature", null, SdkHttpRequest.builder());

        result.getSigningKey()[0] = 9;

        assertThat(result.getSigningKey()).containsExactly(1, 2, 3);
        assertThat(signingKey).containsExactly(1, 2, 3);
    }
}
