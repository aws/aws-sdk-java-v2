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

package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;

class S3SignerExecutionAttributeTest {
    private static final SelectedAuthScheme<Identity> EMPTY_SELECTED_AUTH_SCHEME =
        new SelectedAuthScheme<>(CompletableFuture.completedFuture(Mockito.mock(Identity.class)),
                                 (HttpSigner<Identity>) Mockito.mock(HttpSigner.class),
                                 AuthSchemeOption.builder().schemeId("mock").build());

    private ExecutionAttributes attributes;

    @BeforeEach
    public void setup() {
        this.attributes = new ExecutionAttributes();
    }

    @Test
    public void enableChunkedEncoding_oldAndNewAttributeAreMirrored() {
        assertOldAndNewBooleanAttributesAreMirrored(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING,
                                                    AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED);
    }

    @Test
    public void enablePayloadSigning_oldAndNewAttributeAreMirrored() {
        assertOldAndNewBooleanAttributesAreMirrored(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING,
                                                    AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED);
    }

    private void assertOldAndNewBooleanAttributesAreMirrored(ExecutionAttribute<Boolean> attribute,
                                                             SignerProperty<Boolean> property) {
        assertOldAndNewAttributesAreMirrored(attribute, property, true, true);
        assertOldAndNewAttributesAreMirrored(attribute, property, false, false);
    }

    private <T, U> void assertOldAndNewAttributesAreMirrored(ExecutionAttribute<T> oldAttribute,
                                                             SignerProperty<U> newProperty,
                                                             T oldPropertyValue,
                                                             U newPropertyValue) {
        // If selected auth scheme is null, writing non-null old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        assertOldAttributeWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, oldPropertyValue, newPropertyValue);

        // If selected auth scheme is null, writing null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        assertOldAttributeWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, null, null);

        // If selected auth scheme is non-null, writing non-null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, EMPTY_SELECTED_AUTH_SCHEME);
        assertOldAttributeWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, oldPropertyValue, newPropertyValue);

        // If selected auth scheme is non-null, writing null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, EMPTY_SELECTED_AUTH_SCHEME);
        assertOldAttributeWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, null, null);

        // Writing non-null new property can be read with old property
        assertNewPropertyWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, oldPropertyValue, newPropertyValue);

        // Writing null new property can be read with old property
        assertNewPropertyWrite_canBeReadFromNewAttribute(oldAttribute, newProperty, null, null);

        // Null selected auth scheme can be read with old property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        assertThat(attributes.getAttribute(oldAttribute)).isNull();
    }

    private <T, U> void assertNewPropertyWrite_canBeReadFromNewAttribute(ExecutionAttribute<T> oldAttribute,
                                                                         SignerProperty<U> newProperty,
                                                                         T oldPropertyValue,
                                                                         U newPropertyValue) {
        AuthSchemeOption newOption =
            EMPTY_SELECTED_AUTH_SCHEME.authSchemeOption().copy(o -> o.putSignerProperty(newProperty, newPropertyValue));
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                new SelectedAuthScheme<>(EMPTY_SELECTED_AUTH_SCHEME.identity(),
                                                         EMPTY_SELECTED_AUTH_SCHEME.signer(),
                                                         newOption));
        assertThat(attributes.getAttribute(oldAttribute)).isEqualTo(oldPropertyValue);
    }

    private <T, U> void assertOldAttributeWrite_canBeReadFromNewAttribute(ExecutionAttribute<T> attributeToWrite,
                                                                          SignerProperty<U> propertyToRead,
                                                                          T valueToWrite,
                                                                          U propertyToExpect) {
        attributes.putAttribute(attributeToWrite, valueToWrite);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                             .authSchemeOption()
                             .signerProperty(propertyToRead)).isEqualTo(propertyToExpect);
    }
}