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

package software.amazon.awssdk.codegen.model.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class AuthTypeTest {
    @Test
    public void authTypeConvertBearer() {
        String smithyAuthTypeInput = "smithy.api#httpBearerAuth";
        String c2jAuthTypeInput = "bearer";

        AuthType smithyAuthType = AuthType.fromValue(smithyAuthTypeInput);
        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(smithyAuthType, equalTo(AuthType.BEARER));
        assertThat(c2jAuthType, equalTo(AuthType.BEARER));
        assertThat(smithyAuthType, equalTo(c2jAuthType));
    }

    @Test
    public void authTypeConvertNone() {
        String smithyAuthTypeInput = "smithy.api#noAuth";
        String c2jAuthTypeInput = "none";

        AuthType smithyAuthType = AuthType.fromValue(smithyAuthTypeInput);
        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(smithyAuthType, equalTo(AuthType.NONE));
        assertThat(c2jAuthType, equalTo(AuthType.NONE));
        assertThat(smithyAuthType, equalTo(c2jAuthType));
    }

    @Test
    public void authTypeConvertV4() {
        String smithyAuthTypeInput = "aws.auth#sigv4";
        String c2jAuthTypeInput = "v4";

        AuthType smithyAuthType = AuthType.fromValue(smithyAuthTypeInput);
        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(smithyAuthType, equalTo(AuthType.V4));
        assertThat(c2jAuthType, equalTo(AuthType.V4));
        assertThat(smithyAuthType, equalTo(c2jAuthType));
    }

    @Test
    public void authTypeConvertCustom() {
        String c2jAuthTypeInput = "custom";

        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(c2jAuthType, equalTo(AuthType.CUSTOM));
    }

    @Test
    public void authTypeConvertIam() {
        String c2jAuthTypeInput = "iam";

        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(c2jAuthType, equalTo(AuthType.IAM));
    }

    @Test
    public void authTypeConvertV4UnsignedBody() {
        String c2jAuthTypeInput = "v4-unsigned-body";

        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(c2jAuthType, equalTo(AuthType.V4_UNSIGNED_BODY));
    }

    @Test
    public void authTypeConvertS3() {
        String c2jAuthTypeInput = "s3";

        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(c2jAuthType, equalTo(AuthType.S3));
    }

    @Test
    public void authTypeConvertS3v4() {
        String c2jAuthTypeInput = "s3v4";

        AuthType c2jAuthType = AuthType.fromValue(c2jAuthTypeInput);
        assertThat(c2jAuthType, equalTo(AuthType.S3V4));
    }

    @Test
    public void authTypeConvertUnknownAuthType() {
        String c2jAuthTypeInput = "unknown";

        assertThatThrownBy(() -> AuthType.fromValue(c2jAuthTypeInput))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown AuthType 'unknown'");
    }
}
