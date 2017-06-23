/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.internal.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.RequestSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.http.DefaultSdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.opensdk.BaseRequest;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerAware;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerNotFoundException;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerProvider;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;

@RunWith(MockitoJUnitRunner.class)
public class RequestSignerSignerProviderTest {

    @Mock
    private RequestSignerProvider requestSignerProvider;

    @Mock
    private RequestSigner requestSigner;

    @Mock
    private SdkHttpFullRequest request;

    @Mock
    private AwsCredentials credentials;

    @Mock
    private RequestConfig requestConfig;

    @InjectMocks
    private SignerProviderAdapter sut;

    @Test
    public void providesSignerFromUnderlyingRegistry() {
        when(requestSignerProvider.getSigner(SomeTestRequestSigner.class))
                .thenReturn(Optional.of(requestSigner));
        SignerProviderContext context = requestContextWithRequest(new RequestSignerAwareRequest());

        Signer signer = sut.getSigner(context);
        signer.sign(request, credentials);

        verify(requestSigner).sign(request);
    }

    @Test(expected = RequestSignerNotFoundException.class)
    public void throwsAnExceptionWhenCantFindAnSignerWhenGivenASignerAwareRequest() {
        when(requestSignerProvider.getSigner(SomeTestRequestSigner.class))
                .thenReturn(Optional.empty());
        SignerProviderContext context = requestContextWithRequest(new RequestSignerAwareRequest());

        sut.getSigner(context);
    }

    public void returnsNullForNonSignerAwareRequest() {
        SignerProviderContext context = requestContextWithRequest(new NonSignerAwareRequest());

        assertThat(sut.getSigner(context), is(nullValue()));
    }

    private SignerProviderContext requestContextWithRequest(BaseRequest originalRequest) {
        SignerProviderContext context = mock(SignerProviderContext.class);
        SdkHttpFullRequest request = DefaultSdkHttpFullRequest.builder().build();
        doReturn(request).when(context).getRequest();
        when(requestConfig.getOriginalRequest()).thenReturn(originalRequest);
        when(context.getRequestConfig()).thenReturn(requestConfig);
        return context;
    }

    private interface SomeTestRequestSigner extends RequestSigner {
    }

    private class RequestSignerAwareRequest extends BaseRequest implements RequestSignerAware {
        @Override
        public Class<? extends RequestSigner> signerType() {
            return SomeTestRequestSigner.class;
        }
    }

    private class NonSignerAwareRequest extends BaseRequest {
    }
}
