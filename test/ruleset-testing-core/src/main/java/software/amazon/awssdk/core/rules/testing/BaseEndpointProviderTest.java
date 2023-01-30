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

package software.amazon.awssdk.core.rules.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;

public class BaseEndpointProviderTest {
    protected final void verify(EndpointProviderTestCase tc) {
        Expect expect = tc.getExpect();
        Supplier<Endpoint> testMethod = tc.getTestMethod();
        if (expect.error() != null) {
            assertThatThrownBy(testMethod::get).hasMessageContaining(expect.error());
        } else {
            Endpoint actualEndpoint = testMethod.get();
            Endpoint expectedEndpoint = expect.endpoint();
            assertThat(actualEndpoint.url()).isEqualTo(expectedEndpoint.url());
            assertThat(actualEndpoint.headers()).isEqualTo(expectedEndpoint.headers());
            AwsEndpointAttribute.values().forEach(attr -> {
                if (expectedEndpoint.attribute(attr) != null) {
                    assertThat(actualEndpoint.attribute(attr)).isEqualTo(expectedEndpoint.attribute(attr));
                } else {
                    assertThat(actualEndpoint.attribute(attr)).isNull();
                }
            });
        }
    }
}
