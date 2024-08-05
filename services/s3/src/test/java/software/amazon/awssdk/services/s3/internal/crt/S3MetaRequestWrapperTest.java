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

package software.amazon.awssdk.services.s3.internal.crt;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.s3.S3MetaRequest;

@ExtendWith(MockitoExtension.class)
public class S3MetaRequestWrapperTest {

    @Mock
    private S3MetaRequest request;

    private S3MetaRequestWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new S3MetaRequestWrapper(request);
    }

    @Test
    void close_concurrentCalls_onlyExecuteOnce() {
        CompletableFuture.allOf(CompletableFuture.runAsync(() -> wrapper.close()),
                                CompletableFuture.runAsync(() -> wrapper.close())).join();
        Mockito.verify(request, Mockito.times(1)).close();
    }

    @Test
    void incrementWindow_afterClose_shouldBeNoOp() {
        wrapper.close();
        wrapper.incrementReadWindow(10L);
        Mockito.verify(request, Mockito.times(1)).close();
        Mockito.verify(request, Mockito.never()).incrementReadWindow(Mockito.anyLong());
    }

    @Test
    void pause_afterClose_shouldBeNoOp() {
        wrapper.close();
        wrapper.pause();
        Mockito.verify(request, Mockito.times(1)).close();
        Mockito.verify(request, Mockito.never()).pause();
    }

    @Test
    void cancel_afterClose_shouldBeNoOp() {
        wrapper.close();
        wrapper.cancel();
        Mockito.verify(request, Mockito.times(1)).close();
        Mockito.verify(request, Mockito.never()).cancel();
    }
}
