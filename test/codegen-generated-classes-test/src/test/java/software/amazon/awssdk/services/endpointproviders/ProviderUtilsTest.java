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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.rules.AwsProviderUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.ProviderUtils;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;

public class ProviderUtilsTest {
    @Test
    public void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, false);
        assertThat(ProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(ProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        assertThat(ProviderUtils.endpointIsOverridden(attrs)).isTrue();
    }

    @Test
    public void endpointIsDiscovered_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, false);
        assertThat(ProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(ProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        assertThat(ProviderUtils.endpointIsDiscovered(attrs)).isTrue();
    }

    @Test
    public void valueAsEndpoint_isNone_throws() {
        assertThatThrownBy(() -> ProviderUtils.valueAsEndpointOrThrow(Value.none()))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void valueAsEndpoint_isString_throwsAsMsg() {
        assertThatThrownBy(() -> ProviderUtils.valueAsEndpointOrThrow(Value.fromStr("oops!")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("oops!");
    }

    @Test
    public void valueAsEndpoint_isEndpoint_returnsEndpoint() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .build();

        Endpoint expected = Endpoint.builder()
                                    .url(URI.create("https://myservice.aws"))
                                    .build();

        assertThat(expected.url()).isEqualTo(ProviderUtils.valueAsEndpointOrThrow(endpointVal).url());
    }

    @Test
    public void regionBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        assertThat(AwsProviderUtils.regionBuiltIn(attrs)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    public void dualStackEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, true);
        assertThat(AwsProviderUtils.dualStackEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void fipsEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, true);
        assertThat(AwsProviderUtils.fipsEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void setUri_modifiesRequestUriCorrectly() {
        URI newUri = URI.create("https://myservice.us-west-2.aws:8080");

        SdkHttpRequest request = SdkHttpRequest.builder()
            .method(SdkHttpMethod.GET)
            .protocol("http")
            .host("otherservice.aws")
            .port(443)
            .build();

        SdkHttpRequest newRequest = ProviderUtils.setUri(request, newUri);

        assertThat(newRequest.getUri()).isEqualTo(newUri);
    }
}
