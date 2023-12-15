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

package software.amazon.awssdk.http.auth.spi.scheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.IdentityProperty;

class AuthSchemeOptionTest {
    private static final IdentityProperty<String> IDENTITY_PROPERTY_1 = IdentityProperty.create(AuthSchemeOptionTest.class, "identityKey1");
    private static final SignerProperty<String> SIGNER_PROPERTY_1 = SignerProperty.create(AuthSchemeOptionTest.class, "signingKey1");
    private static final IdentityProperty<String> IDENTITY_PROPERTY_2 = IdentityProperty.create(AuthSchemeOptionTest.class, "identityKey2");
    private static final SignerProperty<String> SIGNER_PROPERTY_2 = SignerProperty.create(AuthSchemeOptionTest.class, "signingKey2");

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

        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(IDENTITY_PROPERTY_1, "identity-value1")
                                                            .putIdentityProperty(IDENTITY_PROPERTY_1, "identity-value2")
                                                            .putSignerProperty(SIGNER_PROPERTY_1, "signing-value1")
                                                            .putSignerProperty(SIGNER_PROPERTY_1, "signing-value2")
                                                            .build();

        assertEquals("identity-value2", authSchemeOption.identityProperty(IDENTITY_PROPERTY_1));
        assertEquals("signing-value2", authSchemeOption.signerProperty(SIGNER_PROPERTY_1));
    }

    @Test
    public void copyBuilder_addProperty_retains() {
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(IDENTITY_PROPERTY_1, "identity-value1")
                                                            .putSignerProperty(SIGNER_PROPERTY_1, "signing-value1")
                                                            .build();

        authSchemeOption =
            authSchemeOption.copy(builder -> builder.putIdentityProperty(IDENTITY_PROPERTY_2, "identity2-value1")
                                                    .putSignerProperty(SIGNER_PROPERTY_2, "signing2-value1"));

        assertEquals("identity-value1", authSchemeOption.identityProperty(IDENTITY_PROPERTY_1));
        assertEquals("identity2-value1", authSchemeOption.identityProperty(IDENTITY_PROPERTY_2));
        assertEquals("signing-value1", authSchemeOption.signerProperty(SIGNER_PROPERTY_1));
        assertEquals("signing2-value1", authSchemeOption.signerProperty(SIGNER_PROPERTY_2));
    }

    @Test
    public void copyBuilder_updateProperty_updates() {
        AuthSchemeOption authSchemeOption = AuthSchemeOption.builder()
                                                            .schemeId("my.api#myAuth")
                                                            .putIdentityProperty(IDENTITY_PROPERTY_1, "identity-value1")
                                                            .putSignerProperty(SIGNER_PROPERTY_1, "signing-value1")
                                                            .build();

        authSchemeOption =
            authSchemeOption.copy(builder -> builder.putIdentityProperty(IDENTITY_PROPERTY_1, "identity-value2")
                                                    .putSignerProperty(SIGNER_PROPERTY_1, "signing-value2"));

        assertEquals("identity-value2", authSchemeOption.identityProperty(IDENTITY_PROPERTY_1));
        assertEquals("signing-value2", authSchemeOption.signerProperty(SIGNER_PROPERTY_1));
    }
}
