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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3ExpressHttpSigner implements HttpSigner<S3ExpressSessionCredentials> {
    private static final String SESSION_HEADER = "x-amz-s3session-token";
    private static final String SESSION_HEADER_PRESIGNING = "X-Amz-S3session-Token";
    private final HttpSigner<AwsCredentialsIdentity> signer;

    private DefaultS3ExpressHttpSigner(HttpSigner<AwsCredentialsIdentity> signer) {
        this.signer = Validate.notNull(signer, "signer");
    }

    public static DefaultS3ExpressHttpSigner create(HttpSigner<AwsCredentialsIdentity> signer) {
        return new DefaultS3ExpressHttpSigner(signer);
    }

    public static DefaultS3ExpressHttpSigner create() {
        return new DefaultS3ExpressHttpSigner(AwsV4HttpSigner.create());
    }

    @Override
    public SignedRequest sign(SignRequest<? extends S3ExpressSessionCredentials> request) {
        SdkHttpRequest requestToSign = withSessionHeader(request);
        SignRequest<? extends AwsCredentialsIdentity> signRequest = request.copy(b -> b.request(requestToSign));
        return signer.sign(signRequest);
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends S3ExpressSessionCredentials> request) {
        SdkHttpRequest requestWithHeader = withSessionHeader(request);
        return signer.signAsync(request.copy(b -> b.request(requestWithHeader)));
    }

    static SdkHttpRequest withSessionHeader(BaseSignRequest<?, ? extends S3ExpressSessionCredentials> request) {
        S3ExpressSessionCredentials identity = request.identity();
        Validate.notNull(identity, "request identity cannot be null");
        if (request.property(AwsV4FamilyHttpSigner.AUTH_LOCATION) == AuthLocation.QUERY_STRING) {
            return request.request().copy(b -> b.putRawQueryParameter(SESSION_HEADER_PRESIGNING, identity.sessionToken()));
        }
        return request.request().copy(b -> b.putHeader(SESSION_HEADER, identity.sessionToken()));
    }

}
