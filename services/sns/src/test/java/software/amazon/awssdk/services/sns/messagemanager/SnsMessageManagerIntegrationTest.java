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

package software.amazon.awssdk.services.sns.messagemanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for SnsMessageManager that verify the complete workflow
 * from public API through all internal components.
 */
class SnsMessageManagerIntegrationTest {

    private SnsMessageManager messageManager;

    @BeforeEach
    void setUp() {
        messageManager = SnsMessageManager.builder().build();
    }

    @AfterEach
    void tearDown() {
        if (messageManager != null) {
            messageManager.close();
        }
    }

    @Test
    void builder_withDefaultConfiguration_createsManagerSuccessfully() {
        try (SnsMessageManager manager = SnsMessageManager.builder().build()) {
            assertThat(manager).isNotNull();
        }
    }

    @Test
    void builder_withCustomConfiguration_createsManagerSuccessfully() {
        MessageManagerConfiguration config = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(Duration.ofMinutes(10))
            .build();

        try (SnsMessageManager manager = SnsMessageManager.builder()
            .configuration(config)
            .build()) {
            assertThat(manager).isNotNull();
        }
    }

    @Test
    void builder_withConsumerConfiguration_createsManagerSuccessfully() {
        try (SnsMessageManager manager = SnsMessageManager.builder()
            .configuration(config -> config.certificateCacheTimeout(Duration.ofMinutes(15)))
            .build()) {
            assertThat(manager).isNotNull();
        }
    }

    @Test
    void parseMessage_withNullString_throwsException() {
        assertThatThrownBy(() -> messageManager.parseMessage((String) null))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message content cannot be null");
    }

    @Test
    void parseMessage_withNullInputStream_throwsException() {
        assertThatThrownBy(() -> messageManager.parseMessage((ByteArrayInputStream) null))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message InputStream cannot be null");
    }

    @Test
    void parseMessage_withEmptyString_throwsException() {
        assertThatThrownBy(() -> messageManager.parseMessage(""))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message content cannot be empty");
    }

    @Test
    void parseMessage_withWhitespaceOnlyString_throwsException() {
        assertThatThrownBy(() -> messageManager.parseMessage("   \n\t  "))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message content cannot be empty");
    }

    @Test
    void parseMessage_withInvalidJson_throwsParsingException() {
        String invalidJson = "{ invalid json }";
        
        assertThatThrownBy(() -> messageManager.parseMessage(invalidJson))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Failed to parse JSON message");
    }

    @Test
    void parseMessage_withNonJsonString_throwsParsingException() {
        String nonJson = "This is not JSON";
        
        assertThatThrownBy(() -> messageManager.parseMessage(nonJson))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message content does not appear to be valid JSON");
    }

