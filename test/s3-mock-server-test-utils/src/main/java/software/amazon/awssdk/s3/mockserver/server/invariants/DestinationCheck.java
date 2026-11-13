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

package software.amazon.awssdk.s3.mockserver.server.invariants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.invariants.Destination;
import software.amazon.awssdk.s3.mockserver.invariants.Invariant;
import software.amazon.awssdk.s3.mockserver.invariants.Observation;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.s3.mockserver.server.CapturedExchange;
import software.amazon.awssdk.s3.mockserver.server.CapturedRequest;
import software.amazon.awssdk.s3.mockserver.server.Request;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;
import software.amazon.awssdk.s3.mockserver.server.ServedVersion;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.GetMockResponders;

public final class DestinationCheck {

    static final int BACKTRACK = 16;

    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes\\s+(\\d+)-\\d+/");

    private static final int CHUNK = 64 * 1024;

    private DestinationCheck() {
    }

    public static void check(Destination destination, List<ServedVersion> served, List<CapturedExchange> history,
                             ObservationSink sink) {
        if (served.isEmpty()) {
            return;
        }
        Map<String, Set<Long>> windows = new HashMap<>();
        for (Path file : Destination.filesUnder(destination.path())) {
            byte[] after = readIfPresent(file);
            if (after == null) {
                continue;
            }
            sink.checked(Invariant.DESTINATION_HOLDS_ONE_VERSION);
            String problem = judge(file, after, destination.initialContent(file), served,
                    key -> windows.computeIfAbsent(key, k -> windowStarts(k, history)));
            if (problem != null) {
                sink.record(new Observation(Invariant.DESTINATION_HOLDS_ONE_VERSION, problem));
            }
        }
    }

    public static List<Observation> observe(Destination destination, List<ServedVersion> served,
                                            List<CapturedExchange> history) {
        ObservationSink scratch = ObservationSink.detached("DestinationCheck.observe");
        try {
            check(destination, served, history, scratch);
        } finally {
            scratch.close();
        }
        return scratch.observations();
    }

    private static byte[] readIfPresent(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException gone) {
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException("could not read download destination " + file, e);
        }
    }

    private static String judge(Path file, byte[] after, byte[] before, List<ServedVersion> served,
                                Function<String, Set<Long>> windowsOf) {
        byte[] prior = before.length >= after.length ? before : Arrays.copyOf(before, after.length);
        int firstChange = ByteArrays.mismatch(after, prior);
        if (firstChange < 0 || firstChange >= after.length) {
            return null;
        }
        String best = "no served version matches even its first byte";
        int bestEnd = -1;
        for (ServedVersion version : served) {
            Set<Long> windows = windowsOf.apply(version.key());
            Set<Long> raised = version.altered() ? windows : Immutable.setOf();
            Set<Integer> starts = new TreeSet<>(Comparator.reverseOrder());
            for (int start = firstChange; start >= Math.max(0, firstChange - BACKTRACK); start--) {
                starts.add(start);
            }
            starts.add(0);
            for (int start : starts) {
                Set<Long> offsets = new TreeSet<>(windows);
                offsets.add(0L);
                offsets.add((long) start);
                for (long offset : offsets) {
                    if (offset >= version.content().size()) {
                        continue;
                    }
                    int end = start + matchLength(after, start, version.content(), offset, raised);
                    if (end <= firstChange) {
                        continue;
                    }
                    boolean restIsPrior = ByteArrays.rangeEquals(after, end, after.length, prior, end, after.length);
                    if (end == after.length || restIsPrior) {
                        return null;
                    }
                    if (end > bestEnd) {
                        bestEnd = end;
                        best = "the longest match is " + version.key() + " (" + version.content().size()
                                + " bytes) from offset " + offset + " written at file byte " + start
                                + ", which stops at file byte " + end;
                    }
                }
            }
        }
        return file + " (" + after.length + " bytes, " + before.length + " before the download) holds no single"
                + " contiguous range of one served version where it changed from byte " + firstChange + "; " + best;
    }

    private static int matchLength(byte[] after, int start, Payload content, long offset, Set<Long> raised) {
        int limit = (int) Math.min(after.length - start, content.size() - offset);
        int matched = 0;
        while (matched < limit) {
            int n = Math.min(CHUNK, limit - matched);
            byte[] expected = content.toByteArray((int) (offset + matched), n);
            for (int i = 0; i < n; i++) {
                byte actual = after[start + matched + i];
                if (actual != expected[i]
                        && !(raised.contains(offset + matched + i) && actual == (byte) (expected[i] + 1))) {
                    return matched + i;
                }
            }
            matched += n;
        }
        return matched;
    }

    private static Set<Long> windowStarts(String key, List<CapturedExchange> history) {
        Set<Long> starts = new TreeSet<>();
        for (CapturedExchange e : history) {
            CapturedRequest r = e.request();
            if (!S3RequestFilters.getObject().test(r) || e.response() == null
                    || e.response().statusCode() / 100 != 2 || !namesKey(r, key)) {
                continue;
            }
            String range = e.response().optionalHeader("Content-Range")
                    .orElseGet(() -> e.response().optionalHeader("x-amz-content-range").orElse(null));
            Matcher m = range == null ? null : CONTENT_RANGE.matcher(range);
            starts.add(m != null && m.find() ? Long.parseLong(m.group(1)) : 0L);
        }
        return starts;
    }

    private static boolean namesKey(CapturedRequest r, String key) {
        return GetMockResponders.matchesKey(Request.builder().method(r.method()).path(r.path()).headers(r.headers())
                .build(), key);
    }
}
