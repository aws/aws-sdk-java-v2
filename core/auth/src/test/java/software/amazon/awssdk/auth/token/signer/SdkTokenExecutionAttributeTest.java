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

package software.amazon.awssdk.auth.token.signer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;

class SdkTokenExecutionAttributeTest {
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
    public void awsCredentials_oldAndNewAttributeAreMirrored() {
        SdkToken token = Mockito.mock(SdkToken.class);

        // If selected auth scheme is null, writing non-null old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        attributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, token);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME).identity().join()).isSameAs(token);

        // If selected auth scheme is null, writing null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        attributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, null);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME).identity().join()).isNull();

        // If selected auth scheme is non-null, writing non-null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, EMPTY_SELECTED_AUTH_SCHEME);
        attributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, token);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME).identity().join()).isSameAs(token);

        // If selected auth scheme is non-null, writing null to old property can be read with new property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, EMPTY_SELECTED_AUTH_SCHEME);
        attributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, null);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME).identity().join()).isNull();

        // Writing non-null new property can be read with old property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                new SelectedAuthScheme<>(CompletableFuture.completedFuture(token),
                                                         EMPTY_SELECTED_AUTH_SCHEME.signer(),
                                                         EMPTY_SELECTED_AUTH_SCHEME.authSchemeOption()));
        assertThat(attributes.getAttribute(SdkTokenExecutionAttribute.SDK_TOKEN)).isSameAs(token);

        // Writing null new property can be read with old property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                new SelectedAuthScheme<>(CompletableFuture.completedFuture(null),
                                                         EMPTY_SELECTED_AUTH_SCHEME.signer(),
                                                         EMPTY_SELECTED_AUTH_SCHEME.authSchemeOption()));
        assertThat(attributes.getAttribute(SdkTokenExecutionAttribute.SDK_TOKEN)).isNull();

        // Null selected auth scheme can be read with old property
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, null);
        assertThat(attributes.getAttribute(SdkTokenExecutionAttribute.SDK_TOKEN)).isNull();
    }
}