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

package software.amazon.awssdk.benchmark.core;

public enum ObjectSize {

    SMALL(1024L * 1024),
    MEDIUM(8L * 1024 * 1024),
    LARGE(64L * 1024 * 1024),;

    private final long sizeInBytes;

    ObjectSize(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public long sizeInBytes() {
        return sizeInBytes;
    }
}
