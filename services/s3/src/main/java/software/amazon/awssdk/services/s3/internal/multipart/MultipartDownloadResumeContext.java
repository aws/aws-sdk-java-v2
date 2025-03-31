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
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * This class keep tracks of the state of a multipart download across multipart GET requests.
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
    private Long bytesToLastCompletedParts;

    /**
     * The total number of parts of the multipart download.
     */
    private Integer totalParts;

    /**
     * The GetObjectResponse to return to the user.
     */
    private GetObjectResponse response;

    public MultipartDownloadResumeContext() {
        this(new TreeSet<>(), 0L);
    }

    public MultipartDownloadResumeContext(Collection<Integer> completedParts, Long bytesToLastCompletedParts) {
        this.completedParts = new TreeSet<>(Validate.notNull(
            completedParts, "completedParts must not be null"));
        this.bytesToLastCompletedParts = Validate.notNull(
            bytesToLastCompletedParts, "bytesToLastCompletedParts must not be null");
    }

    public List<Integer> completedParts() {
        return Arrays.asList(completedParts.toArray(new Integer[0]));
    }

    public Long bytesToLastCompletedParts() {
        return bytesToLastCompletedParts;
    }

    public void addCompletedPart(int partNumber) {
        System.out.println("Completed part: " + partNumber);
        completedParts.add(partNumber);
    }

    public void addToBytesToLastCompletedParts(long bytes) {
        bytesToLastCompletedParts += bytes;
    }

    public void totalParts(int totalParts) {
        this.totalParts = totalParts;
    }

    public Integer totalParts() {
        return totalParts;
    }

    public GetObjectResponse response() {
        return this.response;
    }

    public void response(GetObjectResponse response) {
        this.response = response;
    }

    /**
     * @return the highest sequentially completed part, 0 means no parts completed. Used for non-sequential operation when parts
     * may have been completed in a non-sequential order. For example, if parts [1, 2, 3, 6, 7, 10] were completed, this
     * method will return 3.
     *
     */
    public int highestSequentialCompletedPart() {
        if (completedParts.isEmpty() || completedParts.first() != 1) {
            return 0;
        }
        if (completedParts.size() == 1) {
            return 1;
        }

        // for sequential operation, make sure we don't skip any non-completed part by returning the
        // highest sequentially completed part
        int previous = completedParts.first();
        for (Integer i : completedParts) {
            if (i - previous > 1) {
                return previous;
            }
            previous = i;
        }
        return completedParts.last();
    }

    /**
     * Check if the multipart download is complete or not by checking if the total amount of downloaded parts is equal to the
     * total amount of parts.
     *
     * @return true if all parts were downloaded, false if not.
     */
    public boolean isComplete() {
        if (totalParts == null) {
            return false;
        }
        return completedParts.size() == totalParts;
    }

    @Override
    public String toString() {
        return ToString.builder("MultipartDownloadContext")
                       .add("completedParts", completedParts)
                       .add("bytesToLastCompletedParts", bytesToLastCompletedParts)
                       .build();
    }
}
