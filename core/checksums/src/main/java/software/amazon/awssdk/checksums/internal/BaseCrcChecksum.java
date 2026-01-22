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

package software.amazon.awssdk.checksums.internal;

import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Base class for CRC related checksums
 */
@SdkInternalApi
public abstract class BaseCrcChecksum implements SdkChecksum {

    private Checksum checksum;
    private Checksum lastMarkedChecksum;

    public BaseCrcChecksum(Checksum checksum) {
        this.checksum = checksum;
    }

    public Checksum getChecksum() {
        return checksum;
    }

    @Override
    public void mark(int readLimit) {
        this.lastMarkedChecksum = cloneChecksum(checksum);
    }

    @Override
    public void update(int b) {
        checksum.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        checksum.update(b, off, len);
    }

    @Override
    public long getValue() {
        return checksum.getValue();
    }

    @Override
    public void reset() {
        if (lastMarkedChecksum == null) {
            checksum.reset();
        } else {
            checksum = cloneChecksum(lastMarkedChecksum);
        }
    }

    abstract Checksum cloneChecksum(Checksum checksum);
}
