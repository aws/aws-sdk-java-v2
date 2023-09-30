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

package software.amazon.awssdk.http.proxy;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TestProxySetting {
    private Integer port = 0;
    private String host;
    private String userName;
    private String password;
    private Set<String> nonProxyHosts = new HashSet<>();

    @Override
    public String toString() {
        return "TestProxySetting{" +
               "port=" + port +
               ", host='" + host + '\'' +
               ", userName='" + userName + '\'' +
               ", password='" + password + '\'' +
               ", nonProxyHosts=" + nonProxyHosts +
               '}' ;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getNonProxyHosts() {
        return Collections.unmodifiableSet(nonProxyHosts);
    }

    public TestProxySetting port(Integer port) {
        this.port = port;
        return this;
    }

    public TestProxySetting host(String host) {
        this.host = host;
        return this;
    }

    public TestProxySetting userName(String userName) {
        this.userName = userName;
        return this;
    }

    public TestProxySetting password(String password) {
        this.password = password;
        return this;
    }

    public TestProxySetting nonProxyHost(String... nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts != null ? Arrays.stream(nonProxyHosts)
                                                           .collect(Collectors.toSet()) : new HashSet<>();
        return this;
    }
}