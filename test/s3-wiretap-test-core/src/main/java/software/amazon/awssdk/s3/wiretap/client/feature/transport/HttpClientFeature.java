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

package software.amazon.awssdk.s3.wiretap.client.feature.transport;

import java.util.List;
import java.util.function.BiConsumer;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilder;
import software.amazon.awssdk.s3.wiretap.client.HttpClient;
import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.s3.wiretap.client.feature.CategoryGroup;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public final class HttpClientFeature extends AbstractFeature<HttpClient.Type> {

    public HttpClientFeature() {
        super("http", HttpClient.Type.class);
        BiConsumer<ClientBuilder, HttpClient.Type> writer = HttpClient::apply;
        onSync(writer);
        onAsync(writer);
        onCrt(writer);
    }

    @Override
    public Category<HttpClient.Type> defaultCategory(Class<?> paramType) {
        return paramType == S3AsyncClient.class ? new NettyNio() : new Apache();
    }

    @Override
    public boolean alwaysShowInLabel() {
        return true;
    }

    public static final class Apache implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.APACHE;
        }
    }

    public static final class Apache5 implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.APACHE5;
        }
    }

    public static final class UrlConnection implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.URL_CONNECTION;
        }
    }

    public static final class AwsCrt implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.AWS_CRT;
        }
    }

    public static final class NettyNio implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.NETTY_NIO;
        }
    }

    public static final class AwsCrtAsync implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.AWS_CRT_ASYNC;
        }
    }

    public static final class CrtCreate implements Category<HttpClient.Type> {
        @Override
        public HttpClient.Type value() {
            return HttpClient.Type.CRT_CREATE;
        }
    }

    public static final class All implements CategoryGroup<HttpClient.Type> {
        @Override
        public List<Category<HttpClient.Type>> categories() {
            return List.of(new Apache(), new Apache5(), new UrlConnection(), new AwsCrt(),
                    new NettyNio(), new AwsCrtAsync(), new CrtCreate());
        }
    }

    public static final class AllSync implements CategoryGroup<HttpClient.Type> {
        @Override
        public List<Category<HttpClient.Type>> categories() {
            return List.of(new Apache(), new Apache5(), new UrlConnection(), new AwsCrt());
        }
    }

    public static final class AllAsync implements CategoryGroup<HttpClient.Type> {
        @Override
        public List<Category<HttpClient.Type>> categories() {
            return List.of(new NettyNio(), new AwsCrtAsync(), new CrtCreate());
        }
    }

    public static final class AllAsyncNonCrt implements CategoryGroup<HttpClient.Type> {
        @Override
        public List<Category<HttpClient.Type>> categories() {
            return List.of(new NettyNio(), new AwsCrtAsync());
        }
    }

    public static final class AllNonCrt implements CategoryGroup<HttpClient.Type> {
        @Override
        public List<Category<HttpClient.Type>> categories() {
            return List.of(new Apache(), new Apache5(), new UrlConnection(), new AwsCrt(),
                    new NettyNio(), new AwsCrtAsync());
        }
    }
}
