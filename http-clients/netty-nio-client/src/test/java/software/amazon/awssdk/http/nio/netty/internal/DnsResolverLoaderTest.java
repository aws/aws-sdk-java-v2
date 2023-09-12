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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import org.junit.jupiter.api.Test;

public class DnsResolverLoaderTest {

    @Test
    public void canResolveChannelFactory() {
        assertThat(DnsResolverLoader.init(NioDatagramChannel::new)).isInstanceOf(DnsAddressResolverGroup.class);
        assertThat(DnsResolverLoader.init(EpollDatagramChannel::new)).isInstanceOf(DnsAddressResolverGroup.class);
        assertThat(DnsResolverLoader.init(OioDatagramChannel::new)).isInstanceOf(DnsAddressResolverGroup.class);
    }
}
