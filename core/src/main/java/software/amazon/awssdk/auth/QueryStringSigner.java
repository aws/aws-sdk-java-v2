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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.util.CredentialUtils;

/**
 * Signer implementation responsible for signing an AWS query string request
 * according to the various signature versions and hashing algorithms.
 */
public class QueryStringSigner extends AbstractAwsSigner {
    /**
     * Date override for testing only.
     */
    private Date overriddenDate;

    /**
     * This signer will add "Signature" parameter to the request. Default
     * signature version is "2" and default signing algorithm is "HmacSHA256".
     *
     * AWSAccessKeyId SignatureVersion SignatureMethod Timestamp Signature
     *
     * @param request     request to be signed.
     * @param credentials The credentials used to use to sign the request.
     */
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, AwsCredentials credentials)
            throws SdkClientException {
        return sign(request.toBuilder(), SigningAlgorithm.HmacSHA256, credentials).build();
    }

    /**
     * This signer will add following authentication parameters to the request:
     *
     * AWSAccessKeyId SignatureVersion SignatureMethod Timestamp Signature
     *
     * @param mutableRequest request to be signed.
     * @param algorithm      signature algorithm. "HmacSHA256" is recommended.
     */
    private SdkHttpFullRequest.Builder sign(SdkHttpFullRequest.Builder mutableRequest,
                                            SigningAlgorithm algorithm, AwsCredentials credentials)
            throws SdkClientException {
        // annonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(credentials)) {
            return mutableRequest;
        }

        AwsCredentials sanitizedCredentials = sanitizeCredentials(credentials);
        mutableRequest.queryParameter("AWSAccessKeyId", sanitizedCredentials.accessKeyId());
        mutableRequest.queryParameter("SignatureVersion", "2");

        int timeOffset = mutableRequest.handlerContext(AwsHandlerKeys.TIME_OFFSET) == null ? 0 :
                mutableRequest.handlerContext(AwsHandlerKeys.TIME_OFFSET);
        mutableRequest.queryParameter("Timestamp", getFormattedTimestamp(timeOffset));

        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            addSessionCredentials(mutableRequest, (AwsSessionCredentials) sanitizedCredentials);
        }

        mutableRequest.queryParameter("SignatureMethod", algorithm.toString());
        String stringToSign = calculateStringToSignV2(mutableRequest);

        String signatureValue = signAndBase64Encode(stringToSign,
                                                    sanitizedCredentials.secretAccessKey(), algorithm);
        mutableRequest.queryParameter("Signature", signatureValue);
        return mutableRequest;
    }

    /**
     * Calculate string to sign for signature version 2.
     *
     * @param request The request being signed.
     * @return String to sign
     * @throws SdkClientException If the string to sign cannot be calculated.
     */
    private String calculateStringToSignV2(SdkHttpRequest request) throws SdkClientException {
        return new StringBuilder().append("POST")
                .append("\n")
                .append(getCanonicalizedEndpoint(request.getEndpoint()))
                .append("\n")
                .append(getCanonicalizedResourcePath(request))
                .append("\n")
                .append(getCanonicalizedQueryString(request.getParameters()))
                .toString();
    }

    private String getCanonicalizedResourcePath(SdkHttpRequest request) {
        String resourcePath = "";

        if (request.getEndpoint().getPath() != null) {
            resourcePath += request.getEndpoint().getPath();
        }

        if (request.getResourcePath() != null) {
            if (resourcePath.length() > 0 &&
                !resourcePath.endsWith("/") &&
                !request.getResourcePath().startsWith("/")) {
                resourcePath += "/";
            }

            resourcePath += request.getResourcePath();
        } else if (!resourcePath.endsWith("/")) {
            resourcePath += "/";
        }

        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }

        if (resourcePath.startsWith("//")) {
            resourcePath = resourcePath.substring(1);
        }

        return resourcePath;
    }

    /**
     * Formats date as ISO 8601 timestamp
     */
    private String getFormattedTimestamp(int offset) {
        SimpleDateFormat df = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (overriddenDate != null) {
            return df.format(overriddenDate);
        } else {
            return df.format(getSignatureDate(offset));
        }
    }

    /**
     * For testing purposes only, to control the date used in signing.
     */
    void overrideDate(Date date) {
        this.overriddenDate = date;
    }

    @Override
    protected void addSessionCredentials(SdkHttpFullRequest.Builder request, AwsSessionCredentials credentials) {
        request.queryParameter("SecurityToken", credentials.sessionToken());
    }
}
