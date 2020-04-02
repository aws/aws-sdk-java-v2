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

import static software.amazon.awssdk.auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;


@SdkInternalApi
public abstract class BaseEventStreamAsyncAws4Signer extends BaseAsyncAws4Signer {
    //Constants for event stream headers
    public static final String EVENT_STREAM_SIGNATURE = ":chunk-signature";
    public static final String EVENT_STREAM_DATE = ":date";

    private static final Logger LOG = Logger.loggerFor(BaseEventStreamAsyncAws4Signer.class);
    private static final String HTTP_CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-EVENTS";
    private static final String EVENT_STREAM_PAYLOAD = "AWS4-HMAC-SHA256-PAYLOAD";

    private static final int PAYLOAD_TRUNCATE_LENGTH = 32;


    protected BaseEventStreamAsyncAws4Signer() {
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        request = addContentSha256Header(request);
        return super.sign(request, executionAttributes);
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, Aws4SignerParams signingParams) {
        request = addContentSha256Header(request);
        return super.sign(request, signingParams);
    }

    @Override
    protected AsyncRequestBody transformRequestProvider(String headerSignature,
                                                        Aws4SignerRequestParams signerRequestParams,
                                                        Aws4SignerParams signerParams,
                                                        AsyncRequestBody asyncRequestBody) {
        /**
         * Concat trailing empty frame to publisher
         */
        Publisher<ByteBuffer> publisherWithTrailingEmptyFrame = appendEmptyFrame(asyncRequestBody);

        /**
         * Map publisher with signing function
         */
        Publisher<ByteBuffer> publisherWithSignedFrame =
            transformRequestBodyPublisher(publisherWithTrailingEmptyFrame, headerSignature,
                                          signerParams.awsCredentials(), signerRequestParams);

        AsyncRequestBody transformedRequestBody = AsyncRequestBody.fromPublisher(publisherWithSignedFrame);

        return new SigningRequestBodyProvider(transformedRequestBody);
    }

