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

package software.amazon.awssdk.services.sso.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsResponse;
import software.amazon.awssdk.services.sso.model.RoleCredentials;

/**
 * Validates the functionality of {@link SsoCredentialsProvider}.
 */
public class SsoCredentialsProviderTest {

    private SsoClient ssoClient;

    @Test
    public void cachingDoesNotApplyToExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, false);
        callClient(verify(ssoClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingDoesNotApplyToExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, true);
        callClient(verify(ssoClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, false);
        callClient(verify(ssoClient, times(1)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, true);
        callClient(verify(ssoClient, times(1)), Mockito.any());
    }

    @Test
    public void distantExpiringCredentialsUpdatedInBackground() throws InterruptedException {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, false);

        Instant endCheckTime = Instant.now().plus(Duration.ofSeconds(5));
        while (Mockito.mockingDetails(ssoClient).getInvocations().size() < 2 && endCheckTime.isAfter(Instant.now())) {
            Thread.sleep(100);
        }

        callClient(verify(ssoClient, times(2)), Mockito.any());
    }

    @Test
    public void distantExpiringCredentialsUpdatedInBackground_OverridePrefetchAndStaleTimes() throws InterruptedException {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, true);

        Instant endCheckTime = Instant.now().plus(Duration.ofSeconds(5));
        while (Mockito.mockingDetails(ssoClient).getInvocations().size() < 2 && endCheckTime.isAfter(Instant.now())) {
            Thread.sleep(100);
        }

        callClient(verify(ssoClient, times(2)), Mockito.any());
    }



    private GetRoleCredentialsRequestSupplier getRequestSupplier() {
        return new GetRoleCredentialsRequestSupplier(GetRoleCredentialsRequest.builder().build(), "cachedToken");
    }

    private GetRoleCredentialsResponse getResponse(RoleCredentials roleCredentials) {
        return GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build();
    }

    private GetRoleCredentialsResponse callClient(SsoClient ssoClient, GetRoleCredentialsRequest request) {
        return ssoClient.getRoleCredentials(request);
    }

    private void callClientWithCredentialsProvider(Instant credentialsExpirationDate, int numTimesInvokeCredentialsProvider,
                                                   boolean overrideStaleAndPrefetchTimes) {
        ssoClient = mock(SsoClient.class);
        RoleCredentials credentials = RoleCredentials.builder().accessKeyId("a").secretAccessKey("b").sessionToken("c")
                                                     .expiration(credentialsExpirationDate.toEpochMilli()).build();

        Supplier<GetRoleCredentialsRequest> supplier = getRequestSupplier();
        GetRoleCredentialsResponse response = getResponse(credentials);

        when(ssoClient.getRoleCredentials(supplier.get())).thenReturn(response);

        SsoCredentialsProvider.Builder ssoCredentialsProviderBuilder = SsoCredentialsProvider.builder().refreshRequest(supplier);

        if(overrideStaleAndPrefetchTimes) {
            ssoCredentialsProviderBuilder.staleTime(Duration.ofMinutes(2));
            ssoCredentialsProviderBuilder.prefetchTime(Duration.ofMinutes(4));
        }

        try (SsoCredentialsProvider credentialsProvider = ssoCredentialsProviderBuilder.ssoClient(ssoClient).build()) {
            if(overrideStaleAndPrefetchTimes) {
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(2));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isEqualTo(Duration.ofMinutes(4));
            } else {
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(1));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isEqualTo(Duration.ofMinutes(5));
            }

            for (int i = 0; i < numTimesInvokeCredentialsProvider; ++i) {
                AwsSessionCredentials actualCredentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
                assertThat(actualCredentials.accessKeyId()).isEqualTo("a");
                assertThat(actualCredentials.secretAccessKey()).isEqualTo("b");
                assertThat(actualCredentials.sessionToken()).isEqualTo("c");
            }
        }

    }

    private static final class GetRoleCredentialsRequestSupplier implements Supplier {
        private final GetRoleCredentialsRequest request;
        private final String cachedToken;

        GetRoleCredentialsRequestSupplier(GetRoleCredentialsRequest request,
                                          String cachedToken) {
            this.request = request;
            this.cachedToken = cachedToken;
        }

        @Override
        public Object get() {
            return request.toBuilder().accessToken(cachedToken).build();
        }

    }

}