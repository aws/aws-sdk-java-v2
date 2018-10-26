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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.query.XmlDomParser;
import software.amazon.awssdk.protocols.query.XmlElement;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Unmarshaller implementation for AWS/Query and EC2 services.
 *
 * @param <TypeT> Type to unmarshall into.
 */
@SdkInternalApi
public class QueryProtocolUnmarshaller<TypeT extends SdkPojo> {

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

    // TODO builder
    public QueryProtocolUnmarshaller(boolean hasResultWrapper) {
        this.hasResultWrapper = hasResultWrapper;
    }

    public Pair<TypeT, Map<String, String>> unmarshall(SdkPojo sdkPojo,
                                                       SdkHttpFullResponse response) throws Exception {
        QueryUnmarshallerContext unmarshallerContext = QueryUnmarshallerContext.builder()
                                                                               .registry(UNMARSHALLER_REGISTRY)
                                                                               .protocolUnmarshaller(this)
                                                                               .build();
        XmlElement document = XmlDomParser.parse(response.content().orElseGet(this::emptyStream));
        XmlElement resultRoot = hasResultWrapper ? document.getFirstChild() : document;
        return Pair.of((TypeT) unmarshall(unmarshallerContext, sdkPojo, resultRoot), parseMetadata(document));
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
        return ((SdkBuilder<?, SdkPojo>) sdkPojo).build();
    }

    private AbortableInputStream emptyStream() {
        return AbortableInputStream.create(new StringInputStream("</eof>"));
    }
}
