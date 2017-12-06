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

package software.amazon.awssdk.core.sync;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class RequestBodyTest {

    @Test
    public void stringConstructorUsesUTF8ByteLength() {
        // U+03A9 U+03C9
        final String multibyteChars = "Ωω";
        RequestBody rb = RequestBody.of(multibyteChars);
        assertThat(rb.getContentLength()).isEqualTo(4L);
    }
}
