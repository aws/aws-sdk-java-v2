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

package software.amazon.awssdk.services.sns.internal.messagemanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessage;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessageParsingException;

/**
 * Unit tests for {@link SnsMessageParser}.
 */
class SnsMessageParserTest {

    private static final String VALID_NOTIFICATION_JSON = "{"
        + "\"Type\":\"Notification\","
        + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
        + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
        + "\"Subject\":\"Test Subject\","
        + "\"Message\":\"Test message content\","
        + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
        + "\"SignatureVersion\":\"1\","
        + "\"Signature\":\"test-signature\","
        + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
        + "\"UnsubscribeURL\":\"https://sns.us-east-1.amazonaws.com/unsubscribe\""
        + "}";

    private static final String VALID_SUBSCRIPTION_CONFIRMATION_JSON = "{"
        + "\"Type\":\"SubscriptionConfirmation\","
        + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
        + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
        + "\"Message\":\"You have chosen to subscribe to the topic\","
        + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
        + "\"SignatureVersion\":\"2\","
        + "\"Signature\":\"test-signature\","
        + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
        + "\"Token\":\"confirmation-token-12345\""
        + "}";

    private static final String VALID_UNSUBSCRIBE_CONFIRMATION_JSON = "{"
        + "\"Type\":\"UnsubscribeConfirmation\","
        + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
        + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
        + "\"Message\":\"You have been unsubscribed from the topic\","
        + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
        + "\"SignatureVersion\":\"1\","
        + "\"Signature\":\"test-signature\","
        + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
        + "\"Token\":\"unsubscribe-token-12345\""
        + "}";

    @Test
    void parseMessage_validNotificationMessage_parsesSuccessfully() {
        SnsMessage message = SnsMessageParser.parseMessage(VALID_NOTIFICATION_JSON);

        assertThat(message.type()).isEqualTo("Notification");
        assertThat(message.messageId()).isEqualTo("12345678-1234-1234-1234-123456789012");
        assertThat(message.topicArn()).isEqualTo("arn:aws:sns:us-east-1:123456789012:MyTopic");
        assertThat(message.subject()).hasValue("Test Subject");
        assertThat(message.message()).isEqualTo("Test message content");
        assertThat(message.timestamp()).isEqualTo(Instant.parse("2023-01-01T12:00:00.000Z"));
        assertThat(message.signatureVersion()).isEqualTo("1");
        assertThat(message.signature()).isEqualTo("test-signature");
        assertThat(message.signingCertUrl()).isEqualTo("https://sns.us-east-1.amazonaws.com/cert.pem");
        assertThat(message.unsubscribeUrl()).hasValue("https://sns.us-east-1.amazonaws.com/unsubscribe");
        assertThat(message.token()).isEmpty();
        assertThat(message.messageAttributes()).isEmpty();
    }

    @Test
    void parseMessage_validSubscriptionConfirmationMessage_parsesSuccessfully() {
        SnsMessage message = SnsMessageParser.parseMessage(VALID_SUBSCRIPTION_CONFIRMATION_JSON);

        assertThat(message.type()).isEqualTo("SubscriptionConfirmation");
        assertThat(message.messageId()).isEqualTo("12345678-1234-1234-1234-123456789012");
        assertThat(message.topicArn()).isEqualTo("arn:aws:sns:us-east-1:123456789012:MyTopic");
        assertThat(message.subject()).isEmpty();
        assertThat(message.message()).isEqualTo("You have chosen to subscribe to the topic");
        assertThat(message.timestamp()).isEqualTo(Instant.parse("2023-01-01T12:00:00.000Z"));
        assertThat(message.signatureVersion()).isEqualTo("2");
        assertThat(message.signature()).isEqualTo("test-signature");
        assertThat(message.signingCertUrl()).isEqualTo("https://sns.us-east-1.amazonaws.com/cert.pem");
        assertThat(message.unsubscribeUrl()).isEmpty();
        assertThat(message.token()).hasValue("confirmation-token-12345");
        assertThat(message.messageAttributes()).isEmpty();
    }

    @Test
    void parseMessage_validUnsubscribeConfirmationMessage_parsesSuccessfully() {
        SnsMessage message = SnsMessageParser.parseMessage(VALID_UNSUBSCRIBE_CONFIRMATION_JSON);

        assertThat(message.type()).isEqualTo("UnsubscribeConfirmation");
        assertThat(message.messageId()).isEqualTo("12345678-1234-1234-1234-123456789012");
        assertThat(message.topicArn()).isEqualTo("arn:aws:sns:us-east-1:123456789012:MyTopic");
        assertThat(message.subject()).isEmpty();
        assertThat(message.message()).isEqualTo("You have been unsubscribed from the topic");
        assertThat(message.timestamp()).isEqualTo(Instant.parse("2023-01-01T12:00:00.000Z"));
        assertThat(message.signatureVersion()).isEqualTo("1");
        assertThat(message.signature()).isEqualTo("test-signature");
        assertThat(message.signingCertUrl()).isEqualTo("https://sns.us-east-1.amazonaws.com/cert.pem");
        assertThat(message.unsubscribeUrl()).isEmpty();
        assertThat(message.token()).hasValue("unsubscribe-token-12345");
        assertThat(message.messageAttributes()).isEmpty();
    }

