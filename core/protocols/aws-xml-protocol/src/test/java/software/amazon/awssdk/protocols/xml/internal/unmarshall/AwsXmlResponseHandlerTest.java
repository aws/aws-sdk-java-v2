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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.ImmutableMap;

public class AwsXmlResponseHandlerTest {

    @Test
    public void handleResponse_awsResponse_shouldAddResponseMetadata() throws Exception {
        HttpResponseHandler<FakeResponse> delegate = Mockito.mock(HttpResponseHandler.class);
        AwsXmlResponseHandler<FakeResponse> responseHandler = new AwsXmlResponseHandler<>(delegate);

        SdkHttpFullResponse response = new TestSdkHttpFullResponse();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        FakeResponse fakeResponse = FakeResponse.builder().build();

        Mockito.when(delegate.handle(response, executionAttributes)).thenReturn(fakeResponse);

        assertThat(responseHandler.handle(response, executionAttributes)
            .responseMetadata().requestId()).isEqualTo("1234");
    }

    @Test
    public void handleResponse_nonAwsResponse_shouldReturnDirectly() throws Exception {
        HttpResponseHandler<SdkPojo> delegate = Mockito.mock(HttpResponseHandler.class);
        AwsXmlResponseHandler<SdkPojo> responseHandler = new AwsXmlResponseHandler<>(delegate);

        SdkHttpFullResponse response = new TestSdkHttpFullResponse();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        FakeSdkPojo fakeResponse = new FakeSdkPojo();

        Mockito.when(delegate.handle(response, executionAttributes)).thenReturn(fakeResponse);

        assertThat(responseHandler.handle(response, executionAttributes)).isEqualTo(fakeResponse);
    }

    private static final class FakeSdkPojo implements SdkPojo {

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }
    }

    private static final class FakeResponse extends AwsResponse {
        private FakeResponse(Builder builder) {
            super(builder);
        }

        @Override
        public Builder toBuilder() {
            return new Builder();
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        public static Builder builder() {
            return new Builder();
        }

        private static final class Builder extends AwsResponse.BuilderImpl {

            @Override
            public FakeResponse build() {
                return new FakeResponse(this);
            }
        }
    }

    private static class TestSdkHttpFullResponse implements SdkHttpFullResponse {
        @Override
        public Builder toBuilder() {
            return null;
        }

        @Override
        public Optional<AbortableInputStream> content() {
            return Optional.empty();
        }

        @Override
        public Optional<String> statusText() {
            return Optional.empty();
        }

        @Override
        public int statusCode() {
            return 0;
        }

        @Override
        public Map<String, List<String>> headers() {
            return ImmutableMap.of(AWS_REQUEST_ID, Collections.singletonList("1234"));
        }
    }
}
