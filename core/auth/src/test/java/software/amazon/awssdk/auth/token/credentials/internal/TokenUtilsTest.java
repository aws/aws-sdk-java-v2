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

package software.amazon.awssdk.auth.token.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.TestBearerToken;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.identity.spi.TokenIdentity;

public class TokenUtilsTest {

    @Test
    public void toSdkToken_null_returnsNull() {
        assertThat(software.amazon.awssdk.auth.credentials.TokenUtils.toSdkToken(null)).isNull();
    }

    @Test
    public void toSdkToken_SdkToken_returnsAsIs() {
        TokenIdentity input = TestBearerToken.create("t");
        SdkToken output = software.amazon.awssdk.auth.credentials.TokenUtils.toSdkToken(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    public void toSdkToken_TokenIdentity_returnsSdkToken() {
        TokenIdentity tokenIdentity = TokenIdentity.create("token");
        SdkToken sdkToken = software.amazon.awssdk.auth.credentials.TokenUtils.toSdkToken(tokenIdentity);
        assertThat(sdkToken.token()).isEqualTo("token");
    }
}