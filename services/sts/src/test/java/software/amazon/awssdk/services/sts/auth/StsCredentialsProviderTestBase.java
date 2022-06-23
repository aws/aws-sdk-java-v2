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

package software.amazon.awssdk.services.sts.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validates the functionality of {@link StsCredentialsProvider} and its subclasses.
 */
@ExtendWith(MockitoExtension.class)
public abstract class StsCredentialsProviderTestBase<RequestT, ResponseT> {
    @Mock
    protected StsClient stsClient;

    @Test
    public void cachingDoesNotApplyToExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, false);
        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingDoesNotApplyToExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, true);
        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, false);
        callClient(verify(stsClient, times(1)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, true);
        callClient(verify(stsClient, times(1)), Mockito.any());
    }

    @Test
    public void distantExpiringCredentialsUpdatedInBackground() throws InterruptedException {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, false);

        Instant endCheckTime = Instant.now().plus(Duration.ofSeconds(5));
        while (Mockito.mockingDetails(stsClient).getInvocations().size() < 2 && endCheckTime.isAfter(Instant.now())) {
            Thread.sleep(100);
        }

        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void distantExpiringCredentialsUpdatedInBackground_OverridePrefetchAndStaleTimes() throws InterruptedException {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, true);

        Instant endCheckTime = Instant.now().plus(Duration.ofSeconds(5));
        while (Mockito.mockingDetails(stsClient).getInvocations().size() < 2 && endCheckTime.isAfter(Instant.now())) {
            Thread.sleep(100);
        }

        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    protected abstract RequestT getRequest();

    protected abstract ResponseT getResponse(Credentials credentials);

    protected abstract StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider>
    createCredentialsProviderBuilder(RequestT request);

    protected abstract ResponseT callClient(StsClient client, RequestT request);

    public void callClientWithCredentialsProvider(Instant credentialsExpirationDate, int numTimesInvokeCredentialsProvider, boolean overrideStaleAndPrefetchTimes) {
        Credentials credentials = Credentials.builder().accessKeyId("a").secretAccessKey("b").sessionToken("c").expiration(credentialsExpirationDate).build();
        RequestT request = getRequest();
        ResponseT response = getResponse(credentials);

        when(callClient(stsClient, request)).thenReturn(response);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder = createCredentialsProviderBuilder(request);

        if(overrideStaleAndPrefetchTimes) {
            //do the same values as we would do without overriding the stale and prefetch times
            credentialsProviderBuilder.staleTime(Duration.ofMinutes(2));
            credentialsProviderBuilder.prefetchTime(Duration.ofMinutes(4));
        }

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            if(overrideStaleAndPrefetchTimes) {
                //validate that we actually stored the override values in the build provider
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(2));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isEqualTo(Duration.ofMinutes(4));
            } else {
                //validate that the default values are used
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(1));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isEqualTo(Duration.ofMinutes(5));
            }

            for (int i = 0; i < numTimesInvokeCredentialsProvider; ++i) {
                AwsSessionCredentials providedCredentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
                assertThat(providedCredentials.accessKeyId()).isEqualTo("a");
                assertThat(providedCredentials.secretAccessKey()).isEqualTo("b");
                assertThat(providedCredentials.sessionToken()).isEqualTo("c");
            }
        }
    }
}
