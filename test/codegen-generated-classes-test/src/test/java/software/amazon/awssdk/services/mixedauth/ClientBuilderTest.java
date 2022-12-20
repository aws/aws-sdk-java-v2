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

package software.amazon.awssdk.services.mixedauth;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;


public class ClientBuilderTest {
    @Test
    public void syncClient_buildWithDefaults_validationsSucceed() {
        MixedauthClientBuilder builder = MixedauthClient.builder();
        builder.region(Region.US_WEST_2).credentialsProvider(AnonymousCredentialsProvider.create());
        assertThatNoException().isThrownBy(builder::build);
    }

    @Test
    public void asyncClient_buildWithDefaults_validationsSucceed() {
        MixedauthAsyncClientBuilder builder = MixedauthAsyncClient.builder();
        builder.region(Region.US_WEST_2).credentialsProvider(AnonymousCredentialsProvider.create());
        assertThatNoException().isThrownBy(builder::build);
    }
}
