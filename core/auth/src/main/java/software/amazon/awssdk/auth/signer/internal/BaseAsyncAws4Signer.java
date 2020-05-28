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

package software.amazon.awssdk.auth.signer.internal;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.AsyncRequestBodySigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Logger;


@SdkInternalApi
public abstract class BaseAsyncAws4Signer extends BaseAws4Signer implements AsyncRequestBodySigner {
    private static final Logger LOG = Logger.loggerFor(BaseAsyncAws4Signer.class);

    private static final Pattern AUTHENTICATION_HEADER_PATTERN = Pattern.compile(
        SignerConstant.AWS4_SIGNING_ALGORITHM + "\\s" + "Credential=(\\S+)" + "\\s" + "SignedHeaders=(\\S+)" + "\\s"
        + "Signature=(\\S+)");

    protected BaseAsyncAws4Signer() {
    }

    @Override
    public AsyncRequestBody signAsyncRequestBody(SdkHttpFullRequest request, AsyncRequestBody asyncRequestBody,
                                                 ExecutionAttributes executionAttributes) {
        Aws4SignerParams signingParams = extractSignerParams(Aws4SignerParams.builder(), executionAttributes)
            .build();
        Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return signAsync(request, asyncRequestBody, requestParams, signingParams);
    }

    /**
     * This method is only used in test, where clockOverride is passed in signingParams
     */
    @SdkTestInternalApi
    protected final AsyncRequestBody signAsync(SdkHttpFullRequest request, AsyncRequestBody asyncRequestBody,
                                               Aws4SignerRequestParams requestParams, Aws4SignerParams signingParams) {
        String headerSignature = getHeaderSignature(request);
        return transformRequestProvider(headerSignature, requestParams, signingParams, asyncRequestBody);
    }

    /**
     * Transform the original requestProvider by adding signing operator and returns a new requestProvider
     *
     * Can be overriden by subclasses to provide specific signing method
     */
    protected abstract AsyncRequestBody transformRequestProvider(String headerSignature,
                                                                 Aws4SignerRequestParams signerRequestParams,
                                                                 Aws4SignerParams signerParams,
                                                                 AsyncRequestBody asyncRequestBody);

    /**
     * Extract signature from Authentication header
     *
     * @param request signed request with Authentication header
     * @return signature (Hex) string
     */
    private String getHeaderSignature(SdkHttpFullRequest request) {
        Optional<String> authHeader = request.firstMatchingHeader(SignerConstant.AUTHORIZATION);
        if (authHeader.isPresent()) {
            Matcher matcher = AUTHENTICATION_HEADER_PATTERN.matcher(authHeader.get());
            if (matcher.matches()) {
                String headerSignature = matcher.group(3);
                return headerSignature;
            }
        }

        // Without header signature, signer can not proceed. Thus throw out exception
        throw SdkClientException.builder().message("Signature is missing in AUTHORIZATION header!").build();
    }
}
