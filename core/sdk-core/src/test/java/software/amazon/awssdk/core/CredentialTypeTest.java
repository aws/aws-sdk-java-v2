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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class CredentialTypeTest {

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(CredentialType.class)
                      .withNonnullFields("value")
                      .verify();
    }

    @Test
    public void credentialType_bearerToken(){

        CredentialType token = CredentialType.TOKEN;
        CredentialType tokenFromString = CredentialType.of("TOKEN");
        assertThat(token).isSameAs(tokenFromString);
        assertThat(token).isEqualTo(tokenFromString);
    }

    @Test
    public void credentialType_usesSameInstance_when_sameCredentialTypeOfSameValue(){

        CredentialType credentialTypeOne = CredentialType.of("Credential Type 1");
        CredentialType credentialTypeOneDuplicate = CredentialType.of("Credential Type 1");
        assertThat(credentialTypeOneDuplicate).isSameAs(credentialTypeOne);
        assertThat(credentialTypeOne).isEqualTo(credentialTypeOneDuplicate);
    }
}
