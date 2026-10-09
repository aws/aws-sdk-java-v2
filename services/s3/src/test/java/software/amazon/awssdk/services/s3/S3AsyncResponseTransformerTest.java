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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

class S3AsyncResponseTransformerTest {
    private static final String FACTORY_CLASS = "software.amazon.awssdk.services.s3.S3AsyncResponseTransformer";
    private static final String SENTINEL_CLASS =
        "software.amazon.awssdk.services.s3.internal.crt.S3CrtBorrowedBufferResponseTransformer";

    @Test
    void borrowedFactory_withoutCrtClasses_shouldLoadAndFailClearlyWhenExecuted() throws Exception {
        // Load the sentinel locally to detect accidental CRT linkage from the public factory.
        NoCrtClassLoader classLoader = new NoCrtClassLoader(getClass().getClassLoader(),
                                                            FACTORY_CLASS, SENTINEL_CLASS);
        Class<?> factory = Class.forName(FACTORY_CLASS, true, classLoader);
        Object transformer = factory.getMethod("toBlockingInputStreamWithBorrowedBuffers").invoke(null);

        assertThat(transformer.getClass().getClassLoader()).isSameAs(classLoader);
        assertThatThrownBy(() -> invokePrepare(transformer))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("S3 CRT async client");
        assertThat(classLoader.crtClassRequested()).isFalse();
    }

    @Test
    void borrowedFactory_withNonCrtClient_shouldFailBeforeHttpExecution() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             S3AsyncClient client = S3AsyncClient.builder()
                                                 .region(Region.US_EAST_1)
                                                 .httpClient(httpClient)
                                                 .build()) {
            assertThatThrownBy(() -> client.getObject(GetObjectRequest.builder()
                                                                     .bucket("bucket")
                                                                     .key("key")
                                                                     .build(),
                                                       S3AsyncResponseTransformer
                                                           .toBlockingInputStreamWithBorrowedBuffers()).join())
                .hasCauseInstanceOf(UnsupportedOperationException.class);
            assertThat(httpClient.getRequests()).isEmpty();
        }
    }

    @Test
    void borrowedFactory_withForwardingWrapper_shouldFailBeforeHttpExecution() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             S3AsyncClient client = S3AsyncClient.builder()
                                                 .region(Region.US_EAST_1)
                                                 .httpClient(httpClient)
                                                 .build()) {
            AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> transformer =
                S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers();
            assertThatThrownBy(() -> client.getObject(GetObjectRequest.builder()
                                                                     .bucket("bucket")
                                                                     .key("key")
                                                                     .build(),
                                                       new ForwardingTransformer(transformer)).join())
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("This transformer requires an S3 CRT async client with a configured direct buffer pool");
            assertThat(httpClient.getRequests()).isEmpty();
        }
    }

    @Test
    void borrowedFactory_withMultipartNonCrtClient_shouldFailBeforeHttpExecution() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             S3AsyncClient client = S3AsyncClient.builder()
                                                 .region(Region.US_EAST_1)
                                                 .httpClient(httpClient)
                                                 .multipartEnabled(true)
                                                 .build()) {
            assertThatThrownBy(() -> client.getObject(GetObjectRequest.builder()
                                                                     .bucket("bucket")
                                                                     .key("key")
                                                                     .build(),
                                                       S3AsyncResponseTransformer
                                                           .toBlockingInputStreamWithBorrowedBuffers()).join())
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("This transformer requires an S3 CRT async client with a configured direct buffer pool");
            assertThat(httpClient.getRequests()).isEmpty();
        }
    }

    private static final class ForwardingTransformer
        implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> {
        private final AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> delegate;

        private ForwardingTransformer(
            AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
            return delegate.prepare();
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
        }
    }

    private static void invokePrepare(Object transformer) {
        ((AsyncResponseTransformer<?, ?>) transformer).prepare();
    }

    private static final class NoCrtClassLoader extends ClassLoader {
        private final Set<String> localClasses;
        private boolean crtClassRequested;

        private NoCrtClassLoader(ClassLoader parent, String... localClasses) {
            super(parent);
            this.localClasses = new HashSet<>(Arrays.asList(localClasses));
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (name.startsWith("software.amazon.awssdk.crt.")) {
                    crtClassRequested = true;
                    throw new ClassNotFoundException(name);
                }
                if (localClasses.contains(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineLocalClass(name);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
                return super.loadClass(name, resolve);
            }
        }

        private Class<?> defineLocalClass(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream input = getParent().getResourceAsStream(resource)) {
                if (input == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = readAllBytes(input);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        private boolean crtClassRequested() {
            return crtClassRequested;
        }

        private static byte[] readAllBytes(InputStream input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
