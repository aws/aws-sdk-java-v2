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

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class AWSQueryModeTest {

    private static final SdkClientConfiguration EMPTY_CLIENT_CONFIGURATION = SdkClientConfiguration.builder()
                                                                                                   .build();
    private static final OperationInfo EMPTY_OPERATION_INFO = OperationInfo.builder()
                                                                           .httpMethod(SdkHttpMethod.POST)
                                                                           .hasImplicitPayloadMembers(true)
                                                                           .build();

    private final BaseAwsJsonProtocolFactory factory = AwsJsonProtocolFactory.builder()
                                                               .protocol(AwsJsonProtocol.AWS_JSON)
                                                               .clientConfiguration(EMPTY_CLIENT_CONFIGURATION)
                                                               .build();

    private final Supplier<StructuredJsonFactory> structuredJsonFactory = getStructuredJsonFactory(factory);

    private final AwsJsonProtocolMetadata metadata = AwsJsonProtocolMetadata.builder()
                                                              .protocol(AwsJsonProtocol.AWS_JSON)
                                                              .contentType("application/json")
                                                              .build();


    static Supplier<StructuredJsonFactory> getStructuredJsonFactory(BaseAwsJsonProtocolFactory factory) {
        return () -> {
            try {
                Method method = BaseAwsJsonProtocolFactory.class.getDeclaredMethod("getSdkFactory");
                method.setAccessible(true);
                return (StructuredJsonFactory) method.invoke(factory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }


    /*
     * A simple test POJO to marshall
     */
    private static final class TestPojo implements SdkPojo {

        private TestPojo() {}

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public boolean equalsBySdkFields(Object other) {
            if (!(other instanceof TestPojo)) {
                return false;
            }
            return true;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return Collections.emptyMap();
        }

    }

    @Test
    public void testMarshallWithAwsQueryCompatibleTrue() {
        try {
            ProtocolMarshaller<SdkHttpFullRequest> marshaller =
                JsonProtocolMarshallerBuilder.create()
                                             .endpoint(new URI("http://localhost/"))
                                             .jsonGenerator(structuredJsonFactory.get().createWriter("application/json"))
                                             .contentType("application/json")
                                             .operationInfo(EMPTY_OPERATION_INFO)
                                             .sendExplicitNullForPayload(false)
                                             .protocolMetadata(metadata)
                                             .hasAwsQueryCompatible(true)
                                             .build();
            SdkPojo testPojo = new TestPojo();

            SdkHttpFullRequest result = marshaller.marshall(testPojo);

            assertThat(result.headers()).containsKey("x-amzn-query-mode");
            assertThat(result.headers().get("x-amzn-query-mode").get(0)).isEqualTo("true");

        }catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testMarshallWithNoAwsQueryCompatible() {
        try {
            ProtocolMarshaller<SdkHttpFullRequest> marshaller =
                JsonProtocolMarshallerBuilder.create()
                                             .endpoint(new URI("http://localhost/"))
                                             .jsonGenerator(structuredJsonFactory.get().createWriter("application/json"))
                                             .contentType("application/json")
                                             .operationInfo(EMPTY_OPERATION_INFO)
                                             .sendExplicitNullForPayload(false)
                                             .protocolMetadata(metadata)
                                             .build();
            SdkPojo testPojo = new TestPojo();

            SdkHttpFullRequest result = marshaller.marshall(testPojo);

            assertThat(result.headers()).doesNotContainKey("x-amzn-query-mode");

        }catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

}
