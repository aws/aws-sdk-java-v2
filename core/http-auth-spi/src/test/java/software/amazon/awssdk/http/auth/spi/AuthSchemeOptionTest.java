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

package software.amazon.awssdk.http.auth.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.IdentityProperty;

class AuthSchemeOptionTest {

    @Test
    public void emptyBuilder_isNotSuccessful() {
        assertThrows(NullPointerException.class, () -> AuthSchemeOption.builder().build());
    }

    @Test
    public void build_withSchemeId_isSuccessful() {
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder().schemeId("my.api#myAuth").build();
        assertEquals("my.api#myAuth", authSchemeOption.schemeId());
    }

    @Test
    public void putProperty_sameProperty_isReplaced() {
        IdentityProperty<String> identityProperty = IdentityProperty.create(String.class, "identityKey1");
        SignerProperty<String> signerProperty = SignerProperty.create(String.class, "signingKey1");

        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(identityProperty, "identity-value1")
                                                            .putIdentityProperty(identityProperty, "identity-value2")
                                                            .putSignerProperty(signerProperty, "signing-value1")
                                                            .putSignerProperty(signerProperty, "signing-value2")
                                                            .build();

        assertEquals("identity-value2", authSchemeOption.identityProperty(identityProperty));
        assertEquals("signing-value2", authSchemeOption.signerProperty(signerProperty));
    }

    @Test
    public void copyBuilder_addProperty_retains() {
        IdentityProperty<String> identityProperty = IdentityProperty.create(String.class, "identityKey");
        SignerProperty<String> signerProperty = SignerProperty.create(String.class, "signingKey");
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(identityProperty, "identity-value1")
                                                            .putSignerProperty(signerProperty, "signing-value1")
                                                            .build();

        IdentityProperty<String> identityProperty2 = IdentityProperty.create(String.class, "identityKey2");
        SignerProperty<String> signerProperty2 = SignerProperty.create(String.class, "signingKey2");

        authSchemeOption =
            authSchemeOption.copy(builder -> builder.putIdentityProperty(identityProperty2, "identity2-value1")
                                                    .putSignerProperty(signerProperty2, "signing2-value1"));

        assertEquals("identity-value1", authSchemeOption.identityProperty(identityProperty));
        assertEquals("identity2-value1", authSchemeOption.identityProperty(identityProperty2));
        assertEquals("signing-value1", authSchemeOption.signerProperty(signerProperty));
        assertEquals("signing2-value1", authSchemeOption.signerProperty(signerProperty2));
    }

    @Test
    public void copyBuilder_updateProperty_updates() {
        IdentityProperty<String> identityProperty = IdentityProperty.create(String.class, "identityKey");
        SignerProperty<String> signerProperty = SignerProperty.create(String.class, "signingKey");
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(identityProperty, "identity-value1")
                                                            .putSignerProperty(signerProperty, "signing-value1")
                                                            .build();

        authSchemeOption =
            authSchemeOption.copy(builder -> builder.putIdentityProperty(identityProperty, "identity-value2")
                                                    .putSignerProperty(signerProperty, "signing-value2"));

        assertEquals("identity-value2", authSchemeOption.identityProperty(identityProperty));
        assertEquals("signing-value2", authSchemeOption.signerProperty(signerProperty));
    }
}
