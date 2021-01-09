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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.auth.signer.internal.BaseAws4Signer;
import software.amazon.awssdk.auth.signer.internal.DigestComputingSubscriber;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.AsyncSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * AWS Signature Version 4 signer that can include contents of an asynchronous request body into the signature
 * calculation.
 */
@SdkPublicApi
public final class AsyncAws4Signer extends BaseAws4Signer implements AsyncSigner {

    @Override
    public CompletableFuture<SdkHttpFullRequest> sign(SdkHttpFullRequest request,
                                                      AsyncRequestBody requestBody,
                                                      ExecutionAttributes executionAttributes) {
        Aws4SignerParams signingParams = extractSignerParams(Aws4SignerParams.builder(), executionAttributes).build();
        return signWithBody(request, requestBody, signingParams);
    }

    public CompletableFuture<SdkHttpFullRequest> signWithBody(SdkHttpFullRequest request,
                                                                    AsyncRequestBody requestBody,
                                                                    Aws4SignerParams signingParams) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return CompletableFuture.completedFuture(request);
        }

        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256();

        requestBody.subscribe(bodyDigester);

        CompletableFuture<byte[]> digestBytes = bodyDigester.digestBytes();

        CompletableFuture<SdkHttpFullRequest> signedReqFuture = digestBytes.thenApply(bodyHash -> {
            String digestHex = BinaryUtils.toHex(bodyHash);

            Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

            SdkHttpFullRequest.Builder builder = doSign(request, requestParams, signingParams, digestHex);

            return builder.build();
        });

        return CompletableFutureUtils.forwardExceptionTo(signedReqFuture, digestBytes);
    }

    public static AsyncAws4Signer create() {
        return new AsyncAws4Signer();
    }
}
