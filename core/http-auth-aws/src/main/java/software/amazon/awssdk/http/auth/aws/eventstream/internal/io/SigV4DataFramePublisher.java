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

package software.amazon.awssdk.http.auth.aws.eventstream.internal.io;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.computeSignature;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.deriveSigningKey;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.hash;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.AddingTrailingDataSubscriber;
import software.amazon.awssdk.utils.internal.MappingSubscriber;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

/**
 * A publisher which adapts a publisher by SigV4-signing each frame, and appends an empty trailing frame.
 */
@SdkInternalApi
public final class SigV4DataFramePublisher implements Publisher<ByteBuffer> {

    private static final Logger LOG = Logger.loggerFor(SigV4DataFramePublisher.class);
    private static final String CHUNK_SIGNATURE = ":chunk-signature";
    private static final int PAYLOAD_TRUNCATE_LENGTH = 32;

    private final Publisher<ByteBuffer> sigv4Publisher;

    private SigV4DataFramePublisher(Builder builder) {
        Validate.paramNotNull(builder.publisher, "Publisher");
        Validate.paramNotNull(builder.credentials, "Credentials");
        Validate.paramNotNull(builder.credentialScope, "CredentialScope");
        Validate.paramNotNull(builder.signature, "Signature");
        Validate.paramNotNull(builder.signingClock, "SigningClock");

        Function<ByteBuffer, ByteBuffer> dataFrameSigner = getDataFrameSigner(
            builder.credentials,
            builder.credentialScope,
            builder.signature,
            builder.signingClock
        );

        // Map publisher with signing function
        this.sigv4Publisher = subscriber -> {
            Subscriber<ByteBuffer> signedSubscriber = MappingSubscriber.create(subscriber, dataFrameSigner);

            Subscriber<ByteBuffer> trailingSubscriber = new AddingTrailingDataSubscriber<>(
                signedSubscriber,
                () -> Collections.singletonList(ByteBuffer.wrap(new byte[] {}))
            );

            builder.publisher.subscribe(trailingSubscriber);
        };
    }

    private static Function<ByteBuffer, ByteBuffer> getDataFrameSigner(AwsCredentialsIdentity credentials,
                                                                       CredentialScope credentialScope,
                                                                       String signature,
                                                                       Clock signingClock) {
        return new Function<ByteBuffer, ByteBuffer>() {

            /**
             * Initiate rolling signature with an initial signature
             */
            String priorSignature = signature;

            @Override
            public ByteBuffer apply(ByteBuffer byteBuffer) {
                /**
                 * Signing Date
                 */
                Map<String, HeaderValue> eventHeaders = new HashMap<>();
                Instant signingInstant = signingClock.instant();
                eventHeaders.put(":date", HeaderValue.fromTimestamp(signingInstant));

                /**
                 * Derive Signing Key - since a stream of events could be over a period of time, we should update
                 * the credential scope with the new instant every time the data-frame signer is called
                 */
                CredentialScope updatedCredentialScope = new CredentialScope(credentialScope.getRegion(),
                                                                             credentialScope.getService(), signingInstant);
                byte[] signingKey = deriveSigningKey(credentials, updatedCredentialScope);

                /**
                 * Calculate rolling signature
                 */
                byte[] payload = new byte[byteBuffer.remaining()];
                byteBuffer.get(payload);
                byte[] signatureBytes = signEvent(priorSignature, signingKey, updatedCredentialScope, eventHeaders, payload);
                priorSignature = BinaryUtils.toHex(signatureBytes);

                /**
                 * Add signing layer headers
                 */
                Map<String, HeaderValue> headers = new HashMap<>(eventHeaders);
                //Signature headers
                headers.put(CHUNK_SIGNATURE, HeaderValue.fromByteArray(signatureBytes));

                /**
                 * Wrap payload and headers in a Message object and then encode to bytes
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
     * Sign an event/chunk via SigV4
     *
     * @param priorSignature  signature of previous frame
     * @param signingKey      derived signing key
     * @param credentialScope the credential-scope used to provide region, service, and time
     * @param eventHeaders    headers pertinent to the event
     * @param event           an event of a bytes to sign
     * @return encoded event with signature
     */
    private static byte[] signEvent(
        String priorSignature,
        byte[] signingKey,
        CredentialScope credentialScope,
        Map<String, HeaderValue> eventHeaders,
        byte[] event) {

        // String to sign
        String eventHeadersSignature = BinaryUtils.toHex(hash(Message.encodeHeaders(sortHeaders(eventHeaders).entrySet())));
        String eventHash = BinaryUtils.toHex(hash(event));
        String stringToSign =
            "AWS4-HMAC-SHA256-PAYLOAD" + SignerConstant.LINE_SEPARATOR +
            credentialScope.getDatetime() + SignerConstant.LINE_SEPARATOR +
            credentialScope.scope() + SignerConstant.LINE_SEPARATOR +
            priorSignature + SignerConstant.LINE_SEPARATOR +
            eventHeadersSignature + SignerConstant.LINE_SEPARATOR +
            eventHash;

        // calculate signature
        return computeSignature(stringToSign, signingKey);
    }

    /**
     * Sort event headers in alphabetic order, with exception that CHUNK_SIGNATURE header always at last
     *
     * @param headers unsorted event headers
     * @return sorted event headers
     */
    private static TreeMap<String, HeaderValue> sortHeaders(Map<String, HeaderValue> headers) {
        TreeMap<String, HeaderValue> sortedHeaders = new TreeMap<>((header1, header2) -> {
            // CHUNK_SIGNATURE should always be the last header
            if (header1.equals(CHUNK_SIGNATURE)) {
                return 1; // put header1 at last
            }
            if (header2.equals(CHUNK_SIGNATURE)) {
                return -1; // put header2 at last
            }
            return header1.compareTo(header2);
        });
        sortedHeaders.putAll(headers);
        return sortedHeaders;
    }

    private static String toDebugString(Message m, boolean truncatePayload) {
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
            // Would be nice if BinaryUtils.toHex() could take an array index range instead, so we don't need to copy
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        sigv4Publisher.subscribe(subscriber);
    }

    public static class Builder {
        private Publisher<ByteBuffer> publisher;
        private AwsCredentialsIdentity credentials;
        private CredentialScope credentialScope;
        private String signature;
        private Clock signingClock;

        public Builder publisher(Publisher<ByteBuffer> publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder credentials(AwsCredentialsIdentity credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder credentialScope(CredentialScope credentialScope) {
            this.credentialScope = credentialScope;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder signingClock(Clock signingClock) {
            this.signingClock = signingClock;
            return this;
        }

        public SigV4DataFramePublisher build() {
            return new SigV4DataFramePublisher(this);
        }

    }
}
