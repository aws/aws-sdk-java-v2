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

package software.amazon.awssdk.protocols.rpcv2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;


public class FaultStatusCodeMappingTest {

    @ParameterizedTest
    @MethodSource("unmarshal_faultValue_testCases")
    public void unmarshal_faultValue_useCorrectly(TestCase tc) throws Exception {
        String errorCode = "ServiceException";
        ExceptionMetadata exceptionMedata = ExceptionMetadata.builder()
                                                              .errorCode(errorCode)
                                                              .httpStatusCode(tc.metadataStatusCode)
                                                              .exceptionBuilderSupplier(AwsServiceException::builder)
                                                              .build();
        HttpResponseHandler<AwsServiceException> unmarshaller = makeUnmarshaller(c -> Optional.of(exceptionMedata));

        SdkHttpFullResponse.Builder responseBuilder =
            SdkHttpFullResponse
                .builder()
                .content(errorContent(errorCode));

        if (tc.httpStatusCode != null) {
            responseBuilder.statusCode(tc.httpStatusCode);
        }

        AwsServiceException exception = unmarshaller.handle(responseBuilder.build(), new ExecutionAttributes());

        assertThat(exception.statusCode()).isEqualTo(tc.expectedStatusCode);
        assertThat(exception.awsErrorDetails().errorCode()).isEqualTo(errorCode);
    }

    public static List<TestCase> unmarshal_faultValue_testCases() {
        return Arrays.asList(
            new TestCase(null, null, 500),
            new TestCase(null, 1, 1),
            new TestCase(2, null, 2),
            new TestCase(2, 1, 2)
        );
    }

    private static HttpResponseHandler<AwsServiceException> makeUnmarshaller(Function<String, Optional<ExceptionMetadata>> exceptionMdSupplier) {
        return SmithyRpcV2CborProtocolFactory.builder()
                                      .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
                                      .protocolVersion("1.1").build()
                                      .createErrorResponseHandler(JsonOperationMetadata.builder().isPayloadJson(true).build(),
                                                                  exceptionMdSupplier);
    }

    private static AbortableInputStream errorContent(String code) {
        StructuredJsonGenerator writer = SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter("application/cbor");
        writer.writeStartObject();
        writer.writeFieldName("__type");
        writer.writeValue(code);
        writer.writeEndObject();
        return contentAsStream(writer.getBytes());
    }

    private static AbortableInputStream contentAsStream(byte[] content) {
        return AbortableInputStream.create(new ByteArrayInputStream(content));
    }

    private static class TestCase {
        private final Integer httpStatusCode;
        private final Integer metadataStatusCode;

        private final int expectedStatusCode;

        public TestCase(Integer httpStatusCode, Integer metadataStatusCode, int expectedStatusCode) {
            this.httpStatusCode = httpStatusCode;
            this.metadataStatusCode = metadataStatusCode;
            this.expectedStatusCode = expectedStatusCode;
        }
    }
}
