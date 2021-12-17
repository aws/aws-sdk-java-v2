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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ResponseBytesTest {
    private static final Object OBJECT = new Object();
    @Test
    public void fromByteArrayCreatesCopy() {
        byte[] input = new byte[] { 'a' };
        byte[] output = ResponseBytes.fromByteArray(OBJECT, input).asByteArrayUnsafe();

        input[0] = 'b';
        assertThat(output).isNotEqualTo(input);
    }

    @Test
    public void asByteArrayCreatesCopy() {
        byte[] input = new byte[] { 'a' };
        byte[] output = ResponseBytes.fromByteArrayUnsafe(OBJECT, input).asByteArray();

        input[0] = 'b';
        assertThat(output).isNotEqualTo(input);
    }

    @Test
    public void fromByteArrayUnsafeAndAsByteArrayUnsafeDoNotCopy() {
        byte[] input = new byte[] { 'a' };
        byte[] output = ResponseBytes.fromByteArrayUnsafe(OBJECT, input).asByteArrayUnsafe();

        assertThat(output).isSameAs(input);
    }
}
