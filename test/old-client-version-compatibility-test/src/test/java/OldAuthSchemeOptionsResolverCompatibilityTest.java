/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Verifies that an old service module (Polly 2.48.0, compiled with the 1-arg AuthSchemeOptionsResolver)
 * works correctly with the new core (which calls the 2-arg resolve method).
 * <p>
 * The default method on AuthSchemeOptionsResolver delegates the 2-arg call to the 1-arg implementation,
 * preventing AbstractMethodError in mixed-version scenarios.
 */
public class OldAuthSchemeOptionsResolverCompatibilityTest {

    private MockSyncHttpClient httpClient;
    private PollyClient pollyClient;

    @BeforeEach
    public void setup() {
        this.httpClient = new MockSyncHttpClient();
        this.httpClient.stubNextResponse200();

        this.pollyClient = PollyClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("akid", "skid")))
            .httpClient(httpClient)
            .build();
    }

    @AfterEach
    public void teardown() {
        httpClient.close();
        pollyClient.close();
    }

    /**
     * Old Polly client (2.48.0) uses a 1-arg AuthSchemeOptionsResolver lambda.
     * New core calls the 2-arg resolve() method.
     * The default method bridge should prevent AbstractMethodError.
     */
    @Test
    public void oldServiceModule_withNewCore_doesNotThrowAbstractMethodError() {
        assertThatNoException().isThrownBy(() -> pollyClient.listLexicons());
        assertThat(httpClient.getLastRequest()).isNotNull();
    }
}
