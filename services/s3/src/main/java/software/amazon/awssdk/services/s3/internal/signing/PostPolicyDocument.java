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

package software.amazon.awssdk.services.s3.internal.signing;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions.ContentLengthRange;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions.Eq;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions.PolicyCondition;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions.StartsWith;

/**
 * Immutable POST policy document model and compact JSON serialisation.
 */
@SdkInternalApi
public final class PostPolicyDocument {
    public static final Set<String> RESERVED_FIELD_NAMES;

    static {
        Set<String> names = new HashSet<>();
        names.add("bucket");
        names.add("policy");
        names.add("x-amz-algorithm");
        names.add("x-amz-credential");
        names.add("x-amz-date");
        names.add("x-amz-signature");
        names.add("x-amz-security-token");
        RESERVED_FIELD_NAMES = Collections.unmodifiableSet(names);
    }

    private static final String FILENAME_VARIABLE = "${filename}";
    private static final DateTimeFormatter EXPIRATION_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final String json;

    private PostPolicyDocument(String json) {
        this.json = json;
    }

    /**
     * Assembles a policy document in the order required by this SDK. User field names are validated against
     * {@link #RESERVED_FIELD_NAMES}.
     */
    public static PostPolicyDocument from(String bucket,
                                          String objectKey,
                                          Instant expiration,
                                          PostPolicyConditions userConditions,
                                          Map<String, String> userFields,
                                          String xAmzCredential,
                                          String xAmzDate,
                                          String sessionToken) {
        validateUserFields(userFields);

        List<PolicyCondition> userConditionList =
            userConditions == null ? Collections.emptyList() : userConditions.conditions();

        boolean userSuppliedKeyCondition = userConditionList.stream().anyMatch(PostPolicyDocument::targetsObjectKey);

        List<Object> serializedConditions = new ArrayList<>();
        serializedConditions.add(bucketCondition(bucket));
        if (!userSuppliedKeyCondition) {
            serializedConditions.add(keyCondition(objectKey));
        }
        serializedConditions.addAll(userConditionList);
        serializedConditions.add(eqSingleton("x-amz-credential", xAmzCredential));
        serializedConditions.add(eqSingleton("x-amz-algorithm", "AWS4-HMAC-SHA256"));
        serializedConditions.add(eqSingleton("x-amz-date", xAmzDate));
        if (sessionToken != null) {
            serializedConditions.add(eqSingleton("x-amz-security-token", sessionToken));
        }

        String expirationString = EXPIRATION_FORMATTER.format(expiration);
        StringBuilder json = new StringBuilder();
        json.append("{\"expiration\":\"").append(escapeJsonString(expirationString)).append("\",\"conditions\":[");
        for (int i = 0; i < serializedConditions.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            Object element = serializedConditions.get(i);
            if (element instanceof PolicyCondition) {
                appendConditionJson(json, (PolicyCondition) element);
            } else if (element instanceof EqLiteral) {
                EqLiteral literal = (EqLiteral) element;
                appendEqObject(json, literal.key, literal.value);
            }
        }
        json.append("]}");
        return new PostPolicyDocument(json.toString());
    }

    private static boolean targetsObjectKey(PolicyCondition condition) {
        if (condition instanceof Eq) {
            return "key".equals(((Eq) condition).field());
        }
        if (condition instanceof StartsWith) {
            String field = ((StartsWith) condition).field();
            return "$key".equals(field);
        }
        return false;
    }

    private static EqLiteral bucketCondition(String bucket) {
        return new EqLiteral("bucket", bucket);
    }

    private static Object keyCondition(String objectKey) {
        int filenameIdx = objectKey.indexOf(FILENAME_VARIABLE);
        if (filenameIdx >= 0) {
            String prefix = objectKey.substring(0, filenameIdx);
            return PostPolicyConditions.builder().startsWith("$key", prefix).build().conditions().get(0);
        }
        return eqSingleton("key", objectKey);
    }

    private static EqLiteral eqSingleton(String key, String value) {
        return new EqLiteral(key, value);
    }

    private static void validateUserFields(Map<String, String> userFields) {
        for (String name : userFields.keySet()) {
            if (isReservedFieldName(name)) {
                throw new IllegalArgumentException("Form field '" + name + "' uses a reserved name. Reserved names are "
                                                   + RESERVED_FIELD_NAMES + ".");
            }
        }
    }

    public static boolean isReservedFieldName(String name) {
        return RESERVED_FIELD_NAMES.contains(name.toLowerCase(Locale.US));
    }

    private static void appendConditionJson(StringBuilder json, PolicyCondition condition) {
        if (condition instanceof ContentLengthRange) {
            ContentLengthRange range = (ContentLengthRange) condition;
            json.append("[\"content-length-range\",")
                .append(range.min())
                .append(',')
                .append(range.max())
                .append(']');
        } else if (condition instanceof StartsWith) {
            StartsWith startsWith = (StartsWith) condition;
            json.append("[\"starts-with\",\"")
                .append(escapeJsonString(startsWith.field()))
                .append("\",\"")
                .append(escapeJsonString(startsWith.prefix()))
                .append("\"]");
        } else if (condition instanceof Eq) {
            Eq eq = (Eq) condition;
            appendEqObject(json, eq.field(), eq.value());
        } else {
            throw new IllegalStateException("Unknown condition type: " + condition.getClass());
        }
    }

    private static void appendEqObject(StringBuilder json, String key, String value) {
        json.append("{\"")
            .append(escapeJsonString(key))
            .append("\":\"")
            .append(escapeJsonString(value))
            .append("\"}");
    }

    private static String escapeJsonString(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '"':
                    result.append("\\\"");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        return result.toString();
    }

    public String toJson() {
        return json;
    }

    private static final class EqLiteral {
        private final String key;
        private final String value;

        private EqLiteral(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
