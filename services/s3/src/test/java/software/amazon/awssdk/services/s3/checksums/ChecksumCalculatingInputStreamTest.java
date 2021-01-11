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

package software.amazon.awssdk.services.s3.checksums;

import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.checksums.SdkChecksum;

/**
 * Tests for {@link ChecksumCalculatingInputStream}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChecksumCalculatingInputStreamTest {
    @Mock
    private InputStream mockStream;

    @Mock
    private SdkChecksum mockChecksum;

    @Test
    public void closeMethodClosesWrappedStream() throws IOException {
        ChecksumCalculatingInputStream is = new ChecksumCalculatingInputStream(mockStream, mockChecksum);
        is.close();
        verify(mockStream).close();
    }
}
