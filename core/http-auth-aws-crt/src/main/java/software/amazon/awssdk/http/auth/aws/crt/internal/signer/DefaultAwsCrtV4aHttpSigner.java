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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.validatedProperty;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * A default implementation of {@link AwsCrtV4aHttpSigner}.
 */
@SdkInternalApi
public final class DefaultAwsCrtV4aHttpSigner implements AwsCrtV4aHttpSigner {

    /**
     * Given a request with a set of properties, determine which signer to delegate to, and call it with the request.
     */
    private static AwsCrtV4aHttpSigner getDelegate(
        SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {

        // create the base signer
        BaseAwsCrtV4aHttpSigner<?> v4aSigner = BaseAwsCrtV4aHttpSigner.create();
        return getDelegate(v4aSigner, signRequest);
    }

    /**
     * Given a request with a set of properties and a base signer, compose an implementation with the base
     * signer based on properties, and delegate the request to the composed signer.
     */
    public static AwsCrtV4aHttpSigner getDelegate(
        BaseAwsCrtV4aHttpSigner<?> v4aSigner,
        SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {

        // get the properties to decide on
        Boolean isPayloadSigning = validatedProperty(signRequest, PAYLOAD_SIGNING, false);
        Boolean isChunkedEncoding = validatedProperty(signRequest, CHUNKED_ENCODING, false);

        if (isPayloadSigning || isChunkedEncoding) {
            v4aSigner = new DefaultAwsCrtS3V4aHttpSigner((BaseAwsCrtV4aHttpSigner<AwsCrtV4aHttpProperties>) v4aSigner,
                AwsChunkedEncodingConfig.create());
        }

        return v4aSigner;
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).sign(request);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).signAsync(request);
    }

}
