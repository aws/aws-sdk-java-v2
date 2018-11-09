/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;
import static software.amazon.awssdk.protocols.query.internal.marshall.SimpleTypeQueryMarshaller.defaultTimestampFormats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.protocols.query.unmarshall.XmlErrorUnmarshaller;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Unmarshaller implementation for AWS/Query and EC2 services.
 */
@SdkInternalApi
public class QueryProtocolUnmarshaller implements XmlErrorUnmarshaller {

    private static final QueryUnmarshallerRegistry UNMARSHALLER_REGISTRY = QueryUnmarshallerRegistry
        .builder()
        .unmarshaller(MarshallingType.STRING, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_STRING))
        .unmarshaller(MarshallingType.INTEGER, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_INTEGER))
        .unmarshaller(MarshallingType.LONG, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_LONG))
        .unmarshaller(MarshallingType.FLOAT, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_FLOAT))
        .unmarshaller(MarshallingType.DOUBLE, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
        .unmarshaller(MarshallingType.BOOLEAN, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_BOOLEAN))
        .unmarshaller(MarshallingType.DOUBLE, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_DOUBLE))
        .unmarshaller(MarshallingType.INSTANT,
                      new SimpleTypeQueryUnmarshaller<>(StringToInstant.create(defaultTimestampFormats())))
        .unmarshaller(MarshallingType.SDK_BYTES, new SimpleTypeQueryUnmarshaller<>(StringToValueConverter.TO_SDK_BYTES))
        .unmarshaller(MarshallingType.LIST, new ListQueryUnmarshaller())
        .unmarshaller(MarshallingType.MAP, new MapQueryUnmarshaller())
        .unmarshaller(MarshallingType.NULL, (context, content, field) -> null)
        .unmarshaller(MarshallingType.SDK_POJO, (context, content, field) ->
            context.protocolUnmarshaller().unmarshall(context, field.constructor().get(), content.get(0)))
        .build();

    private final boolean hasResultWrapper;

    private QueryProtocolUnmarshaller(boolean hasResultWrapper) {
        this.hasResultWrapper = hasResultWrapper;
    }

    public <TypeT extends SdkPojo> Pair<TypeT, Map<String, String>> unmarshall(SdkPojo sdkPojo,
                                                                               SdkHttpFullResponse response) {
        XmlElement document = response.content().map(XmlDomParser::parse).orElse(XmlElement.empty());
        XmlElement resultRoot = hasResultWrapper ? document.getFirstChild() : document;
        return Pair.of(unmarshall(sdkPojo, resultRoot, response), parseMetadata(document));
    }

    /**
     * This method is also used to unmarshall exceptions. We use this since we've already parsed the XML
     * and the result root is in a different location depending on the protocol/service.
     */
    @Override
    public <TypeT extends SdkPojo> TypeT unmarshall(SdkPojo sdkPojo,
                                                    XmlElement resultRoot,
                                                    SdkHttpFullResponse response) {
        QueryUnmarshallerContext unmarshallerContext = QueryUnmarshallerContext.builder()
                                                                               .registry(UNMARSHALLER_REGISTRY)
                                                                               .protocolUnmarshaller(this)
                                                                               .build();
        return (TypeT) unmarshall(unmarshallerContext, sdkPojo, resultRoot);
    }

    private Map<String, String> parseMetadata(XmlElement document) {
        XmlElement responseMetadata = document.getElementByName("ResponseMetadata");
        Map<String, String> metadata = new HashMap<>();
        if (responseMetadata != null) {
            responseMetadata.children().forEach(c -> metadata.put(metadataKeyName(c), c.textContent()));
        }
        XmlElement requestId = document.getElementByName("requestId");
        if (requestId != null) {
            metadata.put(AWS_REQUEST_ID, requestId.textContent());
        }
        return metadata;
    }

    private String metadataKeyName(XmlElement c) {
        return c.elementName().equals("RequestId") ? AWS_REQUEST_ID : c.elementName();
    }

    private SdkPojo unmarshall(QueryUnmarshallerContext context, SdkPojo sdkPojo, XmlElement root) {
        if (root != null) {
            for (SdkField<?> field : sdkPojo.sdkFields()) {
                List<XmlElement> element = root.getElementsByName(field.unmarshallLocationName());
                if (!CollectionUtils.isNullOrEmpty(element)) {
                    QueryUnmarshaller<Object> unmarshaller =
                        UNMARSHALLER_REGISTRY.getUnmarshaller(field.location(), field.marshallingType());
                    Object unmarshalled = unmarshaller.unmarshall(context, element, (SdkField<Object>) field);
                    field.set(sdkPojo, unmarshalled);
                }
            }
        }
        return (SdkPojo) ((Buildable) sdkPojo).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean hasResultWrapper;

        private Builder() {
        }

        public Builder hasResultWrapper(boolean hasResultWrapper) {
            this.hasResultWrapper = hasResultWrapper;
            return this;
        }

        public QueryProtocolUnmarshaller build() {
            return new QueryProtocolUnmarshaller(hasResultWrapper);
        }
    }
}
