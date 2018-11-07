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

package software.amazon.awssdk.protocols.xml;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.query.unmarshall.AwsXmlErrorProtocolUnmarshaller;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlGenerator;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.AwsXmlResponseHandler;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlOperationMetadata;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller;

/**
 * Factory to generate the various protocol handlers and generators to be used for
 * communicating with REST/XML services.
 */
@SdkProtectedApi
public class AwsXmlProtocolFactory {

    private final Map<String, Supplier<SdkPojo>> modeledExceptions;
    private final Supplier<SdkPojo> defaultServiceExceptionSupplier;
    private final AwsXmlErrorProtocolUnmarshaller errorUnmarshaller;

    AwsXmlProtocolFactory(Builder<?> builder) {
        this.modeledExceptions = unmodifiableMap(new HashMap<>(builder.modeledExceptions));
        this.defaultServiceExceptionSupplier = builder.defaultServiceExceptionSupplier;
        this.errorUnmarshaller = AwsXmlErrorProtocolUnmarshaller
            .builder()
            .defaultExceptionSupplier(defaultServiceExceptionSupplier)
            .exceptions(modeledExceptions)
            .errorUnmarshaller(XmlProtocolUnmarshaller.builder().build())
            .errorRootExtractor(this::getErrorRoot)
            .build();
    }

    /**
     * Creates an instance of {@link XmlProtocolMarshaller} to be used for marshalling the requess.
     *
     * @param operationInfo Info required to marshall the request
     * @param origRequest The original request to marshall
     * @param rootElement The root of the xml document if present. See {@link XmlProtocolMarshallerBuilder#rootElement(String)}.
     * @param xmlNameSpaceUri The XML namespace to include in the xmlns attribute of the root element.
     */
    public <T extends AwsRequest> ProtocolMarshaller<Request<T>> createProtocolMarshaller(OperationInfo operationInfo,
                                                                                          T origRequest,
                                                                                          String rootElement,
                                                                                          String xmlNameSpaceUri) {
        return XmlProtocolMarshallerBuilder.<T>builder()
            .xmlGenerator(createGenerator(operationInfo, xmlNameSpaceUri))
            .originalRequest(origRequest)
            .operationInfo(operationInfo)
            .rootElement(rootElement)
            .build();
    }

    public <T extends AwsResponse> HttpResponseHandler<T> createResponseHandler(Supplier<SdkPojo> pojoSupplier,
                                                                                XmlOperationMetadata staxOperationMetadata) {
        return new AwsXmlResponseHandler<>(
            XmlProtocolUnmarshaller.builder().build(), r -> pojoSupplier.get(),
            staxOperationMetadata.isHasStreamingSuccessResponse());
    }

    public HttpResponseHandler<AwsServiceException> createErrorResponseHandler() {
        return errorUnmarshaller;
    }

    /**
     * Extracts the <Error/> element from the root XML document. This method is protected as S3 has
     * a slightly different location.
     *
     * @param document Root XML document.
     * @return If error root is found than a fulfilled {@link Optional}, otherwise an empty one.
     */
    Optional<XmlElement> getErrorRoot(XmlElement document) {
        return document.getOptionalElementByName("Error");
    }

    private XmlGenerator createGenerator(OperationInfo operationInfo, String xmlNameSpaceUri) {
        return operationInfo.hasPayloadMembers() ? XmlGenerator.create(xmlNameSpaceUri) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsXmlProtocolFactory}.
     */
    public static class Builder<SubclassT extends Builder> {

        private final Map<String, Supplier<SdkPojo>> modeledExceptions = new HashMap<>();
        private Supplier<SdkPojo> defaultServiceExceptionSupplier;

        Builder() {
        }

        public SubclassT registerModeledException(String errorCode, Supplier<SdkPojo> exceptionBuilderSupplier) {
            modeledExceptions.put(errorCode, exceptionBuilderSupplier);
            return getSubclass();
        }

        public SubclassT defaultServiceExceptionSupplier(Supplier<SdkPojo> exceptionBuilderSupplier) {
            this.defaultServiceExceptionSupplier = exceptionBuilderSupplier;
            return getSubclass();
        }

        @SuppressWarnings("unchecked")
        private SubclassT getSubclass() {
            return (SubclassT) this;
        }

        public AwsXmlProtocolFactory build() {
            return new AwsXmlProtocolFactory(this);
        }
    }
}