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

package software.amazon.awssdk.services.useragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClient;

public class StreamingBodyAndTransformerImplTrackingTest {

    private CapturingInterceptor interceptor;

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @Test
    public void streamingInputOperation_syncClient_fileBody_recordsMetadata() throws IOException {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient.create();
        File testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        testFile.deleteOnExit();
        assertThatThrownBy(() -> client.streamingInputOperation(r -> {}, RequestBody.fromFile(testFile)))
            .hasMessageContaining("stop");
        assertThat(interceptor.userAgent()).contains("md/rb#f");
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public String userAgent() {
            return context.httpRequest().headers().get("User-Agent").get(0);
        }
    }
}
