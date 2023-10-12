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

package software.amazon.awssdk.services.testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.concurrent.CompletableFuture;
import org.mockito.MockSettings;
import org.mockito.internal.creation.MockSettingsImpl;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

public class MockIdentityProviderUtil {

    public static IdentityProvider<AwsCredentialsIdentity> mockIdentityProvider() {
        IdentityProvider<AwsCredentialsIdentity> mockIdentityProvider = mock(IdentityProvider.class, withSettings().lenient());
        setup(mockIdentityProvider);
        return mockIdentityProvider;
    }

    public static void setup(IdentityProvider<AwsCredentialsIdentity> mockIdentityProvider) {
        when(mockIdentityProvider.resolveIdentity(any(ResolveIdentityRequest.class))).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
            return CompletableFuture.completedFuture(AwsBasicCredentials.create("foo", "bar"));
        });
        when(mockIdentityProvider.resolveIdentity()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
            return CompletableFuture.completedFuture(AwsBasicCredentials.create("foo", "bar"));
        });
        when(mockIdentityProvider.identityType()).thenReturn(AwsCredentialsIdentity.class);
    }
}
