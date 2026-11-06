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

package software.amazon.awssdk.s3.wiretap.server.helpers.put;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.s3.wiretap.client.Payload;

@Accessors(fluent = true)
public final class UploadedParts {

    static final int UNPAGINATED = Integer.MAX_VALUE;

    @Getter
    private final Payload payload;

    @Getter
    private final int partSize;

    @Getter
    private final int pageSize;

    private final List<Part> parts;

    private UploadedParts(Payload payload, int partSize, List<Integer> partNumbers, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, was " + pageSize);
        }
        int totalParts = totalParts(payload, partSize);
        this.payload = payload;
        this.partSize = partSize;
        this.pageSize = pageSize;

        List<Part> built = new ArrayList<>(partNumbers.size());
        int previous = 0;
        for (int number : partNumbers) {
            if (number < 1 || number > totalParts) {
                throw new IllegalArgumentException("part number must be in [1, " + totalParts
                        + "] for a " + payload.size() + "-byte payload at part size " + partSize
                        + ", was " + number);
            }
            if (number <= previous) {
                throw new IllegalArgumentException(
                        "part numbers must be strictly ascending, saw " + number + " after "
                                + previous + " — S3 lists parts in partNumber order");
            }
            previous = number;
            int offset = (number - 1) * partSize;
            int length = Math.min(partSize, payload.size() - offset);
            built.add(new Part(number, offset, length, payload.crc32Base64(offset, length)));
        }
        this.parts = List.copyOf(built);
    }

    public static UploadedParts of(Payload payload, int partSize, int completedParts) {
        int totalParts = totalParts(payload, partSize);
        if (completedParts < 0 || completedParts > totalParts) {
            throw new IllegalArgumentException("completedParts must be in [0, " + totalParts
                    + "] for a " + payload.size() + "-byte payload at part size " + partSize
                    + ", was " + completedParts);
        }
        List<Integer> numbers = new ArrayList<>(completedParts);
        for (int number = 1; number <= completedParts; number++) {
            numbers.add(number);
        }
        return new UploadedParts(payload, partSize, numbers, UNPAGINATED);
    }

    public static UploadedParts ofPartNumbers(Payload payload, int partSize, int... partNumbers) {
        List<Integer> numbers = new ArrayList<>(partNumbers.length);
        for (int number : partNumbers) {
            numbers.add(number);
        }
        return new UploadedParts(payload, partSize, numbers, UNPAGINATED);
    }

    public UploadedParts withPageSize(int pageSize) {
        List<Integer> numbers = new ArrayList<>(parts.size());
        for (Part part : parts) {
            numbers.add(part.partNumber());
        }
        return new UploadedParts(payload, partSize, numbers, pageSize);
    }

    public List<Part> parts() {
        return parts;
    }

    public int completedParts() {
        return parts.size();
    }

    public int totalParts() {
        return totalParts(payload, partSize);
    }

    private static int totalParts(Payload payload, int partSize) {
        if (partSize <= 0) {
            throw new IllegalArgumentException("partSize must be positive, was " + partSize);
        }
        return (payload.size() + partSize - 1) / partSize;
    }

    public record Part(int partNumber, int offset, int size, String crc32Base64) { }
}