    @Test
    void parseMessage_messageWithMessageAttributes_parsesSuccessfully() {
        String jsonWithAttributes = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"MessageAttributes\":{"
            + "\"attr1\":\"value1\","
            + "\"attr2\":\"value2\""
            + "}"
            + "}";

        SnsMessage message = SnsMessageParser.parseMessage(jsonWithAttributes);

        assertThat(message.messageAttributes()).hasSize(2);
        assertThat(message.messageAttributes()).containsEntry("attr1", "value1");
        assertThat(message.messageAttributes()).containsEntry("attr2", "value2");
    }

    @Test
    void parseMessage_nullInput_throwsException() {
        assertThatThrownBy(() -> SnsMessageParser.parseMessage(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("messageJson must not be null");
    }

    @Test
    void parseMessage_emptyString_throwsException() {
        assertThatThrownBy(() -> SnsMessageParser.parseMessage(""))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON cannot be empty or blank");
    }

    @Test
    void parseMessage_blankString_throwsException() {
        assertThatThrownBy(() -> SnsMessageParser.parseMessage("   "))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON cannot be empty or blank");
    }

    @Test
    void parseMessage_tooLargeMessage_throwsException() {
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 300000; i++) { // Over 256KB
            largeMessage.append("a");
        }

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(largeMessage.toString()))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON is too large");
    }

