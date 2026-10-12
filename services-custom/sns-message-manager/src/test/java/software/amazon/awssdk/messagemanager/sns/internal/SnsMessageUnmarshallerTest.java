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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.utils.BinaryUtils;

public class SnsMessageUnmarshallerTest {
    private static final SnsMessageUnmarshaller UNMARSHALLER = new SnsMessageUnmarshaller();
    private static final byte[] TEST_SIGNATURE = BinaryUtils.fromBase64("k764G/ur2Ng=");
    private static final SignatureVersion TEST_SIGNATURE_VERSION = SignatureVersion.VERSION_1;
    private static final String TEST_TOPIC_ARN = "arn:aws:sns:us-west-2:123456789012:MyTopic";
    private static final String TEST_MESSAGE_ID = "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324";
    private static final String TEST_MESSAGE = "Hello world!";
    private static final String TEST_SUBJECT = "My First Message";
    private static final URI TEST_SIGNING_CERT_URL = URI.create("https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem");
    private static final URI TEST_SUBSCRIBE_URL = URI.create("https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token");
    private static final URI TEST_UNSUBSCRIBE_URL = URI.create("https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96");
    private static final Instant TEST_TIMESTAMP = Instant.parse("2012-05-02T00:54:06.655Z");
    private static final String TEST_TOKEN = "token";

    @Test
    void unmarshall_notification_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"Notification\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Subject\" : \"My First Message\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\",\n"
                      + "  \"UnsubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsNotification.class);

        SnsNotification notification = (SnsNotification) msg;

        assertThat(notification.type()).isEqualTo(SnsMessageType.NOTIFICATION);
        assertThat(notification.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(notification.message()).isEqualTo(TEST_MESSAGE);
        assertThat(notification.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(notification.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(notification.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(notification.subject()).isEqualTo(TEST_SUBJECT);
        assertThat(notification.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(notification.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
        assertThat(notification.unsubscribeUrl()).isEqualTo(TEST_UNSUBSCRIBE_URL);
    }

    @Test
    void unmarshall_subscriptionNotification_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"SubscriptionConfirmation\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"Token\" : \"token\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"SubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsSubscriptionConfirmation.class);

        SnsSubscriptionConfirmation subscriptionConfirmation = (SnsSubscriptionConfirmation) msg;

        assertThat(subscriptionConfirmation.type()).isEqualTo(SnsMessageType.SUBSCRIPTION_CONFIRMATION);
        assertThat(subscriptionConfirmation.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(subscriptionConfirmation.token()).isEqualTo(TEST_TOKEN);
        assertThat(subscriptionConfirmation.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(subscriptionConfirmation.message()).isEqualTo(TEST_MESSAGE);
        assertThat(subscriptionConfirmation.subscribeUrl()).isEqualTo(TEST_SUBSCRIBE_URL);
        assertThat(subscriptionConfirmation.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(subscriptionConfirmation.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(subscriptionConfirmation.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(subscriptionConfirmation.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
    }

    @Test
    void unmarshall_unsubscribeConfirmation_maximal() {
        String json = "{\n"
                      + "  \"Type\" : \"UnsubscribeConfirmation\",\n"
                      + "  \"MessageId\" : \"22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324\",\n"
                      + "  \"Token\" : \"token\",\n"
                      + "  \"TopicArn\" : \"arn:aws:sns:us-west-2:123456789012:MyTopic\",\n"
                      + "  \"Message\" : \"Hello world!\",\n"
                      + "  \"SubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:123456789012:MyTopic&Token=token\",\n"
                      + "  \"Timestamp\" : \"2012-05-02T00:54:06.655Z\",\n"
                      + "  \"SignatureVersion\" : \"1\",\n"
                      + "  \"Signature\" : \"k764G/ur2Ng=\",\n"
                      + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem\"\n"
                      + "}";

        SnsMessage msg = UNMARSHALLER.unmarshall(asStream(json));

        assertThat(msg).isInstanceOf(SnsUnsubscribeConfirmation.class);

        SnsUnsubscribeConfirmation unsubscribeConfirmation = (SnsUnsubscribeConfirmation) msg;

        assertThat(unsubscribeConfirmation.type()).isEqualTo(SnsMessageType.UNSUBSCRIBE_CONFIRMATION);
        assertThat(unsubscribeConfirmation.messageId()).isEqualTo(TEST_MESSAGE_ID);
        assertThat(unsubscribeConfirmation.token()).isEqualTo(TEST_TOKEN);
        assertThat(unsubscribeConfirmation.topicArn()).isEqualTo(TEST_TOPIC_ARN);
        assertThat(unsubscribeConfirmation.message()).isEqualTo(TEST_MESSAGE);
        assertThat(unsubscribeConfirmation.subscribeUrl()).isEqualTo(TEST_SUBSCRIBE_URL);
        assertThat(unsubscribeConfirmation.timestamp()).isEqualTo(TEST_TIMESTAMP);
        assertThat(unsubscribeConfirmation.signatureVersion()).isEqualTo(TEST_SIGNATURE_VERSION);
        assertThat(unsubscribeConfirmation.signature().asByteArray()).isEqualTo(TEST_SIGNATURE);
        assertThat(unsubscribeConfirmation.signingCertUrl()).isEqualTo(TEST_SIGNING_CERT_URL);
    }

    @Test
    void unmarshall_unknownType_throws() {
        String json = "{\n"
                      + "  \"Type\" : \"MyCustomType\"\n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unsupported sns message type");
    }

    @Test
    void unmarshall_jsonNotObject_throws() {
        String json = "[]";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected an JSON object");
    }

    @Test
    void unmarshall_signatureNotDecodable_throws() {
        String json = "{\n"
                      + "  \"Type\" : \"SubscriptionConfirmation\",\n"
                      + "  \"Signature\" : \"this is not base64\"\n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to decode signature");
    }

    @Test
    void unmarshall_timestampNotParsable_throws() {
        String json = "{\n"
                      + "  \"Type\" : \"SubscriptionConfirmation\",\n"
                      + "  \"MessageId\" : \"165545c9-2a5c-472c-8df2-7ff2be2b3b1b\",\n"
                      + "  \"Timestamp\" : \"right now\"\n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to parse timestamp");
    }

    @Test
    void unmarshall_uriNotParsable_throws() {
        String json = "{\n"
                      + "  \"Type\" : \"SubscriptionConfirmation\",\n"
                      + "  \"SigningCertURL\" : \"https:// not a uri\"\n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to parse URI");
    }

    @Test
    void unmarshall_valueNotString_throws() {
        String json = "{\n"
                      + "  \"Type\" : [] \n"
                      + "}";

        assertThatThrownBy(() -> UNMARSHALLER.unmarshall(asStream(json)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected field 'Type' to be a string");
    }

    private static InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

}
