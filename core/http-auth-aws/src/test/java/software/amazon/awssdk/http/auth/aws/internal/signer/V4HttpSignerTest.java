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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicAsyncRequest;
import static software.amazon.awssdk.http.auth.aws.TestUtils.generateBasicRequest;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.TestUtils.AnonymousCredentialsIdentity;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;
import software.amazon.awssdk.http.auth.aws.signer.V4Context;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.signer.V4RequestSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public class V4HttpSignerTest {

    Checksummer checksummer = mock(Checksummer.class);

    V4RequestSigner requestSigner = mock(V4RequestSigner.class);

    V4PayloadSigner payloadSigner = mock(V4PayloadSigner.class);

    V4Context v4Context = mock(V4Context.class);

    SdkHttpRequest.Builder signedRequestBuilder = mock(SdkHttpRequest.Builder.class);

    SdkHttpRequest signedRequest = mock(SdkHttpRequest.class);

    V4HttpSigner signer = new V4HttpSigner(checksummer, requestSigner, payloadSigner);

    @BeforeEach
    public void setUp() {
        when(requestSigner.sign(any(SdkHttpRequest.Builder.class))).thenReturn(v4Context);
        when(v4Context.getSignedRequest()).thenReturn(signedRequestBuilder);
        when(signedRequestBuilder.build()).thenReturn(signedRequest);
    }

    @Test
    public void sign_shouldDelegateToComponents() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );
        ContentStreamProvider payload = request.payload().get();
        when(checksummer.checksum(payload)).thenReturn("checksum");

        signer.sign(request);

        verify(checksummer).checksum(payload);
        verify(requestSigner).sign(argThat(arg -> arg.firstMatchingHeader("x-amz-content-sha256").isPresent()));
        verify(payloadSigner).sign(payload, v4Context);
    }

    @Test
    public void sign_withAnonymousCreds_shouldNotSign() {
        SyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicRequest(
            new AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        signer.sign(request);

        verifyNoInteractions(checksummer);
        verifyNoInteractions(requestSigner);
        verifyNoInteractions(payloadSigner);
    }

    @Test
    public void signAsync_shouldDelegateToComponents() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            AwsCredentialsIdentity.create("access", "secret"),
            httpRequest -> {
            },
            signRequest -> {
            }
        );
        Publisher<ByteBuffer> payload = request.payload().get();
        when(checksummer.checksum(payload)).thenReturn(CompletableFuture.completedFuture("checksum"));

        signer.signAsync(request);

        verify(checksummer).checksum(payload);
        verify(requestSigner).sign(argThat(arg -> arg.firstMatchingHeader("x-amz-content-sha256").isPresent()));
        verify(payloadSigner).sign(
            eq(payload),
            argThat(ctx -> {
                try {
                    return ctx.get().equals(v4Context);
                } catch (Exception e) {
                    return false;
                }
            })
        );
    }

    @Test
    public void signAsync_withAnonymousCreds_shouldNotSign() {
        AsyncSignRequest<? extends AwsCredentialsIdentity> request = generateBasicAsyncRequest(
            new AnonymousCredentialsIdentity(),
            httpRequest -> {
            },
            signRequest -> {
            }
        );

        signer.signAsync(request);

        verifyNoInteractions(checksummer);
        verifyNoInteractions(requestSigner);
        verifyNoInteractions(payloadSigner);
    }
}
