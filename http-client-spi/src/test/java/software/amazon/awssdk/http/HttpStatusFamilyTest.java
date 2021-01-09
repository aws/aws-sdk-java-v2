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

package software.amazon.awssdk.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HttpStatusFamilyTest {
    @Test
    public void statusFamiliesAreMappedCorrectly() {
        assertThat(HttpStatusFamily.of(-1)).isEqualTo(HttpStatusFamily.OTHER);
        assertThat(HttpStatusFamily.of(HttpStatusCode.CONTINUE)).isEqualTo(HttpStatusFamily.INFORMATIONAL);
        assertThat(HttpStatusFamily.of(HttpStatusCode.OK)).isEqualTo(HttpStatusFamily.SUCCESSFUL);
        assertThat(HttpStatusFamily.of(HttpStatusCode.MOVED_PERMANENTLY)).isEqualTo(HttpStatusFamily.REDIRECTION);
        assertThat(HttpStatusFamily.of(HttpStatusCode.NOT_FOUND)).isEqualTo(HttpStatusFamily.CLIENT_ERROR);
        assertThat(HttpStatusFamily.of(HttpStatusCode.INTERNAL_SERVER_ERROR)).isEqualTo(HttpStatusFamily.SERVER_ERROR);
    }

    @Test
    public void matchingIsCorrect() {
        assertThat(HttpStatusFamily.SUCCESSFUL.isOneOf()).isFalse();
        assertThat(HttpStatusFamily.SUCCESSFUL.isOneOf(null)).isFalse();
        assertThat(HttpStatusFamily.SUCCESSFUL.isOneOf(HttpStatusFamily.CLIENT_ERROR)).isFalse();
        assertThat(HttpStatusFamily.SUCCESSFUL.isOneOf(HttpStatusFamily.CLIENT_ERROR, HttpStatusFamily.SUCCESSFUL)).isTrue();
        assertThat(HttpStatusFamily.SUCCESSFUL.isOneOf(HttpStatusFamily.SUCCESSFUL, HttpStatusFamily.CLIENT_ERROR)).isTrue();
        assertThat(HttpStatusFamily.OTHER.isOneOf(HttpStatusFamily.values())).isTrue();
    }
}
