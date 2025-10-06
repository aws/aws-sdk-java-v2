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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessage;
import software.amazon.awssdk.services.sns.messagemanager.SnsMessageParsingException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal parser for SNS message JSON payloads.
 */
@SdkInternalApi
public final class SnsMessageParser {

    private static final JsonNodeParser JSON_PARSER = JsonNodeParser.create();

    // Supported message types
    private static final String TYPE_NOTIFICATION = "Notification";
    private static final String TYPE_SUBSCRIPTION_CONFIRMATION = "SubscriptionConfirmation";
    private static final String TYPE_UNSUBSCRIBE_CONFIRMATION = "UnsubscribeConfirmation";

    // Required fields for all message types
    private static final Set<String> COMMON_REQUIRED_FIELDS = createSet(
        "Type", "MessageId", "TopicArn", "Timestamp", "SignatureVersion", "Signature", "SigningCertURL"
    );

    // Required fields specific to Notification messages
    private static final Set<String> NOTIFICATION_REQUIRED_FIELDS = createSet("Message");

    // Required fields specific to confirmation messages
    private static final Set<String> CONFIRMATION_REQUIRED_FIELDS = createSet("Message", "Token");

    // All valid fields that can appear in SNS messages
    private static final Set<String> VALID_FIELDS = createSet(
        "Type", "MessageId", "TopicArn", "Subject", "Message", "Timestamp",
        "SignatureVersion", "Signature", "SigningCertURL", "UnsubscribeURL", "Token", "MessageAttributes"
    );

    private SnsMessageParser() {
        // Utility class - prevent instantiation
    }

    private static Set<String> createSet(String... elements) {
        Set<String> set = new HashSet<>();
        for (String element : elements) {
            set.add(element);
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Parses an SNS message from JSON string with comprehensive validation and error reporting.
     *
     * @param messageJson The JSON string to parse.
     * @return The parsed SNS message.
     * @throws SnsMessageParsingException If parsing or validation fails.
     */
    public static SnsMessage parseMessage(String messageJson) {
        // Enhanced input validation
        validateMessageJsonInput(messageJson);

        try {
            JsonNode rootNode = JSON_PARSER.parse(messageJson);
            return parseMessageFromJsonNode(rootNode);
        } catch (SnsMessageParsingException e) {
            // Re-throw SNS parsing exceptions as-is
            throw e;
        } catch (Exception e) {
            // Provide more specific error messages for JSON parsing failures
            String errorMessage = "Failed to parse JSON message";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Unexpected character")) {
                    errorMessage += ". The message contains invalid JSON syntax. " +
                                  "Please ensure the message is properly formatted JSON from Amazon SNS.";
                } else if (e.getMessage().contains("Unexpected end-of-input")) {
                    errorMessage += ". The JSON message appears to be truncated or incomplete. " +
                                  "Please ensure the complete message was received.";
                } else {
                    errorMessage += ". " + e.getMessage();
                }
            }
            
            throw SnsMessageParsingException.builder()
                .message(errorMessage + " Raw error: " + e.getMessage())
                .cause(e)
                .build();
        }
    }

    /**
     * Validates the input JSON string with comprehensive error reporting.
     *
     * @param messageJson The JSON string to validate.
     * @throws SnsMessageParsingException If validation fails.
     */
    private static void validateMessageJsonInput(String messageJson) {
        Validate.paramNotNull(messageJson, "messageJson");

        if (StringUtils.isBlank(messageJson)) {
            throw SnsMessageParsingException.builder()
                .message("Message JSON cannot be empty or blank. Please provide a valid SNS message JSON string.")
                .build();
        }

        // Check for reasonable size limits
        if (messageJson.length() > 256 * 1024) { // 256KB
            throw SnsMessageParsingException.builder()
                .message("Message JSON is too large (" + messageJson.length() + " characters). " +
                        "SNS messages should typically be under 256KB.")
                .build();
        }

        // Basic JSON format validation
        String trimmed = messageJson.trim();
        if (!trimmed.startsWith("{")) {
            throw SnsMessageParsingException.builder()
                .message("Message JSON must start with '{'. Received content starts with: " + 
                        getMessagePreview(trimmed))
                .build();
        }

        if (!trimmed.endsWith("}")) {
            throw SnsMessageParsingException.builder()
                .message("Message JSON must end with '}'. Received content ends with: " + 
                        getMessageSuffix(trimmed))
                .build();
        }

        // Check for common JSON issues
        if (hasUnbalancedBraces(trimmed)) {
            throw SnsMessageParsingException.builder()
                .message("Message JSON appears to have unbalanced braces. Please ensure the JSON is properly formatted.")
                .build();
        }
    }

