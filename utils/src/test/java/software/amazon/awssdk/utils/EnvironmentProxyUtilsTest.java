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

package software.amazon.awssdk.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class EnvironmentProxyUtilsTest {

    @Test
    void testParseHost() {
        assertEquals("1.2.3.4", EnvironmentProxyUtils.parseHost("http://1.2.3.4:8080").get());
        assertEquals("localhost", EnvironmentProxyUtils.parseHost("http://localhost:8080").get());
        assertEquals("subdomain.proxy.com", EnvironmentProxyUtils.parseHost("http://subdomain.proxy.com:8080").get());
        assertEquals("proxy.com", EnvironmentProxyUtils.parseHost("http://proxy.com:8080").get());
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parseHost("invalid-value"));
    }

    @Test
    void testParsePort() {
        assertEquals(8080, EnvironmentProxyUtils.parsePort("http://username:password@host.com:8080").get());
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parsePort("http://username:password@host.com:-2000"));
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parsePort("http://username:password@host.com"));
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parsePort("invalid-value"));
    }

    @Test
    void testParseProtocol() {
        assertEquals("http", EnvironmentProxyUtils.parseProtocol("http://host.com:8080").get());
        assertEquals("https", EnvironmentProxyUtils.parseProtocol("https://host.com:8080").get());
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parseProtocol("host.com:8080"));
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parseProtocol("invalid-value"));
    }

    @Test
    void testParseUsername() {
        assertEquals("username", EnvironmentProxyUtils.parseUsername("http://username:password@host.com:8080").get());
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parseUsername("http://host.com:8080"));
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parseUsername("invalid-value"));
    }

    @Test
    void testParsePassword() {
        assertEquals("password", EnvironmentProxyUtils.parsePassword("http://username:password@host.com:8080").get());
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parsePassword("http://host.com:8080"));
        assertEquals(Optional.empty(), EnvironmentProxyUtils.parsePassword("invalid-value"));
    }

}
