/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@RunWith(MockitoJUnitRunner.class)
public class SignerAsRequestSignerTest {

    @Mock Signer signer;

    @Mock AwsCredentialsProvider credentialsProvider;

    @Mock AwsCredentials credentials;

    @Mock
    SdkHttpFullRequest request;

    @InjectMocks SignerAsRequestSigner sut;

    @Test
    public void callsDownToUnderlyingSignerWithCredentialsFromCredentialProvider() {
        when(credentialsProvider.getCredentials()).thenReturn(credentials);
        sut.sign(request);
        verify(signer).sign(request, credentials);
    }

}
