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

package software.amazon.awssdk.services.s3.s3express;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.internal.s3express.DefaultS3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;

/**
 * An auth scheme for faster authentication when interacting with S3 express.
 *
 * <p>This authentication scheme performs pre-authentication with S3 express using {@link S3Client#createSession}. These
 * {@link S3ExpressSessionCredentials} are cached for future requests to the same bucket that are using the same credential
 * provider.
 *
 * <p>By default, this auth scheme is included on every {@link S3Client} instance and does not need to be explicitly enabled.
 * If S3 express authentication is not desired for any reason, you can disable it on the client with
 * {@link S3ClientBuilder#disableS3ExpressSessionAuth(Boolean)}. When disabled, normal sigv4 will be used instead.
 *
 * @see S3ExpressSessionCredentials
 * @see S3Client#createSession(CreateSessionRequest)
 */
@SdkPublicApi
public interface S3ExpressAuthScheme extends AuthScheme<S3ExpressSessionCredentials> {

    /**
     * The Scheme-Id for S3Express auth scheme
     */
    String SCHEME_ID = "aws.auth#sigv4-s3express";

    static S3ExpressAuthScheme create() {
        return DefaultS3ExpressAuthScheme.create();
    }

    /**
     * Retrieve the {@link AwsCredentialsIdentity} based {@link IdentityProvider} associated with this authentication scheme.
     */
    @Override
    IdentityProvider<S3ExpressSessionCredentials> identityProvider(IdentityProviders providers);

    /**
     * Retrieve the {@link AwsV4HttpSigner} associated with this authentication scheme.
     */
    @Override
    HttpSigner<S3ExpressSessionCredentials> signer();
}
