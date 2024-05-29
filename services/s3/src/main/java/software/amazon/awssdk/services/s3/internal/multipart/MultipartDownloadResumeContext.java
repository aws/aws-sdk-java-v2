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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public class MultipartDownloadResumeContext {

    private final SortedSet<Integer> completedParts;

    public MultipartDownloadResumeContext() {
        this.completedParts = new TreeSet<>();
    }

    public List<Integer> completedParts() {
        return Arrays.asList(completedParts.toArray(new Integer[0]));
    }

    public void addCompletedPart(int partNumber) {
        completedParts.add(partNumber);
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
                       .build();
    }
}
