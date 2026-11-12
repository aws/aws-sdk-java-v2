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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.PresignedUrlDownloadRequestWrapper;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link PresignedUrlDownloadRequestWrapper} Marshaller
 *
 * <p>
 * Marshalls presigned URL requests by using the complete URL directly and adding optional Range headers.
 * Unlike regular S3 marshalers, this preserves all embedded authentication parameters in the presigned URL.
 * </p>
 */
@SdkInternalApi
public class PresignedUrlDownloadRequestMarshaller implements Marshaller<PresignedUrlDownloadRequestWrapper> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
            .requestUri("").httpMethod(SdkHttpMethod.GET).hasExplicitPayloadMember(false).hasPayloadMembers(false)
            .putAdditionalMetadata(AwsXmlProtocolFactory.ROOT_MARSHALL_LOCATION_ATTRIBUTE, null)
            .putAdditionalMetadata(AwsXmlProtocolFactory.XML_NAMESPACE_ATTRIBUTE, null).build();

    private final AwsXmlProtocolFactory protocolFactory;

    public PresignedUrlDownloadRequestMarshaller(AwsXmlProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Marshalls the presigned URL request into an HTTP GET request.
     *
     * @param presignedUrlDownloadRequestWrapper the request to marshall
     * @return HTTP request ready for execution
     * @throws SdkClientException if URL conversion fails
     */
    @Override
    public SdkHttpFullRequest marshall(PresignedUrlDownloadRequestWrapper presignedUrlDownloadRequestWrapper) {
        Validate.paramNotNull(presignedUrlDownloadRequestWrapper, "presignedUrlDownloadRequestWrapper");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING);
            URI presignedUri = presignedUrlDownloadRequestWrapper.url().toURI();

            SdkHttpFullRequest.Builder requestBuilder = protocolMarshaller
                .marshall(presignedUrlDownloadRequestWrapper)
                .toBuilder()
                .uri(presignedUri);

            addChecksumModeHeaderIfSignedInUrl(requestBuilder, presignedUri);

            return requestBuilder.build();
        } catch (Exception e) {
            String redactedUrl = PresignedUrlRedactionUtils.redactQueryString(presignedUrlDownloadRequestWrapper.url());
            throw SdkClientException.builder()
                    .message("Unable to marshall pre-signed URL Request for " + redactedUrl + ": " + failureDescription(e))
                    .cause(redactedCause(e, redactedUrl))
                    .build();
        }
    }

    /**
     * Describes a marshalling failure without echoing the presigned URL: {@link URISyntaxException#getMessage()} appends
     * the URL it was given, query string included, so only the reason and the index are used.
     */
    private static String failureDescription(Exception e) {
        if (e instanceof URISyntaxException) {
            URISyntaxException syntaxException = (URISyntaxException) e;
            int index = syntaxException.getIndex();
            return index < 0 ? syntaxException.getReason()
                             : syntaxException.getReason() + " at index " + index;
        }
        return e.getClass().getName();
    }

    /**
     * Rebuilds the failure around the redacted URL, because the chained cause is read back by
     * {@code getCause().getMessage()} and printed by every stack trace that renders the exception. A
     * {@link URISyntaxException} keeps its type, reason and index; any other failure is named by its class only, since its
     * message is not known to be free of the URL. Both carry the frames of the original failure.
     */
    private static Throwable redactedCause(Exception e, String redactedUrl) {
        String url = redactedUrl == null ? "null" : redactedUrl;
        Throwable redacted;
        if (e instanceof URISyntaxException && ((URISyntaxException) e).getReason() != null) {
            URISyntaxException syntaxException = (URISyntaxException) e;
            redacted = new URISyntaxException(url, syntaxException.getReason(), syntaxException.getIndex());
        } else {
            redacted = SdkClientException.builder()
                                         .message(e.getClass().getName() + " for " + url)
                                         .build();
        }
        redacted.setStackTrace(e.getStackTrace());
        return redacted;
    }

    /**
     * If the presigned URL's X-Amz-SignedHeaders contains "x-amz-checksum-mode", automatically add
     * the header with value "ENABLED" so S3 returns checksum headers in the response.
     */
    private void addChecksumModeHeaderIfSignedInUrl(SdkHttpFullRequest.Builder requestBuilder, URI uri) {
        if (hasChecksumModeInSignedHeaders(uri.getQuery())) {
            requestBuilder.putHeader("x-amz-checksum-mode", "ENABLED");
        }
    }

    /**
     * Returns true if the decoded query string's X-Amz-SignedHeaders parameter contains "x-amz-checksum-mode".
     */
    static boolean hasChecksumModeInSignedHeaders(String query) {
        if (query == null) {
            return false;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("X-Amz-SignedHeaders=")) {
                return param.substring("X-Amz-SignedHeaders=".length()).contains("x-amz-checksum-mode");
            }
        }
        return false;
    }
}