    /**
     * Returns the pre-defined header value and set other necessary headers if
     * the request needs to be chunk-encoded. Otherwise calls the superclass
     * method which calculates the hash of the whole content for signing.
     */
    @Override
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, Aws4SignerParams signerParams) {
        return HTTP_CONTENT_SHA_256;
    }

    private static Publisher<ByteBuffer> appendEmptyFrame(Publisher<ByteBuffer> publisher) {
        return s -> {
            Subscriber<ByteBuffer> adaptedSubscriber = new AsyncSigV4SubscriberAdapter(s);
            publisher.subscribe(adaptedSubscriber);
        };
    }

    private Publisher<ByteBuffer> transformRequestBodyPublisher(Publisher<ByteBuffer> publisher, String headerSignature,
                                                                AwsCredentials credentials,
                                                                Aws4SignerRequestParams signerRequestParams) {
        return SdkPublisher.adapt(publisher)
                           .map(getDataFrameSigner(headerSignature, credentials, signerRequestParams));
    }

    private Function<ByteBuffer, ByteBuffer> getDataFrameSigner(String headerSignature,
                                                                AwsCredentials credentials,
                                                                Aws4SignerRequestParams signerRequestParams) {
        return new Function<ByteBuffer, ByteBuffer>() {
            final Aws4SignerRequestParams requestParams = signerRequestParams;

            /**
             * Initiate rolling signature with header signature
             */
            String priorSignature = headerSignature;

            @Override
            public ByteBuffer apply(ByteBuffer byteBuffer) {
                /**
                 * Signing Date
                 */
                Map<String, HeaderValue> nonSignatureHeaders = new HashMap<>();
                Instant signingInstant = requestParams.getSigningClock().instant();
                nonSignatureHeaders.put(EVENT_STREAM_DATE, HeaderValue.fromTimestamp(signingInstant));

                /**
                 * Derive Signing Key
                 */
                AwsCredentials sanitizedCredentials = sanitizeCredentials(credentials);
                byte[] signingKey = deriveSigningKey(sanitizedCredentials,
                                                     signingInstant,
                                                     requestParams.getRegionName(),
                                                     requestParams.getServiceSigningName());
                /**
                 * Calculate rolling signature
                 */
                byte[] payload = byteBuffer.array();
                byte[] signatureBytes = signEventStream(priorSignature, signingKey, signingInstant, requestParams,
                                                        nonSignatureHeaders, payload);
                priorSignature = BinaryUtils.toHex(signatureBytes);

                /**
                 * Add signing layer event-stream headers
                 */
                Map<String, HeaderValue> headers = new HashMap<>(nonSignatureHeaders);
                //Signature headers
                headers.put(EVENT_STREAM_SIGNATURE, HeaderValue.fromByteArray(signatureBytes));

                /**
                 * Encode signed event to byte
                 */
                Message signedMessage = new Message(sortHeaders(headers), payload);

                if (LOG.isLoggingLevelEnabled("trace")) {
                    LOG.trace(() -> "Signed message: " + toDebugString(signedMessage, false));
                } else {
                    LOG.debug(() -> "Signed message: " + toDebugString(signedMessage, true));
                }

                return signedMessage.toByteBuffer();
            }
        };
    }


    /**
     * Sign event stream with SigV4 signature
     *
     * @param priorSignature signature of previous frame (Header frame is the 0th frame)
     * @param signingKey derived signing key
     * @param signingInstant the instant at which this message is being signed
     * @param requestParams request parameters
     * @param nonSignatureHeaders non-signature headers
     * @param payload event stream payload
     * @return encoded event with signature
     */
    private byte[] signEventStream(
        String priorSignature,
        byte[] signingKey,
        Instant signingInstant,
        Aws4SignerRequestParams requestParams,
        Map<String, HeaderValue> nonSignatureHeaders,
        byte[] payload) {

        // String to sign
        String stringToSign =
            EVENT_STREAM_PAYLOAD +
            SignerConstant.LINE_SEPARATOR +
            Aws4SignerUtils.formatTimestamp(signingInstant) +
            SignerConstant.LINE_SEPARATOR +
            computeScope(signingInstant, requestParams) +
            SignerConstant.LINE_SEPARATOR +
            priorSignature +
            SignerConstant.LINE_SEPARATOR +
            BinaryUtils.toHex(hash(Message.encodeHeaders(sortHeaders(nonSignatureHeaders).entrySet()))) +
            SignerConstant.LINE_SEPARATOR +
            BinaryUtils.toHex(hash(payload));

        // calculate signature
        return sign(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey,
                    SigningAlgorithm.HmacSHA256);
    }

    private String computeScope(Instant signingInstant, Aws4SignerRequestParams requestParams) {
        return Aws4SignerUtils.formatDateStamp(signingInstant) + "/" +
               requestParams.getRegionName() + "/" +
               requestParams.getServiceSigningName() + "/" +
               SignerConstant.AWS4_TERMINATOR;
    }

    /**
     * Sort headers in alphabetic order, with exception that EVENT_STREAM_SIGNATURE header always at last
     *
     * @param headers unsorted headers
     * @return sorted headers
     */
    private TreeMap<String, HeaderValue> sortHeaders(Map<String, HeaderValue> headers) {
        TreeMap<String, HeaderValue> sortedHeaders = new TreeMap<>((header1, header2) -> {
            // signature header should always be the last header
            if (header1.equals(EVENT_STREAM_SIGNATURE)) {
                return 1; // put header1 at last
            } else if (header2.equals(EVENT_STREAM_SIGNATURE)) {
                return -1; // put header2 at last
            } else {
                return header1.compareTo(header2);
            }
        });
        sortedHeaders.putAll(headers);
        return sortedHeaders;
    }

    private SdkHttpFullRequest addContentSha256Header(SdkHttpFullRequest request) {
        return request.toBuilder()
                      .putHeader(X_AMZ_CONTENT_SHA256, "STREAMING-AWS4-HMAC-SHA256-EVENTS").build();
    }

    /**
     * {@link AsyncRequestBody} implementation that use the provider that signs the events.
     * Using anonymous class raises spot bug violation
     */
    private static class SigningRequestBodyProvider implements AsyncRequestBody {

        private AsyncRequestBody transformedRequestBody;

        SigningRequestBodyProvider(AsyncRequestBody transformedRequestBody) {
            this.transformedRequestBody = transformedRequestBody;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            transformedRequestBody.subscribe(s);
        }

        @Override
        public Optional<Long> contentLength() {
            return transformedRequestBody.contentLength();
        }
    }

    static String toDebugString(Message m, boolean truncatePayload) {
        StringBuilder sb = new StringBuilder("Message = {headers={");
        Map<String, HeaderValue> headers = m.getHeaders();

        Iterator<Map.Entry<String, HeaderValue>> headersIter = headers.entrySet().iterator();

        while (headersIter.hasNext()) {
            Map.Entry<String, HeaderValue> h = headersIter.next();

            sb.append(h.getKey()).append("={").append(h.getValue().toString()).append("}");

            if (headersIter.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("}, payload=");

        byte[] payload = m.getPayload();
        byte[] payloadToLog;

        // We don't actually need to truncate if the payload length is already within the truncate limit
        truncatePayload = truncatePayload && payload.length > PAYLOAD_TRUNCATE_LENGTH;

        if (truncatePayload) {
            // Would be nice if BinaryUtils.toHex() could take an array index range instead so we don't need to copy
            payloadToLog = Arrays.copyOf(payload, PAYLOAD_TRUNCATE_LENGTH);
        } else {
            payloadToLog = payload;
        }

        sb.append(BinaryUtils.toHex(payloadToLog));

        if (truncatePayload) {
            sb.append("...");
        }

        sb.append("}");

        return sb.toString();
    }
}
