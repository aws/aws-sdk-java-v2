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

package software.amazon.awssdk.protocols.query.unmarshall;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.query.internal.unmarshall.AwsXmlErrorUnmarshaller;
import software.amazon.awssdk.utils.Pair;

/**
 * Error unmarshaller for Query/EC2/XML based protocols. Some examples of error responses from
 * the various protocols are below.
 *
 * <h3>Legacy Query (SimpleDB/EC2)</h3>
 * <pre>
 * {@code
 * <Response>
 *    <Errors>
 *       <Error>
 *          <Code>MissingParameter</Code>
 *          <Message>The request must contain the parameter DomainName</Message>
 *          <BoxUsage>0.0055590278</BoxUsage>
 *       </Error>
 *    </Errors>
 *    <RequestID>ad3280dd-5ac1-efd1-b9b0-a86969a9234d</RequestID>
 * </Response>
 * }
 * </pre>
 *
 * <h3>Traditional Query/Rest-XML (Cloudfront)</h3>
 * <pre>
 * {@code
 * <ErrorResponse xmlns="http://cloudfront.amazonaws.com/doc/2017-10-30/">
 *    <Error>
 *       <Type>Sender</Type>
 *       <Code>MalformedInput</Code>
 *       <Message>Invalid XML document</Message>
 *    </Error>
 *    <RequestId>7c8da4af-de44-11e8-a60e-1b2014315455</RequestId>
 * </ErrorResponse>
 * }
 * </pre>
 *
 * <h3>Amazon S3</h3>
 * <pre>
 * {@code
 * <Error>
 *    <Code>NoSuchBucket</Code>
 *    <Message>The specified bucket does not exist</Message>
 *    <BucketName>flajdfadjfladjf</BucketName>
 *    <RequestId>D9DBB9F267849CA3</RequestId>
 *    <HostId>fn8B1fUvWzg7I3CIeMT4UMqCZDF4+QO1JlbOJlQAVOosACZsLWv/K2dapVncz34a2mArhp11PjI=</HostId>
 * </Error>
 * }
 * </pre>
 */
@SdkProtectedApi
public final class AwsXmlErrorProtocolUnmarshaller implements HttpResponseHandler<AwsServiceException> {

    private final AwsXmlErrorUnmarshaller awsXmlErrorUnmarshaller;
    private final Function<XmlElement, Optional<XmlElement>> errorRootExtractor;

    private AwsXmlErrorProtocolUnmarshaller(Builder builder) {
        this.errorRootExtractor = builder.errorRootExtractor;
        this.awsXmlErrorUnmarshaller = AwsXmlErrorUnmarshaller.builder()
                                                              .defaultExceptionSupplier(builder.defaultExceptionSupplier)
                                                              .exceptions(builder.exceptions)
                                                              .errorUnmarshaller(builder.errorUnmarshaller)
                                                              .build();
    }

    @Override
    public AwsServiceException handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
        Pair<XmlElement, SdkBytes> xmlAndBytes = parseXml(response);
        XmlElement document = xmlAndBytes.left();
        Optional<XmlElement> errorRoot = errorRootExtractor.apply(document);
        return awsXmlErrorUnmarshaller.unmarshall(document, errorRoot, Optional.of(xmlAndBytes.right()), response,
                                                  executionAttributes);
    }

    /**
     * Parse the response content into an XML document. If we fail to read the content or the XML is malformed
     * we still return an empty {@link XmlElement} so that unmarshalling can proceed. In those failure
     * cases we can't parse out the error code so we'd unmarshall into a generic service exception.
     *
     * @param response HTTP response.
     * @return Pair of the parsed {@link XmlElement} and the raw bytes of the response.
     */
    private Pair<XmlElement, SdkBytes> parseXml(SdkHttpFullResponse response) {
        SdkBytes bytes = getResponseBytes(response);
        try {
            return Pair.of(XmlDomParser.parse(bytes.asInputStream()), bytes);
        } catch (Exception e) {
            return Pair.of(XmlElement.empty(), bytes);
        }
    }

    /**
     * Buffers the response content into an {@link SdkBytes} object. Used to fill the {@link AwsErrorDetails}. If
     * an {@link IOException} occurs then this will return {@link #emptyXmlBytes()} so that unmarshalling can proceed.
     *
     * @param response HTTP response.
     * @return Raw bytes of response.
     */
    private SdkBytes getResponseBytes(SdkHttpFullResponse response) {
        try {
            return response.content()
                           .map(SdkBytes::fromInputStream)
                           .orElseGet(this::emptyXmlBytes);
        } catch (Exception e) {
            return emptyXmlBytes();
        }
    }

    /**
     * @return Dummy XML document to allow unmarshalling when response can't be read/parsed.
     */
    private SdkBytes emptyXmlBytes() {
        return SdkBytes.fromUtf8String("<eof/>");
    }


    /**
     * @return New Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsXmlErrorProtocolUnmarshaller}.
     */
    public static final class Builder {

        private List<ExceptionMetadata> exceptions;
        private Supplier<SdkPojo> defaultExceptionSupplier;
        private Function<XmlElement, Optional<XmlElement>> errorRootExtractor;
        private XmlErrorUnmarshaller errorUnmarshaller;

        private Builder() {
        }

        /**
         * List of {@link ExceptionMetadata} to represent the modeled exceptions for the service.
         * For AWS services the error type is a string representing the type of the modeled exception.
         *
         * @return This builder for method chaining.
         */
        public Builder exceptions(List<ExceptionMetadata> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        /**
         * Default exception type if "error code" does not match any known modeled exception. This is the generated
         * base exception for the service (i.e. DynamoDbException).
         *
         * @return This builder for method chaining.
         */
        public Builder defaultExceptionSupplier(Supplier<SdkPojo> defaultExceptionSupplier) {
            this.defaultExceptionSupplier = defaultExceptionSupplier;
            return this;
        }

        /**
         * Extracts the <Error/> element from the top level XML document. Different protocols have slightly
         * different formats for where the Error element is located.  The error root of the XML document contains
         * the code, message and any modeled fields of the exception. See javadocs of
         * {@link AwsXmlErrorProtocolUnmarshaller} for examples.
         *
         * @param errorRootExtractor Function that extracts the <Error/> element from the root XML document.
         * @return This builder for method chaining.
         */
        public Builder errorRootExtractor(Function<XmlElement, Optional<XmlElement>> errorRootExtractor) {
            this.errorRootExtractor = errorRootExtractor;
            return this;
        }

        /**
         * The unmarshaller to use. The unmarshaller only unmarshalls any modeled fields of the exception,
         * additional metadata is extracted by {@link AwsXmlErrorProtocolUnmarshaller}.
         *
         * @param errorUnmarshaller Error unmarshaller to use.
         * @return This builder for method chaining.
         */
        public Builder errorUnmarshaller(XmlErrorUnmarshaller errorUnmarshaller) {
            this.errorUnmarshaller = errorUnmarshaller;
            return this;
        }

        /**
         * @return New instance of {@link AwsXmlErrorProtocolUnmarshaller}.
         */
        public AwsXmlErrorProtocolUnmarshaller build() {
            return new AwsXmlErrorProtocolUnmarshaller(this);
        }
    }
}
