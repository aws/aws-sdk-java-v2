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

package software.amazon.awssdk.messagemanager.sns.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Base class for SNS message types. This contains the common fields of SNS messages.
 */
@SdkPublicApi
public abstract class SnsMessage {
    private final String messageId;
    private final String message;
    private final String topicArn;
    private final Instant timestamp;
    private final SdkBytes signature;
    private final SignatureVersion signatureVersion;
    private final URI signingCertUrl;

    SnsMessage(BuilderImpl<?> builder) {
        this.messageId = builder.messageId;
        this.message = builder.message;
        this.topicArn = builder.topicArn;
        this.timestamp = builder.timestamp;
        this.signature = builder.signature;
        this.signatureVersion = builder.signatureVersion;
        this.signingCertUrl = builder.signingCertUrl;
    }

    /**
     * The type of this message.
     */
    public abstract SnsMessageType type();

    /**
     * A Universally Unique Identifier (UUID), unique for each message published. For a message that Amazon SNS resends during a
     * retry, the message ID of the original message is used.
     */
    public String messageId() {
        return messageId;
    }

    /**
     * The message body.
     */
    public String message() {
        return message;
    }

    /**
     * The Amazon Resource Name (ARN) for the topic.
     */
    public String topicArn() {
        return topicArn;
    }

    /**
     * The time (GMT) when the message was sent.
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * SHA1withRSA or SHA256withRSA signature of this message. The values from the message used to calculate the signature are
     * dictated by the message type. See
     * <a href="https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html">the service documentation</a>
     * for more information.
     */
    public SdkBytes signature() {
        return signature;
    }

    /**
     * Version of the Amazon SNS signature used.
     */
    public SignatureVersion signatureVersion() {
        return signatureVersion;
    }

    /**
     * The URL to the certificate that was used to sign the message.
     */
    public URI signingCertUrl() {
        return signingCertUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SnsMessage that = (SnsMessage) o;
        return Objects.equals(messageId, that.messageId)
               && Objects.equals(message, that.message)
               && Objects.equals(topicArn, that.topicArn)
               && Objects.equals(timestamp, that.timestamp)
               && Objects.equals(signature, that.signature)
               && signatureVersion == that.signatureVersion
               && Objects.equals(signingCertUrl, that.signingCertUrl);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(messageId);
        result = 31 * result + Objects.hashCode(message);
        result = 31 * result + Objects.hashCode(topicArn);
        result = 31 * result + Objects.hashCode(timestamp);
        result = 31 * result + Objects.hashCode(signature);
        result = 31 * result + Objects.hashCode(signatureVersion);
        result = 31 * result + Objects.hashCode(signingCertUrl);
        return result;
    }

    public interface Builder<SubclassT extends Builder<?>> {
        /**
         * A Universally Unique Identifier (UUID), unique for each message published. For a message that Amazon SNS resends during
         * a retry, the message ID of the original message is used.
         */
        SubclassT messageId(String messageId);

        /**
         * The message body.
         */
        SubclassT message(String message);

        /**
         * The Amazon Resource Name (ARN) for the topic.
         */
        SubclassT topicArn(String topicArn);

        /**
         * The time (GMT) when the message was sent.
         */
        SubclassT timestamp(Instant timestamp);

        /**
         * SHA1withRSA or SHA256withRSA signature of this message. The values from the message used to calculate the signature are
         * dictated by the message type. See
         * <a href="https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html">the service documentation</a>
         * for more information.
         */
        SubclassT signature(SdkBytes signature);

        /**
         * Version of the Amazon SNS signature used.
         */
        SubclassT signatureVersion(SignatureVersion signatureVersion);

        /**
         * The URL to the certificate that was used to sign the message.
         */
        SubclassT signingCertUrl(URI signingCertUrl);
    }

    @SuppressWarnings("unchecked")
    protected static class BuilderImpl<SubclassT extends Builder<?>> implements Builder<SubclassT> {
        private String messageId;
        private String message;
        private String topicArn;
        private Instant timestamp;
        private SdkBytes signature;
        private SignatureVersion signatureVersion;
        private URI signingCertUrl;

        @Override
        public SubclassT message(String message) {
            this.message = message;
            return (SubclassT) this;
        }

        @Override
        public SubclassT messageId(String messageId) {
            this.messageId = messageId;
            return (SubclassT) this;
        }

        @Override
        public SubclassT topicArn(String topicArn) {
            this.topicArn = topicArn;
            return (SubclassT) this;
        }

        @Override
        public SubclassT timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return (SubclassT) this;
        }

        @Override
        public SubclassT signature(SdkBytes signature) {
            this.signature = signature;
            return (SubclassT) this;
        }

        @Override
        public SubclassT signatureVersion(SignatureVersion signatureVersion) {
            this.signatureVersion = signatureVersion;
            return (SubclassT) this;
        }

        @Override
        public SubclassT signingCertUrl(URI signingCertUrl) {
            this.signingCertUrl = signingCertUrl;
            return (SubclassT) this;
        }
    }
}