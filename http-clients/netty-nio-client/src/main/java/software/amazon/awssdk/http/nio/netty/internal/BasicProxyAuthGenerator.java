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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.util.CharsetUtil;
import java.net.URI;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;
import software.amazon.awssdk.utils.Validate;

/**
 * Auth param generator for Basic proxy authentication.
 * <p>
 * See <a href="https://datatracker.ietf.org/doc/html/rfc7617">https://datatracker.ietf.org/doc/html/rfc7617</a>.
 */
@SdkInternalApi
public class BasicProxyAuthGenerator implements ProxyAuthGenerator {
    private final String username;
    private final String password;

    public BasicProxyAuthGenerator(String username, String password) {
        this.username = Validate.notEmpty(username, "username must not be empty");
        this.password = Validate.notEmpty(password, "password must not be empty");
    }

    @Override
    public ProxyAuthScheme scheme() {
        return ProxyAuthScheme.BASIC;
    }

    @Override
    public String generateAuthParams(URI proxyEndpoint) {
        String authToken = String.format("%s:%s", this.username, this.password);
        return Base64.getEncoder().encodeToString(authToken.getBytes(CharsetUtil.UTF_8));
    }
}