    /**
     * Gets a preview of the message content for error reporting.
     */
    private static String getMessagePreview(String content) {
        if (content.length() <= 50) {
            return "'" + content + "'";
        }
        return "'" + content.substring(0, 50) + "...'";
    }

    /**
     * Gets the suffix of the message content for error reporting.
     */
    private static String getMessageSuffix(String content) {
        if (content.length() <= 50) {
            return "'" + content + "'";
        }
        return "'..." + content.substring(content.length() - 50) + "'";
    }

    /**
     * Performs a basic check for unbalanced braces.
     */
    private static boolean hasUnbalancedBraces(String content) {
        int braceCount = 0;
        for (char c : content.toCharArray()) {
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount < 0) {
                    return true; // More closing braces than opening
                }
            }
        }
        return braceCount != 0; // Should be balanced
    }

    private static SnsMessage parseMessageFromJsonNode(JsonNode rootNode) {
        validateJsonStructure(rootNode);

        String messageType = extractRequiredStringField(rootNode, "Type");
        validateMessageType(messageType);
        validateRequiredFields(rootNode, messageType);
        validateNoUnexpectedFields(rootNode);

        SnsMessage.Builder messageBuilder = SnsMessage.builder()
            .type(messageType)
            .messageId(extractRequiredStringField(rootNode, "MessageId"))
            .topicArn(extractRequiredStringField(rootNode, "TopicArn"))
            .message(extractRequiredStringField(rootNode, "Message"))
            .timestamp(parseTimestamp(extractRequiredStringField(rootNode, "Timestamp")))
            .signatureVersion(extractRequiredStringField(rootNode, "SignatureVersion"))
            .signature(extractRequiredStringField(rootNode, "Signature"))
            .signingCertUrl(extractRequiredStringField(rootNode, "SigningCertURL"));

        // Optional fields
        if (rootNode.field("Subject").isPresent()) {
            messageBuilder.subject(extractStringField(rootNode, "Subject"));
        }

        if (rootNode.field("UnsubscribeURL").isPresent()) {
            messageBuilder.unsubscribeUrl(extractStringField(rootNode, "UnsubscribeURL"));
        }

        if (rootNode.field("Token").isPresent()) {
            messageBuilder.token(extractStringField(rootNode, "Token"));
        }

        if (rootNode.field("MessageAttributes").isPresent()) {
            messageBuilder.messageAttributes(parseMessageAttributes(rootNode.field("MessageAttributes").get()));
        }

        return messageBuilder.build();
    }

    private static void validateJsonStructure(JsonNode rootNode) {
        if (!rootNode.isObject()) {
            throw SnsMessageParsingException.builder()
                .message("Message must be a JSON object")
                .build();
        }

        if (rootNode.asObject().isEmpty()) {
            throw SnsMessageParsingException.builder()
                .message("Message cannot be empty")
                .build();
        }
    }

    private static void validateMessageType(String messageType) {
        if (!TYPE_NOTIFICATION.equals(messageType) &&
            !TYPE_SUBSCRIPTION_CONFIRMATION.equals(messageType) &&
            !TYPE_UNSUBSCRIBE_CONFIRMATION.equals(messageType)) {
            throw SnsMessageParsingException.builder()
                .message("Unsupported message type: " + messageType + ". Supported types are: " +
                        TYPE_NOTIFICATION + ", " + TYPE_SUBSCRIPTION_CONFIRMATION + ", " + TYPE_UNSUBSCRIBE_CONFIRMATION)
                .build();
        }
    }

    private static void validateRequiredFields(JsonNode rootNode, String messageType) {
        Set<String> missingFields = new HashSet<>();
        Map<String, JsonNode> fields = rootNode.asObject();
        
        for (String field : COMMON_REQUIRED_FIELDS) {
            if (!fields.containsKey(field) || fields.get(field).isNull()) {
                missingFields.add(field);
            }
        }

        // Check type-specific required fields
        Set<String> typeSpecificFields = getTypeSpecificRequiredFields(messageType);
        for (String field : typeSpecificFields) {
            if (!fields.containsKey(field) || fields.get(field).isNull()) {
                missingFields.add(field);
            }
        }

        if (!missingFields.isEmpty()) {
            throw SnsMessageParsingException.builder()
                .message("Missing required fields for message type '" + messageType + "': " + missingFields)
                .build();
        }
    }

    private static Set<String> getTypeSpecificRequiredFields(String messageType) {
        switch (messageType) {
            case TYPE_NOTIFICATION:
                return NOTIFICATION_REQUIRED_FIELDS;
            case TYPE_SUBSCRIPTION_CONFIRMATION:
            case TYPE_UNSUBSCRIBE_CONFIRMATION:
                return CONFIRMATION_REQUIRED_FIELDS;
            default:
                return Collections.emptySet();
        }
    }

    private static void validateNoUnexpectedFields(JsonNode rootNode) {
        Set<String> unexpectedFields = new HashSet<>();
        Map<String, JsonNode> fields = rootNode.asObject();
        
        for (String fieldName : fields.keySet()) {
            if (!VALID_FIELDS.contains(fieldName)) {
                unexpectedFields.add(fieldName);
            }
        }

        if (!unexpectedFields.isEmpty()) {
            throw SnsMessageParsingException.builder()
                .message("Message contains unexpected fields: " + unexpectedFields + 
                        ". Valid fields are: " + VALID_FIELDS)
                .build();
        }
    }

    private static String extractRequiredStringField(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.field(fieldName).orElse(null);
        if (fieldNode == null || fieldNode.isNull()) {
            throw SnsMessageParsingException.builder()
                .message("Required field '" + fieldName + "' is missing or null. " +
                        "This field is mandatory for all SNS messages. Please ensure the message " +
                        "is a valid SNS message from Amazon.")
                .build();
        }

        if (!fieldNode.isString()) {
            String actualType = getJsonNodeTypeName(fieldNode);
            throw SnsMessageParsingException.builder()
                .message("Field '" + fieldName + "' must be a string but found " + actualType + ". " +
                        "SNS message fields should be string values. Received value: " + 
                        getFieldValuePreview(fieldNode))
                .build();
        }

        String value = fieldNode.asString();
        if (StringUtils.isBlank(value)) {
            throw SnsMessageParsingException.builder()
                .message("Required field '" + fieldName + "' cannot be empty or blank. " +
                        "This field must contain a valid value for SNS message processing.")
                .build();
        }

        // Additional field-specific validation
        validateFieldContent(fieldName, value);

        return value;
    }

    private static String extractStringField(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.field(fieldName).orElse(null);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }

        if (!fieldNode.isString()) {
            String actualType = getJsonNodeTypeName(fieldNode);
            throw SnsMessageParsingException.builder()
                .message("Field '" + fieldName + "' must be a string but found " + actualType + ". " +
                        "Received value: " + getFieldValuePreview(fieldNode))
                .build();
        }

        String value = fieldNode.asString();
        
        // Additional field-specific validation for optional fields
        if (!StringUtils.isBlank(value)) {
            validateFieldContent(fieldName, value);
        }

        return value;
    }

    /**
     * Gets a human-readable name for the JSON node type.
     */
    private static String getJsonNodeTypeName(JsonNode node) {
        if (node.isNumber()) {
            return "number";
        } else if (node.isBoolean()) {
            return "boolean";
        } else if (node.isArray()) {
            return "array";
        } else if (node.isObject()) {
            return "object";
        } else {
            return "unknown type";
        }
    }

    /**
     * Gets a preview of the field value for error reporting.
     */
    private static String getFieldValuePreview(JsonNode node) {
        String value = node.toString();
        if (value.length() > 100) {
            return value.substring(0, 100) + "...";
        }
        return value;
    }

    /**
     * Validates field content based on field-specific rules.
     */
    private static void validateFieldContent(String fieldName, String value) {
        switch (fieldName) {
            case "Type":
                // Already validated in validateMessageType
                break;
            case "MessageId":
                if (value.length() > 100) {
                    throw SnsMessageParsingException.builder()
                        .message("MessageId is too long (" + value.length() + " characters). " +
                                "SNS MessageIds should be reasonable length identifiers.")
                        .build();
                }
                break;
            case "TopicArn":
                if (!value.startsWith("arn:")) {
                    throw SnsMessageParsingException.builder()
                        .message("TopicArn must be a valid ARN starting with 'arn:'. " +
                                "Received: " + (value.length() > 50 ? value.substring(0, 50) + "..." : value))
                        .build();
                }
                if (!value.contains(":sns:")) {
                    throw SnsMessageParsingException.builder()
                        .message("TopicArn must be an SNS topic ARN containing ':sns:'. " +
                                "Received: " + (value.length() > 50 ? value.substring(0, 50) + "..." : value))
                        .build();
                }
                break;
            case "SigningCertURL":
                if (!value.startsWith("https://")) {
                    throw SnsMessageParsingException.builder()
                        .message("SigningCertURL must use HTTPS protocol for security. " +
                                "Received URL: " + (value.length() > 100 ? value.substring(0, 100) + "..." : value))
                        .build();
                }
                break;
            case "UnsubscribeURL":
                if (!value.startsWith("https://")) {
                    throw SnsMessageParsingException.builder()
                        .message("UnsubscribeURL must use HTTPS protocol for security. " +
                                "Received URL: " + (value.length() > 100 ? value.substring(0, 100) + "..." : value))
                        .build();
                }
                break;
            case "SignatureVersion":
                if (!"1".equals(value) && !"2".equals(value)) {
                    throw SnsMessageParsingException.builder()
                        .message("SignatureVersion must be '1' or '2'. Received: '" + value + "'")
                        .build();
                }
                break;
            default:
                // No specific validation for other fields
                break;
        }
    }

    private static Instant parseTimestamp(String timestampStr) {
        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            throw SnsMessageParsingException.builder()
                .message("Invalid timestamp format: " + timestampStr + ". Expected ISO-8601 format.")
                .cause(e)
                .build();
        }
    }

    private static Map<String, String> parseMessageAttributes(JsonNode messageAttributesNode) {
        if (messageAttributesNode.isNull()) {
            return Collections.emptyMap();
        }

        if (!messageAttributesNode.isObject()) {
            throw SnsMessageParsingException.builder()
                .message("MessageAttributes must be a JSON object")
                .build();
        }

        Map<String, String> attributes = new HashMap<>();
        Map<String, JsonNode> fields = messageAttributesNode.asObject();

        for (Map.Entry<String, JsonNode> entry : fields.entrySet()) {
            String key = entry.getKey();
            JsonNode valueNode = entry.getValue();

            if (valueNode.isNull()) {
                continue; // Skip null values
            }

            if (!valueNode.isString()) {
                throw SnsMessageParsingException.builder()
                    .message("MessageAttribute value for key '" + key + "' must be a string")
                    .build();
            }

            attributes.put(key, valueNode.asString());
        }

        return attributes;
    }
}