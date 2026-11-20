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

package software.amazon.awssdk.s3.mockserver.invariants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;

public final class Destination {
    private final Path path;
    private final Map<Path, byte[]> initial;

    public Destination(Path path, Map<Path, byte[]> initial) {
        this.path = path;
        this.initial = initial;
    }

    public static Destination of(Path path) {
        Map<Path, byte[]> initial = new LinkedHashMap<>();
        for (Path file : filesUnder(path)) {
            try {
                initial.put(file, Files.readAllBytes(file));
            } catch (IOException e) {
                throw new UncheckedIOException("could not read download destination " + file, e);
            }
        }
        return new Destination(path, Immutable.copyOfMap(initial));
    }

    public byte[] initialContent(Path file) {
        return initial.getOrDefault(file, new byte[0]);
    }

    public static List<Path> filesUnder(Path path) {
        if (Files.isRegularFile(path)) {
            return Immutable.listOf(path);
        }
        if (!Files.isDirectory(path)) {
            return Immutable.listOf();
        }
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).sorted().collect(Immutable.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("could not list download destination " + path, e);
        }
    }

    public Path path() {
        return path;
    }

    public Map<Path, byte[]> initial() {
        return initial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Destination that = (Destination) o;
        return Objects.equals(path, that.path)
               && Objects.equals(initial, that.initial);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(path);
        result = 31 * result + Objects.hashCode(initial);
        return result;
    }

    @Override
    public String toString() {
        return "Destination[path=" + path + ", initial=" + initial + "]";
    }
}
