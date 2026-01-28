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

package software.amazon.awssdk.protocols.json.internal.marshall;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.core.ProtocolUtils;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Experimental implementation of {@link ProtocolMarshaller} that uses direct StringBuilder
 * instead of Jackson for JSON generation.
 */
@SdkInternalApi
public class DirectJsonProtocolMarshaller implements ProtocolMarshaller<SdkHttpFullRequest> {

    private final URI endpoint;
    private final SdkHttpFullRequest.Builder request;
    private final String contentType;
    private final AwsJsonProtocolMetadata protocolMetadata;
    private final boolean hasExplicitPayloadMember;
    private final boolean hasImplicitPayloadMembers;
    private final boolean hasStreamingInput;
    private final boolean hasEvent;
    private final boolean hasAwsQueryCompatible;

    public DirectJsonProtocolMarshaller(URI endpoint,
                                        String contentType,
                                        OperationInfo operationInfo,
                                        AwsJsonProtocolMetadata protocolMetadata,
                                        boolean hasAwsQueryCompatible) {
        this.endpoint = endpoint;
        this.contentType = contentType;
        this.protocolMetadata = protocolMetadata;
        this.hasExplicitPayloadMember = operationInfo.hasExplicitPayloadMember();
        this.hasImplicitPayloadMembers = operationInfo.hasImplicitPayloadMembers();
        this.hasStreamingInput = operationInfo.hasStreamingInput();
        this.hasEvent = operationInfo.hasEvent();
        this.request = fillBasicRequestParams(operationInfo);
        this.hasAwsQueryCompatible = hasAwsQueryCompatible;
    }

    private SdkHttpFullRequest.Builder fillBasicRequestParams(OperationInfo operationInfo) {
        SdkHttpFullRequest.Builder requestBuilder = ProtocolUtils.createSdkHttpRequest(operationInfo, endpoint);
        String operationIdentifier = operationInfo.operationIdentifier();
        if (operationIdentifier != null) {
            requestBuilder.putHeader("X-Amz-Target", operationIdentifier);
        }
        return requestBuilder;
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        StringBuilder jsonBuilder = new StringBuilder();
        if (needTopLevelJsonObject()) {
            jsonBuilder.append('{');
        }
        doMarshall(pojo, jsonBuilder);
        return finishMarshalling(jsonBuilder);
    }

    void doMarshall(SdkPojo pojo, StringBuilder jsonBuilder) {
        boolean needsComma = false;
        for (SdkField<?> field : pojo.sdkFields()) {
            Object val = field.getValueOrDefault(pojo);
            if (isExplicitBinaryPayload(field)) {
                if (val != null) {
                    SdkBytes sdkBytes = (SdkBytes) val;
                    request.contentStreamProvider(sdkBytes::asInputStream);
                    updateContentLengthHeader(sdkBytes.asByteArrayUnsafe().length);
                }
            } else if (isExplicitStringPayload(field)) {
                if (val != null) {
                    byte[] content = ((String) val).getBytes(StandardCharsets.UTF_8);
                    request.contentStreamProvider(() -> new ByteArrayInputStream(content));
                    updateContentLengthHeader(content.length);
                }
            } else if (isExplicitPayloadMember(field)) {
                marshallExplicitJsonPayload(field, val, jsonBuilder);
            } else {
                needsComma = marshallField(field, val, jsonBuilder, needsComma);
            }
        }
    }

    private void updateContentLengthHeader(int contentLength) {
        request.putHeader("Content-Length", Integer.toString(contentLength));
    }

    private boolean isExplicitBinaryPayload(SdkField<?> field) {
        return isExplicitPayloadMember(field) && MarshallingType.SDK_BYTES.equals(field.marshallingType());
    }

    private boolean isExplicitStringPayload(SdkField<?> field) {
        return isExplicitPayloadMember(field) && MarshallingType.STRING.equals(field.marshallingType());
    }

    private boolean isExplicitPayloadMember(SdkField<?> field) {
        return field.containsTrait(PayloadTrait.class, TraitType.PAYLOAD_TRAIT);
    }

    private void marshallExplicitJsonPayload(SdkField<?> field, Object val, StringBuilder jsonBuilder) {
        jsonBuilder.append('{');
        if (val != null) {
            if (MarshallingType.DOCUMENT.equals(field.marshallingType())) {
                marshallField(field, val, jsonBuilder, false);
            } else {
                doMarshall((SdkPojo) val, jsonBuilder);
            }
        }
        jsonBuilder.append('}');
    }

