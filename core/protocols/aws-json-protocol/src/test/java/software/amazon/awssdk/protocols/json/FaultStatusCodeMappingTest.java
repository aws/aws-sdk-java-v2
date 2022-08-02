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

package software.amazon.awssdk.protocols.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonProtocolErrorUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;


public class FaultStatusCodeMappingTest {

    @ParameterizedTest
    @MethodSource("unmarshal_faultValue_testCases")
    public void unmarshal_faultValue_useCorrectly(TestCase tc) {
        AwsJsonProtocolErrorUnmarshaller unmarshaller = makeUnmarshaller(
            Arrays.asList(ExceptionMetadata.builder()
                                           .errorCode("ServiceException")
                                           .httpStatusCode(tc.metadataStatusCode)
                                           .exceptionBuilderSupplier(AwsServiceException::builder)
                                           .build()));

        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                                                                         .content(errorContent("ServiceException"));

        if (tc.httpStatusCode != null) {
            responseBuilder.statusCode(tc.httpStatusCode);
        }

        AwsServiceException exception = unmarshaller.handle(responseBuilder.build(), new ExecutionAttributes());

        assertThat(exception.statusCode()).isEqualTo(tc.expectedStatusCode);
    }

    public static List<TestCase> unmarshal_faultValue_testCases() {
        return Arrays.asList(
            new TestCase(null, null, 500),
            new TestCase(null, 1, 1),
            new TestCase(2, null, 2),
            new TestCase(2, 1, 2)
        );
    }

    private static AwsJsonProtocolErrorUnmarshaller makeUnmarshaller(List<ExceptionMetadata> exceptionMetadata) {
        return AwsJsonProtocolErrorUnmarshaller.builder()
                                               .exceptions(exceptionMetadata)
                                               .jsonProtocolUnmarshaller(JsonProtocolUnmarshaller.builder()
                                                                                                 .defaultTimestampFormats(Collections.emptyMap())
                                                                                                 .build())
                                               .jsonFactory(new JsonFactory())
                                               .errorMessageParser((resp, content) -> "Some server error")
                                               .errorCodeParser((resp, content) ->
                                                                    content.getJsonNode().asObject().get("errorCode").asString())
                                               .build();
    }

    private static AbortableInputStream errorContent(String code) {
        String json = String.format("{\"errorCode\":\"%s\"}", code);
        return contentAsStream(json);
    }

    private static AbortableInputStream contentAsStream(String content) {
        return AbortableInputStream.create(SdkBytes.fromUtf8String(content).asInputStream());
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
