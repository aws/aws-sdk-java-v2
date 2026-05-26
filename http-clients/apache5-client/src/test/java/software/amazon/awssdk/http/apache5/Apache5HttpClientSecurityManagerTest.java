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

package software.amazon.awssdk.http.apache5;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that Apache5HttpClient fails fast at construction time when a SecurityManager
 * denies jdk.net.NetworkPermission for TCP keepalive extended options.
 */
@EnabledForJreRange(max = JRE.JAVA_17)
class Apache5HttpClientSecurityManagerTest {

    @AfterEach
    void tearDown() {
        System.setSecurityManager(null);
        System.clearProperty("java.security.policy");
        java.security.Policy.getPolicy().refresh();
    }

    @Test
    void buildWithDefaults_whenStandardPermissionsGrantedButNetworkPermissionMissing_shouldThrowIllegalStateException() {
        System.setProperty("java.security.policy", "=" + getPolicyUrl());
        java.security.Policy.getPolicy().refresh();
        System.setSecurityManager(new SecurityManager());

        assertThatThrownBy(() -> Apache5HttpClient.builder().build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jdk.net.NetworkPermission");
    }

    private String getPolicyUrl() {
        return getClass().getResource("security-manager-test.policy").toExternalForm();
    }

    @ParameterizedTest
    @MethodSource("partiallyGrantedPermissions")
    void buildWithDefaults_whenNotAllPermissionsGranted_shouldThrowIllegalStateException(Set<String> grantedPermissions) {
        System.setSecurityManager(new GrantOnlyNetworkPermissionSecurityManager(grantedPermissions));

        assertThatThrownBy(() -> Apache5HttpClient.builder().build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jdk.net.NetworkPermission");
    }

    @Test
    void buildWithDefaults_whenAllPermissionsGranted_shouldSucceed() {
        Set<String> allGranted = new HashSet<>(Arrays.asList(
            "setOption.TCP_KEEPIDLE", "setOption.TCP_KEEPINTERVAL", "setOption.TCP_KEEPCOUNT"));
        System.setSecurityManager(new GrantOnlyNetworkPermissionSecurityManager(allGranted));
        assertThatNoException().isThrownBy(() -> {
            Apache5HttpClient.builder().build().close();
        });
    }

    @Test
    void buildWithDefaults_whenNoSecurityManager_shouldSucceed() {
        assertThatNoException().isThrownBy(() -> {
            Apache5HttpClient.builder().build().close();
        });
    }

    static Stream<Arguments> partiallyGrantedPermissions() {
        return Stream.of(
            // 0 out of 3 granted
            Arguments.of(new HashSet<>()),
            // 1 out of 3 granted
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPIDLE"))),
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPINTERVAL"))),
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPCOUNT"))),
            // 2 out of 3 granted
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPIDLE", "setOption.TCP_KEEPINTERVAL"))),
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPIDLE", "setOption.TCP_KEEPCOUNT"))),
            Arguments.of(new HashSet<>(Arrays.asList("setOption.TCP_KEEPINTERVAL", "setOption.TCP_KEEPCOUNT")))
        );
    }

    /**
     * SecurityManager that only grants specific jdk.net.NetworkPermission entries and denies the rest.
     */
    private static class GrantOnlyNetworkPermissionSecurityManager extends SecurityManager {
        private final Set<String> grantedPermissions;

        GrantOnlyNetworkPermissionSecurityManager(Set<String> grantedPermissions) {
            this.grantedPermissions = grantedPermissions;
        }

        @Override
        public void checkPermission(Permission perm) {
            if ("jdk.net.NetworkPermission".equals(perm.getClass().getName())
                && !grantedPermissions.contains(perm.getName())) {
                throw new SecurityException("Denied: " + perm.getName());
            }
        }
    }
}
