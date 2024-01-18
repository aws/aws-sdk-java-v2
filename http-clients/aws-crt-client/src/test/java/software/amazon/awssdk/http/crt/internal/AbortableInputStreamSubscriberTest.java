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

package software.amazon.awssdk.http.crt.internal;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.crt.internal.response.AbortableInputStreamSubscriber;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

@ExtendWith(MockitoExtension.class)
public class AbortableInputStreamSubscriberTest {

    private AbortableInputStreamSubscriber abortableInputStreamSubscriber;

    @Mock
    private Runnable onClose;

    @BeforeEach
    void setUp() {
        abortableInputStreamSubscriber = new AbortableInputStreamSubscriber(onClose, new InputStreamSubscriber());
    }

    @Test
    void close_shouldInvokeOnClose() {
        abortableInputStreamSubscriber.close();
        verify(onClose).run();
    }

    @Test
    void abort_shouldInvokeOnClose() {
        abortableInputStreamSubscriber.abort();
        verify(onClose).run();
    }
}
