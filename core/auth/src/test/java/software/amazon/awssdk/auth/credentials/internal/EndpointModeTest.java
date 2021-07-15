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

package software.amazon.awssdk.auth.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.auth.credentials.internal.Ec2MetadataConfigProvider.EndpointMode.IPV4;
import static software.amazon.awssdk.auth.credentials.internal.Ec2MetadataConfigProvider.EndpointMode.IPV6;
import org.junit.Test;

public class EndpointModeTest {
    @Test
    public void fromString_caseInsensitive() {
        assertThat(Ec2MetadataConfigProvider.EndpointMode.fromValue("iPv6")).isEqualTo(IPV6);
        assertThat(Ec2MetadataConfigProvider.EndpointMode.fromValue("iPv4")).isEqualTo(IPV4);
    }

    @Test
    public void fromString_unknownValue_throws() {
        assertThatThrownBy(() -> Ec2MetadataConfigProvider.EndpointMode.fromValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fromString_nullValue_returnsNull() {
        assertThat(Ec2MetadataConfigProvider.EndpointMode.fromValue(null)).isNull();
    }
}
