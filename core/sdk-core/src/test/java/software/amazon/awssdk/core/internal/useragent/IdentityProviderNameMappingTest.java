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

package software.amazon.awssdk.core.internal.useragent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class IdentityProviderNameMappingTest {

    @Test
    void when_providerIsKnown_shortValueIsReturned() {
        Optional<String> mappedProviderName = IdentityProviderNameMapping.fromValue("StaticCredentialsProvider");
        assertThat(mappedProviderName).isPresent();
        assertThat(mappedProviderName.get()).isEqualTo("STAT");
    }

    @Test
    void when_providerIsUnknown_stringIsReturned() {
        Optional<String> mappedProviderName = IdentityProviderNameMapping.fromValue("MyHomebrewedCredentialsProvider");
        assertThat(mappedProviderName).isPresent();
        assertThat(mappedProviderName.get()).isEqualTo("MyHomebrewedCredentialsProvider");
    }

    @Test
    void when_providerIsIllegal_noValueIsReturned() {
        Optional<String> mappedProviderName = IdentityProviderNameMapping.fromValue("My@#$%$CredentialsProvider");
        assertThat(mappedProviderName).isNotPresent();
    }

    @Test
    void when_providerIsTooLong_noValueIsReturned() {
        Optional<String> mappedProviderName = IdentityProviderNameMapping.fromValue(
            "MyMegaGinormousBubbaBubbaBubbaBubbaBubbaBubbaCredentialsProvider");
        assertThat(mappedProviderName).isNotPresent();
    }
}