    @Test
    void parseMessage_withValidJsonButMissingRequiredFields_throwsParsingException() {
        String incompleteMessage = "{\"Type\": \"Notification\"}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(incompleteMessage))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Missing required fields");
    }

    @Test
    void parseMessage_withUnsupportedMessageType_throwsParsingException() {
        String messageWithInvalidType = "{"
            + "\"Type\": \"InvalidType\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/test.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithInvalidType))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Unsupported message type: InvalidType");
    }

    @Test
    void parseMessage_withInvalidCertificateUrl_throwsParsingException() {
        String messageWithInvalidCertUrl = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"http://malicious-site.com/fake-cert.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithInvalidCertUrl))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("SigningCertURL must use HTTPS protocol for security");
    }

    @Test
    void parseMessage_withHttpCertificateUrl_throwsParsingException() {
        String messageWithHttpCertUrl = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"http://sns.us-east-1.amazonaws.com/test.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithHttpCertUrl))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("SigningCertURL must use HTTPS protocol for security");
    }

    @Test
    void parseMessage_withValidUrlButNetworkFailure_throwsCertificateException() {
        // This test uses a valid HTTPS SNS URL that will pass parsing but fail during certificate retrieval
        String messageWithValidButUnreachableUrl = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/nonexistent-cert.pem\""
            + "}";
        
        // This should pass parsing but fail during certificate retrieval
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithValidButUnreachableUrl))
            .isInstanceOf(SnsCertificateException.class)
            .hasMessageContaining("Failed to retrieve certificate");
    }

    @Test
    void parseMessage_withInputStream_handlesParsingCorrectly() {
        String invalidJson = "{ invalid json }";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
        
        assertThatThrownBy(() -> messageManager.parseMessage(inputStream))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Failed to parse JSON message");
    }

    @Test
    void parseMessage_withEmptyInputStream_throwsException() {
        ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        
        assertThatThrownBy(() -> messageManager.parseMessage(emptyStream))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("InputStream is empty");
    }

    @Test
    void parseMessage_withLargeMessage_throwsException() {
        // Create a message larger than 256KB
        StringBuilder largeMessage = new StringBuilder("{\"Type\": \"Notification\",");
        largeMessage.append("\"Message\": \"");
        for (int i = 0; i < 300 * 1024; i++) { // 300KB of 'a' characters
            largeMessage.append("a");
        }
        largeMessage.append("\"}");
        
        assertThatThrownBy(() -> messageManager.parseMessage(largeMessage.toString()))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message content is too large");
    }

    @Test
    void parseMessage_withInvalidTopicArn_throwsParsingException() {
        String messageWithInvalidArn = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"invalid-arn\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/test.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithInvalidArn))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("TopicArn must be a valid ARN starting with 'arn:'");
    }

    @Test
    void parseMessage_withInvalidSignatureVersion_throwsParsingException() {
        String messageWithInvalidSigVersion = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"3\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/test.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithInvalidSigVersion))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("SignatureVersion must be '1' or '2'");
    }

    @Test
    void parseMessage_withInvalidTimestamp_throwsParsingException() {
        String messageWithInvalidTimestamp = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"invalid-timestamp\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/test.pem\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithInvalidTimestamp))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Invalid timestamp format");
    }

    @Test
    void parseMessage_withUnexpectedFields_throwsParsingException() {
        String messageWithUnexpectedField = "{"
            + "\"Type\": \"Notification\","
            + "\"MessageId\": \"test-id\","
            + "\"TopicArn\": \"arn:aws:sns:us-east-1:123456789012:test-topic\","
            + "\"Message\": \"test message\","
            + "\"Timestamp\": \"2023-01-01T00:00:00.000Z\","
            + "\"SignatureVersion\": \"1\","
            + "\"Signature\": \"test-signature\","
            + "\"SigningCertURL\": \"https://sns.us-east-1.amazonaws.com/test.pem\","
            + "\"UnexpectedField\": \"should not be here\""
            + "}";
        
        assertThatThrownBy(() -> messageManager.parseMessage(messageWithUnexpectedField))
            .isInstanceOf(SnsMessageParsingException.class)
            .hasMessageContaining("Message contains unexpected fields");
    }

    @Test
    void close_withDefaultHttpClient_closesSuccessfully() {
        SnsMessageManager manager = SnsMessageManager.builder().build();
        
        // Should not throw any exception
        manager.close();
    }

    @Test
    void close_multipleCallsToClose_handlesGracefully() {
        SnsMessageManager manager = SnsMessageManager.builder().build();
        
        // Multiple calls to close should not throw exceptions
        manager.close();
        manager.close();
        manager.close();
    }

    @Test
    void messageManagerConfiguration_builderPattern_worksCorrectly() {
        Duration customTimeout = Duration.ofHours(2);
        
        MessageManagerConfiguration config = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(customTimeout)
            .build();
        
        assertThat(config.certificateCacheTimeout()).isEqualTo(customTimeout);
        assertThat(config.httpClient()).isNull(); // Default should be null
    }

    @Test
    void messageManagerConfiguration_toBuilder_preservesValues() {
        Duration originalTimeout = Duration.ofMinutes(30);
        
        MessageManagerConfiguration original = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(originalTimeout)
            .build();
        
        MessageManagerConfiguration copy = original.toBuilder()
            .certificateCacheTimeout(Duration.ofHours(1))
            .build();
        
        assertThat(original.certificateCacheTimeout()).isEqualTo(originalTimeout);
        assertThat(copy.certificateCacheTimeout()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void messageManagerConfiguration_equalsAndHashCode_workCorrectly() {
        Duration timeout = Duration.ofMinutes(10);
        
        MessageManagerConfiguration config1 = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(timeout)
            .build();
        
        MessageManagerConfiguration config2 = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(timeout)
            .build();
        
        MessageManagerConfiguration config3 = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(Duration.ofMinutes(20))
            .build();
        
        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        assertThat(config1.hashCode()).isNotEqualTo(config3.hashCode());
    }

    @Test
    void messageManagerConfiguration_toString_containsExpectedFields() {
        Duration timeout = Duration.ofMinutes(5);
        
        MessageManagerConfiguration config = MessageManagerConfiguration.builder()
            .certificateCacheTimeout(timeout)
            .build();
        
        String toString = config.toString();
        assertThat(toString).contains("MessageManagerConfiguration");
        assertThat(toString).contains("certificateCacheTimeout");
    }
}