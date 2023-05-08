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

import io.netty.channel.ChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.resolver.AddressResolverGroup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Utility class for instantiating netty dns resolvers only if they're available on the class path.
 */
@SdkProtectedApi
public class DnsResolverLoader {

    private DnsResolverLoader() {
    }

    public static AddressResolverGroup<InetSocketAddress> init(ChannelFactory<? extends DatagramChannel> datagramChannelFactory) {
        try {
            Class<?> addressResolver = ClassLoaderHelper.loadClass(getAddressResolverGroup(), false, (Class) null);
            Class<?> dnsNameResolverBuilder = ClassLoaderHelper.loadClass(getDnsNameResolverBuilder(), false, (Class) null);

            Object dnsResolverObj = dnsNameResolverBuilder.newInstance();
            Method method = dnsResolverObj.getClass().getMethod("channelFactory", ChannelFactory.class);
            method.invoke(dnsResolverObj, datagramChannelFactory);

            Object e = addressResolver.getConstructor(dnsNameResolverBuilder).newInstance(dnsResolverObj);
            return (AddressResolverGroup<InetSocketAddress>) e;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find module io.netty.resolver.dns "
                                            + " To use netty non blocking dns," +
                                            " the 'netty-resolver-dns' module from io.netty must be on the class path. ", e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Failed to create AddressResolverGroup", e);
        }
    }

    private static String getAddressResolverGroup() {
        return "io.netty.resolver.dns.DnsAddressResolverGroup";
    }

    private static String getDnsNameResolverBuilder() {
        return "io.netty.resolver.dns.DnsNameResolverBuilder";
    }
}
