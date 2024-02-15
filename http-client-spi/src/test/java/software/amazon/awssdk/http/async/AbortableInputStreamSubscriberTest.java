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

package software.amazon.awssdk.http.async;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

@ExtendWith(MockitoExtension.class)
public class AbortableInputStreamSubscriberTest {

    private AbortableInputStreamSubscriber abortableInputStreamSubscriber;

    @Mock
    private Runnable onClose;

    @Mock
    private Runnable onAbort;

    @Mock
    private InputStreamSubscriber inputStreamSubscriber;

    @BeforeEach
    void setUp() {
        abortableInputStreamSubscriber = new AbortableInputStreamSubscriber(AbortableInputStreamSubscriber.builder()
                                                                                                          .doAfterClose(onClose)
                                                                                                          .doAfterAbort(onAbort),
                                                                            inputStreamSubscriber);


    }

    @Test
    void close_closeConfigured_shouldInvokeOnClose() {
        abortableInputStreamSubscriber.close();
        verify(inputStreamSubscriber).close();
        verify(onClose).run();
    }

    @Test
    void abort_abortConfigured_shouldInvokeDoAfterAbort() {
        abortableInputStreamSubscriber.abort();
        verify(onAbort).run();
    }

    @Test
    void abort_abortNotConfigured_shouldInvokeOnClose() {
        abortableInputStreamSubscriber = new AbortableInputStreamSubscriber(AbortableInputStreamSubscriber.builder()
                                                                                                          .doAfterClose(onClose),
                                                                            inputStreamSubscriber);
        abortableInputStreamSubscriber.abort();
        verify(onClose).run();
    }

    @Test
    void close_closeNotConfigured_shouldCloseDelegate() {
        abortableInputStreamSubscriber = new AbortableInputStreamSubscriber(AbortableInputStreamSubscriber.builder(),
                                                                            inputStreamSubscriber);
        abortableInputStreamSubscriber.close();
        verify(inputStreamSubscriber).close();
    }
}