    @Test
    void parseMessage_invalidJsonFormat_throwsException() {
        String invalidJson = "{ invalid json }";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(invalidJson))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Failed to parse JSON message");
    }

    @Test
    void parseMessage_notJsonObject_throwsException() {
        String jsonArray = "[\"not\", \"an\", \"object\"]";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonArray))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON must start with '{'");
    }

    @Test
    void parseMessage_emptyJsonObject_throwsException() {
        assertThatThrownBy(() -> SnsMessageParser.parseMessage("{}"))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message cannot be empty");
    }

    @Test
    void parseMessage_missingType_throwsException() {
        String jsonWithoutType = "{"
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutType))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Required field 'Type' is missing");
    }

    @Test
    void parseMessage_missingMessageId_throwsException() {
        String jsonWithoutMessageId = "{"
            + "\"Type\":\"Notification\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutMessageId))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("MessageId");
    }

    @Test
    void parseMessage_missingTopicArn_throwsException() {
        String jsonWithoutTopicArn = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutTopicArn))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("TopicArn");
    }

    @Test
    void parseMessage_missingMessage_throwsException() {
        String jsonWithoutMessage = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutMessage))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("Message");
    }

    @Test
    void parseMessage_missingTimestamp_throwsException() {
        String jsonWithoutTimestamp = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutTimestamp))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("Timestamp");
    }

    @Test
    void parseMessage_missingSignatureVersion_throwsException() {
        String jsonWithoutSignatureVersion = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutSignatureVersion))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("SignatureVersion");
    }

    @Test
    void parseMessage_missingSignature_throwsException() {
        String jsonWithoutSignature = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutSignature))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("Signature");
    }

    @Test
    void parseMessage_missingSigningCertURL_throwsException() {
        String jsonWithoutSigningCertURL = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutSigningCertURL))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("SigningCertURL");
    }

    @Test
    void parseMessage_missingTokenForSubscriptionConfirmation_throwsException() {
        String jsonWithoutToken = "{"
            + "\"Type\":\"SubscriptionConfirmation\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"You have chosen to subscribe to the topic\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"2\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithoutToken))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("Token");
    }

    @Test
    void parseMessage_unsupportedMessageType_throwsException() {
        String jsonWithUnsupportedType = "{"
            + "\"Type\":\"UnsupportedType\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithUnsupportedType))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Unsupported message type: UnsupportedType")
            .hasMessageContaining("Supported types are: Notification, SubscriptionConfirmation, UnsubscribeConfirmation");
    }

    @Test
    void parseMessage_unexpectedFields_throwsException() {
        String jsonWithUnexpectedField = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"UnexpectedField\":\"unexpected-value\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithUnexpectedField))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message contains unexpected fields")
            .hasMessageContaining("UnexpectedField");
    }

    @Test
    void parseMessage_nullFieldValue_throwsException() {
        String jsonWithNullField = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":null,"
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithNullField))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields")
            .hasMessageContaining("MessageId");
    }

    @Test
    void parseMessage_emptyFieldValue_throwsException() {
        String jsonWithEmptyField = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithEmptyField))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Required field 'MessageId' cannot be empty or blank");
    }

    @Test
    void parseMessage_nonStringFieldValue_throwsException() {
        String jsonWithNonStringField = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":12345,"
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithNonStringField))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Field 'MessageId' must be a string but found number");
    }

    @Test
    void parseMessage_invalidTimestampFormat_throwsException() {
        String jsonWithInvalidTimestamp = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"invalid-timestamp\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithInvalidTimestamp))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Invalid timestamp format: invalid-timestamp");
    }

    @Test
    void parseMessage_invalidTopicArn_throwsException() {
        String jsonWithInvalidTopicArn = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"invalid-arn\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithInvalidTopicArn))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("TopicArn must be a valid ARN starting with 'arn:'");
    }

    @Test
    void parseMessage_nonSnsTopicArn_throwsException() {
        String jsonWithNonSnsArn = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:s3:::my-bucket\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithNonSnsArn))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("TopicArn must be an SNS topic ARN containing ':sns:'");
    }

    @Test
    void parseMessage_invalidSignatureVersion_throwsException() {
        String jsonWithInvalidSignatureVersion = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"3\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithInvalidSignatureVersion))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("SignatureVersion must be '1' or '2'. Received: '3'");
    }

    @Test
    void parseMessage_nonHttpsSigningCertURL_throwsException() {
        String jsonWithHttpCertUrl = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"http://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithHttpCertUrl))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("SigningCertURL must use HTTPS protocol for security");
    }

    @Test
    void parseMessage_nonHttpsUnsubscribeURL_throwsException() {
        String jsonWithHttpUnsubscribeUrl = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"UnsubscribeURL\":\"http://sns.us-east-1.amazonaws.com/unsubscribe\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithHttpUnsubscribeUrl))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("UnsubscribeURL must use HTTPS protocol for security");
    }

    @Test
    void parseMessage_tooLongMessageId_throwsException() {
        StringBuilder longMessageId = new StringBuilder();
        for (int i = 0; i < 101; i++) { // Over 100 characters
            longMessageId.append("a");
        }

        String jsonWithLongMessageId = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"" + longMessageId.toString() + "\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithLongMessageId))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("MessageId is too long");
    }

    @Test
    void parseMessage_invalidMessageAttributesType_throwsException() {
        String jsonWithInvalidMessageAttributes = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"MessageAttributes\":\"not-an-object\""
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithInvalidMessageAttributes))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("MessageAttributes must be a JSON object");
    }

    @Test
    void parseMessage_invalidMessageAttributeValueType_throwsException() {
        String jsonWithInvalidAttributeValue = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"MessageAttributes\":{"
            + "\"attr1\":123"
            + "}"
            + "}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithInvalidAttributeValue))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("MessageAttribute value for key 'attr1' must be a string");
    }

    @Test
    void parseMessage_nullMessageAttributeValue_skipsAttribute() {
        String jsonWithNullAttributeValue = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\","
            + "\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:MyTopic\","
            + "\"Message\":\"Test message content\","
            + "\"Timestamp\":\"2023-01-01T12:00:00.000Z\","
            + "\"SignatureVersion\":\"1\","
            + "\"Signature\":\"test-signature\","
            + "\"SigningCertURL\":\"https://sns.us-east-1.amazonaws.com/cert.pem\","
            + "\"MessageAttributes\":{"
            + "\"attr1\":\"value1\","
            + "\"attr2\":null,"
            + "\"attr3\":\"value3\""
            + "}"
            + "}";

        SnsMessage message = SnsMessageParser.parseMessage(jsonWithNullAttributeValue);

        assertThat(message.messageAttributes()).hasSize(2);
        assertThat(message.messageAttributes()).containsEntry("attr1", "value1");
        assertThat(message.messageAttributes()).containsEntry("attr3", "value3");
        assertThat(message.messageAttributes()).doesNotContainKey("attr2");
    }

    @Test
    void parseMessage_unbalancedBraces_throwsException() {
        String jsonWithUnbalancedBraces = "{"
            + "\"Type\":\"Notification\","
            + "\"MessageId\":\"12345678-1234-1234-1234-123456789012\"";
        // Missing closing brace

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(jsonWithUnbalancedBraces))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON must end with '}'");
    }

    @Test
    void parseMessage_doesNotStartWithBrace_throwsException() {
        String invalidJson = "invalid{\"Type\":\"Notification\"}";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(invalidJson))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON must start with '{'");
    }

    @Test
    void parseMessage_doesNotEndWithBrace_throwsException() {
        String invalidJson = "{\"Type\":\"Notification\"}invalid";

        assertThatThrownBy(() -> SnsMessageParser.parseMessage(invalidJson))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message JSON must end with '}'");
    }
}