    private boolean marshallField(SdkField<?> field, Object val, StringBuilder jsonBuilder, boolean needsComma) {
        if (field.location() != MarshallLocation.PAYLOAD) {
            return needsComma; // Only handle payload marshalling for now
        }
        
        if (val == null) {
            return needsComma; // Skip null values
        }
        
        // Skip SDK auto-construct collections (but not regular empty collections)
        if (val instanceof SdkAutoConstructList || val instanceof SdkAutoConstructMap) {
            return needsComma;
        }
        
        String fieldName = field.locationName();
        MarshallingType<?> type = field.marshallingType();
        
        if (needsComma) {
            jsonBuilder.append(',');
        }
        
        appendFieldName(fieldName, jsonBuilder);
        marshallValue(val, type, field, jsonBuilder);
        
        return true;
    }

    private void appendFieldName(String fieldName, StringBuilder jsonBuilder) {
        jsonBuilder.append('"').append(escapeJson(fieldName)).append("\":");
    }

    private void marshallValue(Object val, MarshallingType<?> type, SdkField<?> field, StringBuilder jsonBuilder) {
        if (type == MarshallingType.STRING) {
            appendString((String) val, jsonBuilder);
        } else if (type == MarshallingType.INTEGER) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.LONG) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.SHORT) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.BYTE) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.DOUBLE) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.FLOAT) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.BIG_DECIMAL) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.BOOLEAN) {
            jsonBuilder.append(val);
        } else if (type == MarshallingType.INSTANT) {
            marshallInstant((Instant) val, field, jsonBuilder);
        } else if (type == MarshallingType.SDK_BYTES) {
            marshallSdkBytes((SdkBytes) val, jsonBuilder);
        } else if (type == MarshallingType.SDK_POJO) {
            marshallSdkPojo((SdkPojo) val, jsonBuilder);
        } else if (type == MarshallingType.LIST) {
            marshallList((List<?>) val, jsonBuilder);
        } else if (type == MarshallingType.MAP) {
            marshallMap((Map<String, ?>) val, jsonBuilder);
        } else if (type == MarshallingType.DOCUMENT) {
            marshallDocument((Document) val, jsonBuilder);
        } else {
            throw new UnsupportedOperationException("Unsupported marshalling type: " + type);
        }
    }

    private void appendString(String str, StringBuilder jsonBuilder) {
        jsonBuilder.append('"').append(escapeJson(str)).append('"');
    }

    private String escapeJson(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private void marshallInstant(Instant instant, SdkField<?> field, StringBuilder jsonBuilder) {
        TimestampFormatTrait trait = field != null ? field.getTrait(TimestampFormatTrait.class,
                                                                     TraitType.TIMESTAMP_FORMAT_TRAIT) : null;
        if (trait != null) {
            switch (trait.format()) {
                case UNIX_TIMESTAMP:
                    jsonBuilder.append(DateUtils.formatUnixTimestampInstant(instant));
                    break;
                case RFC_822:
                    appendString(DateUtils.formatRfc822Date(instant), jsonBuilder);
                    break;
                case ISO_8601:
                    appendString(DateUtils.formatIso8601Date(instant), jsonBuilder);
                    break;
                default:
                    // Default to unix timestamp as decimal
                    jsonBuilder.append(DateUtils.formatUnixTimestampInstant(instant));
            }
        } else {
            // Default to unix timestamp as decimal
            jsonBuilder.append(DateUtils.formatUnixTimestampInstant(instant));
        }
    }

    private void marshallSdkBytes(SdkBytes bytes, StringBuilder jsonBuilder) {
        // Base64 encode the bytes, matching Jackson's behavior
        String base64 = Base64.getEncoder().encodeToString(bytes.asByteArrayUnsafe());
        appendString(base64, jsonBuilder);
    }

    private void marshallSdkPojo(SdkPojo pojo, StringBuilder jsonBuilder) {
        jsonBuilder.append('{');
        doMarshall(pojo, jsonBuilder);
        jsonBuilder.append('}');
    }

    private void marshallList(List<?> list, StringBuilder jsonBuilder) {
        jsonBuilder.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                jsonBuilder.append(',');
            }
            marshallListItem(item, jsonBuilder);
            first = false;
        }
        jsonBuilder.append(']');
    }

    private void marshallListItem(Object item, StringBuilder jsonBuilder) {
        if (item == null) {
            jsonBuilder.append("null");
        } else if (item instanceof String) {
            appendString((String) item, jsonBuilder);
        } else if (item instanceof Number) {
            jsonBuilder.append(item);
        } else if (item instanceof Boolean) {
            jsonBuilder.append(item);
        } else if (item instanceof SdkPojo) {
            marshallSdkPojo((SdkPojo) item, jsonBuilder);
        } else if (item instanceof Map) {
            marshallMap((Map<String, ?>) item, jsonBuilder);
        } else if (item instanceof SdkBytes) {
            marshallSdkBytes((SdkBytes) item, jsonBuilder);
        } else {
            throw new UnsupportedOperationException("Unsupported list item type: " + item.getClass());
        }
    }

    private void marshallMap(Map<String, ?> map, StringBuilder jsonBuilder) {
        jsonBuilder.append('{');
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                if (!first) {
                    jsonBuilder.append(',');
                }
                appendFieldName(entry.getKey(), jsonBuilder);
                marshallMapValue(entry.getValue(), jsonBuilder);
                first = false;
            }
        }
        jsonBuilder.append('}');
    }

    private void marshallMapValue(Object value, StringBuilder jsonBuilder) {
        if (value instanceof String) {
            appendString((String) value, jsonBuilder);
        } else if (value instanceof Number) {
            jsonBuilder.append(value);
        } else if (value instanceof Boolean) {
            jsonBuilder.append(value);
        } else if (value instanceof SdkPojo) {
            marshallSdkPojo((SdkPojo) value, jsonBuilder);
        } else if (value instanceof List) {
            marshallList((List<?>) value, jsonBuilder);
        } else if (value instanceof SdkBytes) {
            marshallSdkBytes((SdkBytes) value, jsonBuilder);
        } else {
            throw new UnsupportedOperationException("Unsupported map value type: " + value.getClass());
        }
    }

    private void marshallDocument(Document document, StringBuilder jsonBuilder) {
        if (document.isNull()) {
            jsonBuilder.append("null");
        } else if (document.isBoolean()) {
            jsonBuilder.append(document.asBoolean());
        } else if (document.isNumber()) {
            jsonBuilder.append(document.asNumber());
        } else if (document.isString()) {
            appendString(document.asString(), jsonBuilder);
        } else if (document.isList()) {
            jsonBuilder.append('[');
            boolean first = true;
            for (Document item : document.asList()) {
                if (!first) {
                    jsonBuilder.append(',');
                }
                marshallDocument(item, jsonBuilder);
                first = false;
            }
            jsonBuilder.append(']');
        } else if (document.isMap()) {
            jsonBuilder.append('{');
            boolean first = true;
            for (Map.Entry<String, Document> entry : document.asMap().entrySet()) {
                if (!first) {
                    jsonBuilder.append(',');
                }
                appendFieldName(entry.getKey(), jsonBuilder);
                marshallDocument(entry.getValue(), jsonBuilder);
                first = false;
            }
            jsonBuilder.append('}');
        }
    }

    private SdkHttpFullRequest finishMarshalling(StringBuilder jsonBuilder) {
        if (request.contentStreamProvider() == null) {
            if (needTopLevelJsonObject()) {
                jsonBuilder.append('}');
            }

            byte[] content = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);

            if (content.length > 0) {
                request.contentStreamProvider(() -> new ByteArrayInputStream(content));
                request.putHeader("Content-Length", Integer.toString(content.length));
            }
        }

        if (!request.firstMatchingHeader("Content-Type").isPresent() && !hasEvent) {
            if (contentType != null && !hasStreamingInput && request.firstMatchingHeader("Content-Length").isPresent()) {
                request.putHeader("Content-Type", contentType);
            }
        }

        if (hasAwsQueryCompatible) {
            request.putHeader("x-amzn-query-mode", "true");
        }

        return request.build();
    }

    private boolean needTopLevelJsonObject() {
        AwsJsonProtocol protocol = protocolMetadata.protocol();
        return protocol == AwsJsonProtocol.AWS_JSON
               || protocol == AwsJsonProtocol.SMITHY_RPC_V2_CBOR
               || (!hasExplicitPayloadMember && hasImplicitPayloadMembers);
    }
}
