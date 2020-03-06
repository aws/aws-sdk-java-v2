/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class ApplyUserAgentInterceptorTest {

    private ApplyUserAgentInterceptor interceptor = new ApplyUserAgentInterceptor();

    @Test
    public void ddbRequest_shouldModifyRequest() {
        GetItemRequest getItemRequest = GetItemRequest.builder().build();
        SdkRequest sdkRequest = interceptor.modifyRequest(() -> getItemRequest, new ExecutionAttributes());

        RequestOverrideConfiguration requestOverrideConfiguration = sdkRequest.overrideConfiguration().get();
        assertThat(requestOverrideConfiguration.apiNames()
                                               .stream()
                                               .filter(a -> a.name()
                                                             .equals("hll") &&
                                                            a.version().equals("ddb-enh")).findAny())
            .isPresent();
    }

    @Test
    public void otherRequest_shouldNotModifyRequest() {
        SdkRequest someOtherRequest = new SdkRequest() {
            @Override
            public List<SdkField<?>> sdkFields() {
                return null;
            }

            @Override
            public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
                return Optional.empty();
            }

            @Override
            public Builder toBuilder() {
                return null;
            }
        };
        SdkRequest sdkRequest = interceptor.modifyRequest(() -> someOtherRequest, new ExecutionAttributes());

        assertThat(sdkRequest).isEqualTo(someOtherRequest);
    }
}
