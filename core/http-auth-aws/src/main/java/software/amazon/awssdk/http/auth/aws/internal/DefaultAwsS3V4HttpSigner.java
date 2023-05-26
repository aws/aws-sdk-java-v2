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

package software.amazon.awssdk.http.auth.aws.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.AwsS3V4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedHttpRequest;
import software.amazon.awssdk.http.auth.spi.SyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A default implementation of {@link AwsS3V4HttpSigner}.
 */
@SdkInternalApi
public class DefaultAwsS3V4HttpSigner implements AwsS3V4HttpSigner {

    @Override
    public SyncSignedHttpRequest sign(SyncHttpSignRequest<? extends AwsCredentialsIdentity> request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<? extends AwsCredentialsIdentity> request) {
        throw new UnsupportedOperationException();
    }
}
