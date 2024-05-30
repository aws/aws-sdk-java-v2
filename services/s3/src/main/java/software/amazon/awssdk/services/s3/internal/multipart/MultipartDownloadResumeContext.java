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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ToString;

/**
 * This class keep tracks of the state of a multipart dwnload across multipar part GET requests.
 */
@SdkInternalApi
public class MultipartDownloadResumeContext {

    /**
     * Keeps track of complete parts in a list sorted in ascending order
     */
    private final SortedSet<Integer> completedParts;

    /**
     * Keep track of the byte index to the last byte of the last completed part
     */
    private Long bytesToLastCompletedParts = 0L;

    public MultipartDownloadResumeContext() {
        this.completedParts = new TreeSet<>();
    }

    public List<Integer> completedParts() {
        return Arrays.asList(completedParts.toArray(new Integer[0]));
    }

    public Long bytesToLastCompletedParts() {
        return bytesToLastCompletedParts;
    }

    public void addCompletedPart(int partNumber) {
        completedParts.add(partNumber);
    }

    public void addToBytesToLastCompletedParts(long bytes) {
        bytesToLastCompletedParts += bytes;
    }

    /**
     * Return the highest sequentially completed part, 0 means no parts completed
     * @return
     */
    public int highestSequentialCompletedPart() {
        if (completedParts.isEmpty() || completedParts.first() != 1) {
            return 0;
        }
        if (completedParts.size() == 1) {
            return 1;
        }

        int previous = completedParts.first();
        for (Integer i : completedParts) {
            if (i - previous > 1) {
                return previous;
            }
            previous = i;
        }
        return completedParts.last();
    }

    @Override
    public String toString() {
        return ToString.builder("MultipartDownloadContext")
                       .add("completedParts", completedParts)
                       .add("bytesToLastCompletedParts", bytesToLastCompletedParts)
                       .build();
    }
}
