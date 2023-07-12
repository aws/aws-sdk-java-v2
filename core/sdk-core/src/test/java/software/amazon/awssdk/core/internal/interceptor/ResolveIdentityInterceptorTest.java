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

package software.amazon.awssdk.core.internal.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.IDENTITY;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

@RunWith(MockitoJUnitRunner.class)
public class ResolveIdentityInterceptorTest {

    @Mock
    private Context.BeforeExecution context;

    @Mock
    private HttpSigner<MyIdentity> signer;

    @Mock
    private AuthSchemeOption authSchemeOption;

    // TODO: need to allow nulls in SelectedAuthScheme - https://github.com/aws/aws-sdk-java-v2/pull/4180
    // @Test
    public void noAuth_doesNotResolve() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        SelectedAuthScheme<?> selectedAuthScheme =
            new SelectedAuthScheme<>(null, null, AuthSchemeOption.builder().schemeId("smithy.api#noAuth").build());
        executionAttributes.putAttribute(SELECTED_AUTH_SCHEME, selectedAuthScheme);
        new ResolveIdentityInterceptor().beforeExecution(context, executionAttributes);
        assertThat(executionAttributes.getAttribute(IDENTITY)).isNull();
    }

    @Test
    public void resolvesIdentity() {
        when(authSchemeOption.schemeId()).thenReturn("my.api#myAuth");
        MyIdentity identity = new MyIdentity();
        MyIdentityProvider identityProvider = new MyIdentityProvider(identity);

        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        SelectedAuthScheme<?> selectedAuthScheme =
            new SelectedAuthScheme<>(identityProvider, signer, authSchemeOption);
        executionAttributes.putAttribute(SELECTED_AUTH_SCHEME, selectedAuthScheme);

        new ResolveIdentityInterceptor().beforeExecution(context, executionAttributes);
        assertThat(executionAttributes.getAttribute(IDENTITY).join()).isEqualTo(identity);
        verify(authSchemeOption).forEachIdentityProperty(any(AuthSchemeOption.IdentityPropertyConsumer.class));
    }

    private static class MyIdentity implements Identity {
    }

    private static class MyIdentityProvider implements IdentityProvider<MyIdentity> {

        private final MyIdentity identity;

        public MyIdentityProvider(MyIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Class<MyIdentity> identityType() {
            return MyIdentity.class;
        }

        @Override
        public CompletableFuture<? extends MyIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(identity);
        }
    }
}
