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
public final class QueryProtocolUnmarshaller implements XmlErrorUnmarshaller {

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

    private QueryProtocolUnmarshaller(Builder builder) {
        this.hasResultWrapper = builder.hasResultWrapper;
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

    /**
     * @return New {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QueryProtocolUnmarshaller}.
     */
    public static final class Builder {

        private boolean hasResultWrapper;

        private Builder() {
        }

        /**
         * <h3>Example response with result wrapper</h3>
         * <pre>
         * {@code
         * <ListQueuesResponse>
         *     <ListQueuesResult>
         *         <QueueUrl>https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue</QueueUrl>
         *     </ListQueuesResult>
         *     <ResponseMetadata>
         *         <RequestId>725275ae-0b9b-4762-b238-436d7c65a1ac</RequestId>
         *     </ResponseMetadata>
         * </ListQueuesResponse>
         * }
         * </pre>
         *
         * <h3>Example response without result wrapper</h3>
         * <pre>
         * {@code
         * <DescribeAddressesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15/">
         *    <requestId>f7de5e98-491a-4c19-a92d-908d6EXAMPLE</requestId>
         *    <addressesSet>
         *      <item>
         *        <publicIp>203.0.113.41</publicIp>
         *        <allocationId>eipalloc-08229861</allocationId>
         *        <domain>vpc</domain>
         *        <instanceId>i-0598c7d356eba48d7</instanceId>
         *        <associationId>eipassoc-f0229899</associationId>
         *        <networkInterfaceId>eni-ef229886</networkInterfaceId>
         *        <networkInterfaceOwnerId>053230519467</networkInterfaceOwnerId>
         *        <privateIpAddress>10.0.0.228</privateIpAddress>
         *      </item>
         *    </addressesSet>
         * </DescribeAddressesResponse>
         * }
         * </pre>
         *
         * @param hasResultWrapper True if the response has a result wrapper, false if the result is in the top level
         * XML document.
         * @return This builder for method chaining.
         */
        public Builder hasResultWrapper(boolean hasResultWrapper) {
            this.hasResultWrapper = hasResultWrapper;
            return this;
        }

        /**
         * @return New instance of {@link QueryProtocolUnmarshaller}.
         */
        public QueryProtocolUnmarshaller build() {
            return new QueryProtocolUnmarshaller(this);
        }
    }
}
