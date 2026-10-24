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

package software.amazon.awssdk.s3.wiretap.testarchive.downloadStreams;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.FileTransformerConfiguration.FailureBehavior;
import software.amazon.awssdk.core.FileTransformerConfiguration.FileWriteOption;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.s3.wiretap.client.ConfiguredClient;
import software.amazon.awssdk.s3.wiretap.client.HttpClient;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllAsync;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllSync;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.S3TestBase;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class GetBodyToFileTest extends S3TestBase {

    private static final Logger LOG = LogManager.getLogger(GetBodyToFileTest.class);

    private static final String KEY = "get-body-to-file/object";

    private static final int OBJECT_SIZE = 1024 * 1024;

    private static final int EXISTING_SIZE = 6 * 1024 * 1024;

    private static final int POSITION = 4 * 1024;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = StoredObject.Shape.SINGLE_PUT_CRC32, size = OBJECT_SIZE)
    @Feature(feature = HttpClientFeature.class, group = AllSync.class)
    @Feature(feature = RetryFeature.class)
    private @interface SyncFileSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = StoredObject.Shape.SINGLE_PUT_CRC32, size = OBJECT_SIZE)
    @Feature(feature = HttpClientFeature.class, group = AllAsync.class)
    @Feature(feature = RetryFeature.class)
    private @interface AsyncFileSweep {
    }

    @SyncFileSweep
    void syncGetObjectToPath(ConfiguredClient c, StoredObject object) throws IOException {
        S3Client s3 = c.client();
        walkDestinationStates(c, object, "sync getObject(request, Path)", true,
                (request, file) -> s3.getObject(request, file));
    }

    @SyncFileSweep
    void syncTransformerToFilePath(ConfiguredClient c, StoredObject object) throws IOException {
        S3Client s3 = c.client();
        walkDestinationStates(c, object, "sync ResponseTransformer.toFile(Path)", false,
                (request, file) -> s3.getObject(request, ResponseTransformer.toFile(file)));
    }

    @SyncFileSweep
    void syncTransformerToFileObject(ConfiguredClient c, StoredObject object) throws IOException {
        S3Client s3 = c.client();
        walkDestinationStates(c, object, "sync ResponseTransformer.toFile(File)", false,
                (request, file) -> s3.getObject(request, ResponseTransformer.toFile(file.toFile())));
    }

    @AsyncFileSweep
    void asyncGetObjectToPath(ConfiguredClient c, StoredObject object) throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkDestinationStates(c, object, "async getObject(request, Path)", true,
                (request, file) -> s3.getObject(request, file).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFilePath(ConfiguredClient c, StoredObject object) throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkDestinationStates(c, object, "async toFile(Path)", false, (request, file) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file)).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFileObject(ConfiguredClient c, StoredObject object) throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkDestinationStates(c, object, "async toFile(File)", false, (request, file) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file.toFile())).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFilePathWithConfiguration(ConfiguredClient c, StoredObject object)
            throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkWriteOptions(c, object, "async toFile(Path, config)", (request, file, config) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file, config)).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFileObjectWithConfiguration(ConfiguredClient c, StoredObject object)
            throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkWriteOptions(c, object, "async toFile(File, config)", (request, file, config) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file.toFile(), config)).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFilePathWithConsumer(ConfiguredClient c, StoredObject object)
            throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkWriteOptions(c, object, "async toFile(Path, consumer)", (request, file, config) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file,
                        builder -> applyTo(builder, config))).join());
    }

    @AsyncFileSweep
    void asyncTransformerToFileObjectWithConsumer(ConfiguredClient c, StoredObject object)
            throws IOException {
        S3AsyncClient s3 = c.asyncClient();
        walkWriteOptions(c, object, "async toFile(File, consumer)", (request, file, config) ->
                s3.getObject(request, AsyncResponseTransformer.toFile(file.toFile(),
                        builder -> applyTo(builder, config))).join());
    }

    private interface Download {
        void into(GetObjectRequest request, Path file) throws Exception;
    }

    private interface ConfiguredDownload {
        void into(GetObjectRequest request, Path file, FileTransformerConfiguration config)
                throws Exception;
    }

    private enum ObjectState {
        PRESENT,
        MISSING
    }

    private enum Destination {
        ABSENT,
        EXISTS_EMPTY,
        EXISTS_NON_EMPTY
    }

    private enum Outcome {
        WROTE_OBJECT,
        REPLACED_EXISTING,
        APPENDED_TO_EXISTING,
        WROTE_AT_POSITION,
        SPLICED_INTO_EXISTING,
        REFUSED_FILE_KEPT,
        REFUSED_FILE_DELETED,
        REFUSED_NO_SUCH_FILE,
        MISSING_OBJECT_DESTINATION_UNTOUCHED,
        MISSING_OBJECT_FILE_DELETED,
        MISSING_OBJECT_FILE_TRUNCATED,
        MISSING_OBJECT_STRAY_FILE_CREATED,
        UNCLASSIFIED
    }

    private void walkDestinationStates(ConfiguredClient c, StoredObject object, String what,
                                       boolean convenienceOverload, Download download)
            throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (ObjectState objectState : ObjectState.values()) {
            for (Destination destination : Destination.values()) {
                runOne(c, object, what, destination, objectState, null, convenienceOverload,
                        download, mismatches);
            }
        }
        reportAll(mismatches);
    }

    private void walkWriteOptions(ConfiguredClient c, StoredObject object, String what,
                                  ConfiguredDownload download) throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (FileWriteOption writeOption : FileWriteOption.values()) {
            for (FailureBehavior failureBehavior : FailureBehavior.values()) {
                FileTransformerConfiguration config = configuration(writeOption, failureBehavior);
                String label = what + " " + writeOption + "/" + failureBehavior;
                for (ObjectState objectState : ObjectState.values()) {
                    for (Destination destination : Destination.values()) {
                        runOne(c, object, label, destination, objectState, config, false,
                                (request, file) -> download.into(request, file, config),
                                mismatches);
                    }
                }
            }
        }
        reportAll(mismatches);
    }

    private static void reportAll(List<String> mismatches) {
        assertTrue(mismatches.isEmpty(),
                () -> mismatches.size() + " of the grid disagreed with the model:\n  "
                        + String.join("\n  ", mismatches));
    }

    private static FileTransformerConfiguration configuration(FileWriteOption writeOption,
                                                              FailureBehavior failureBehavior) {
        FileTransformerConfiguration.Builder builder = FileTransformerConfiguration.builder()
                .fileWriteOption(writeOption)
                .failureBehavior(failureBehavior);
        if (writeOption == FileWriteOption.WRITE_TO_POSITION) {
            builder.position((long) POSITION);
        }
        return builder.build();
    }

    private void runOne(ConfiguredClient c, StoredObject object, String what,
                        Destination destination, ObjectState objectState,
                        FileTransformerConfiguration config, boolean convenienceOverload,
                        Download download, List<String> mismatches) throws IOException {
        Path directory = Files.createTempDirectory("wiretap-get-to-file-");
        Path file = directory.resolve("destination.bin");
        byte[] before = seed(file, destination);

        if (objectState == ObjectState.PRESENT) {
            GetMockResponders.register(wiretap(), KEY, object);
        } else {
            GetMockResponders.registerMissing(wiretap(), KEY);
        }
        Throwable raised = null;
        try {
            download.into(GetObjectRequest.builder().bucket(bucket()).key(KEY).build(), file);
        } catch (Throwable t) {
            raised = rootCause(t);
        }

        Outcome expected = expected(config, destination, objectState, c, convenienceOverload);
        String where = c.config().describe() + " | " + what + " | object=" + objectState
                + " destination=" + destination;
        String actual = describe(raised, file);
        try {
            Outcome observed = observe(raised, file, before, object);
            if (observed != expected) {
                mismatches.add(where + ": expected " + expected + ", observed " + observed
                        + " (" + actual + ")");
            } else {
                mismatches.addAll(byteProblems(where, observed, file, before, object));
            }
            if (objectState == ObjectState.MISSING) {
                boolean typed = raised instanceof NoSuchKeyException;
                if (typed == isManagedCrt(c)) {
                    mismatches.add(where + ": expected the 404 to be "
                            + (isManagedCrt(c) ? "an untyped S3Exception" : "a NoSuchKeyException")
                            + ", got " + actual);
                }
            }
        } finally {
            LOG.info("{} -> {}", where, actual);
            Payload.deleteQuietly(file);
            Files.deleteIfExists(directory);
            wiretap().clearExchanges();
            wiretap().clearPendingResponses();
        }
    }

    private static byte[] seed(Path file, Destination state) throws IOException {
        return switch (state) {
            case ABSENT -> null;
            case EXISTS_EMPTY -> {
                Files.write(file, new byte[0]);
                yield new byte[0];
            }
            case EXISTS_NON_EMPTY -> {
                byte[] bytes = new byte[EXISTING_SIZE];
                Arrays.fill(bytes, (byte) 'X');
                Files.write(file, bytes);
                yield bytes;
            }
        };
    }

    private static Outcome expected(FileTransformerConfiguration config, Destination destination,
                                    ObjectState objectState, ConfiguredClient c,
                                    boolean convenienceOverload) {
        FileWriteOption writeOption =
                config == null ? FileWriteOption.CREATE_NEW : config.fileWriteOption();
        if (objectState == ObjectState.MISSING) {
            return missingObjectOutcome(destination, c, convenienceOverload);
        }

        return switch (writeOption) {
            case CREATE_OR_REPLACE_EXISTING ->
                    destination == Destination.ABSENT ? Outcome.WROTE_OBJECT
                            : Outcome.REPLACED_EXISTING;

            case CREATE_OR_APPEND_TO_EXISTING -> switch (destination) {
                case ABSENT -> Outcome.WROTE_OBJECT;
                case EXISTS_EMPTY -> Outcome.REPLACED_EXISTING;
                case EXISTS_NON_EMPTY -> Outcome.APPENDED_TO_EXISTING;
            };

            case WRITE_TO_POSITION -> switch (destination) {
                case ABSENT -> Outcome.REFUSED_NO_SUCH_FILE;
                case EXISTS_EMPTY -> Outcome.WROTE_AT_POSITION;
                case EXISTS_NON_EMPTY -> Outcome.SPLICED_INTO_EXISTING;
            };

            case CREATE_NEW -> createNewOutcome(destination, c, convenienceOverload);
        };
    }

    private static Outcome missingObjectOutcome(Destination destination, ConfiguredClient c,
                                                boolean convenienceOverload) {
        if (isManagedCrt(c) && convenienceOverload && destination != Destination.ABSENT) {
            return Outcome.REFUSED_FILE_KEPT;
        }
        return Outcome.MISSING_OBJECT_DESTINATION_UNTOUCHED;
    }

    private static Outcome createNewOutcome(Destination destination, ConfiguredClient c,
                                            boolean convenienceOverload) {
        if (destination == Destination.ABSENT) {
            return Outcome.WROTE_OBJECT;
        }

        return Outcome.REFUSED_FILE_KEPT;
    }

    private static Outcome observe(Throwable raised, Path file, byte[] before, StoredObject object)
            throws IOException {
        if (raised instanceof FileAlreadyExistsException) {
            return Files.exists(file) ? Outcome.REFUSED_FILE_KEPT : Outcome.REFUSED_FILE_DELETED;
        }
        if (raised instanceof NoSuchFileException) {
            return Outcome.REFUSED_NO_SUCH_FILE;
        }
        if (raised instanceof S3Exception s3Error && s3Error.statusCode() == 404) {
            if (before == null) {
                return Files.notExists(file) ? Outcome.MISSING_OBJECT_DESTINATION_UNTOUCHED
                        : Outcome.MISSING_OBJECT_STRAY_FILE_CREATED;
            }
            if (Files.notExists(file)) {
                return Outcome.MISSING_OBJECT_FILE_DELETED;
            }
            byte[] onDisk = Files.readAllBytes(file);
            if (Arrays.equals(before, onDisk)) {
                return Outcome.MISSING_OBJECT_DESTINATION_UNTOUCHED;
            }
            return onDisk.length == 0 ? Outcome.MISSING_OBJECT_FILE_TRUNCATED : Outcome.UNCLASSIFIED;
        }
        if (raised != null || Files.notExists(file)) {
            return Outcome.UNCLASSIFIED;
        }
        byte[] onDisk = Files.readAllBytes(file);
        byte[] wholeObject = object.payload().toByteArray(0, object.size());
        if (Arrays.equals(onDisk, wholeObject)) {
            return before == null ? Outcome.WROTE_OBJECT : Outcome.REPLACED_EXISTING;
        }
        if (before != null && Arrays.equals(onDisk, concat(before, wholeObject))) {
            return Outcome.APPENDED_TO_EXISTING;
        }
        if (onDisk.length == POSITION + object.size()
                && Arrays.equals(Arrays.copyOfRange(onDisk, POSITION, onDisk.length), wholeObject)) {
            return Outcome.WROTE_AT_POSITION;
        }
        if (before != null && onDisk.length == before.length && Arrays.equals(
                Arrays.copyOfRange(onDisk, POSITION, POSITION + object.size()), wholeObject)) {
            return Outcome.SPLICED_INTO_EXISTING;
        }
        return Outcome.UNCLASSIFIED;
    }

    private static List<String> byteProblems(String where, Outcome observed, Path file,
                                             byte[] before, StoredObject object) throws IOException {
        byte[] wholeObject = object.payload().toByteArray(0, object.size());
        List<String> problems = new ArrayList<>();
        switch (observed) {
            case WROTE_AT_POSITION -> {
                byte[] onDisk = Files.readAllBytes(file);
                if (!Arrays.equals(wholeObject,
                        Arrays.copyOfRange(onDisk, POSITION, onDisk.length))) {
                    problems.add(where + ": the object should start at byte " + POSITION);
                }
            }
            case APPENDED_TO_EXISTING -> {
                long expectedLength = before.length + (long) object.size();
                long actualLength = Files.size(file);
                if (expectedLength != actualLength) {
                    problems.add(where + ": expected previous + the whole object (" + expectedLength
                            + " bytes, longer than the object), found " + actualLength);
                }
            }
            case SPLICED_INTO_EXISTING -> {
                byte[] onDisk = Files.readAllBytes(file);
                if (before.length != onDisk.length) {
                    problems.add(where + ": the existing file should keep its length "
                            + before.length + ", found " + onDisk.length);
                } else {
                    if (!Arrays.equals(Arrays.copyOfRange(before, 0, POSITION),
                            Arrays.copyOfRange(onDisk, 0, POSITION))) {
                        problems.add(where
                                + ": the bytes before the position should still be the old file's");
                    }
                    int after = POSITION + object.size();
                    if (!Arrays.equals(Arrays.copyOfRange(before, after, before.length),
                            Arrays.copyOfRange(onDisk, after, onDisk.length))) {
                        problems.add(where
                                + ": the bytes after the object should still be the old file's");
                    }
                }
            }
            default -> {
            }
        }
        return problems;
    }

    private static String describe(Throwable raised, Path file) throws IOException {
        String file0 = Files.exists(file) ? Files.size(file) + " bytes on disk" : "no file";
        return (raised == null ? "nothing raised"
                : raised.getClass().getSimpleName() + "[" + raised.getMessage() + "]")
                + ", " + file0;
    }

    private static boolean isManagedCrt(ConfiguredClient c) {
        return c.value(HttpClientFeature.class) == HttpClient.Type.CRT_CREATE;
    }

    private static void applyTo(FileTransformerConfiguration.Builder builder,
                                FileTransformerConfiguration config) {
        builder.fileWriteOption(config.fileWriteOption())
                .failureBehavior(config.failureBehavior());
        if (config.position() != null) {
            builder.position(config.position());
        }
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] out = new byte[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    private static Throwable rootCause(Throwable thrown) {
        Throwable cause = thrown;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
