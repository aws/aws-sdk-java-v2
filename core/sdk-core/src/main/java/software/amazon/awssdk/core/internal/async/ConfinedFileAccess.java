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

package software.amazon.awssdk.core.internal.async;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Reads the attributes of, and opens, a file below a root directory without following a symbolic link in any path component
 * below that root.
 *
 * <p>Where the file system supports {@link SecureDirectoryStream}, every component is resolved relative to the descriptor of
 * its parent directory, with {@link LinkOption#NOFOLLOW_LINKS}, so a symbolic link at any level makes the call fail.
 * Otherwise, every intermediate component is checked to be a directory without following links, and the file is opened with
 * {@link LinkOption#NOFOLLOW_LINKS}.
 *
 * <p>Which mode is used depends on the default file system provider. OpenJDK's provider on Linux returns a
 * {@link SecureDirectoryStream}. The macOS and Windows providers do not, so they use the per-component checks.
 *
 * <p>The root directory itself, and its ancestors, are trusted.
 */
@SdkInternalApi
final class ConfinedFileAccess {
    private static final Logger log = Logger.loggerFor(ConfinedFileAccess.class);

    private static final Set<OpenOption> READ_NOFOLLOW_OPTIONS =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)));

    /**
     * Not instantiable. All methods are static.
     */
    private ConfinedFileAccess() {
    }

    /**
     * Reads the attributes of {@code path} without following a symbolic link in any component below {@code root}.
     *
     * @throws IOException if a component is a symbolic link, the file is not a regular file, or the file cannot be read.
     */
    static BasicFileAttributes readAttributes(Path root, Path path) throws IOException {
        List<Path> names = relativeNames(root, path);
        Path fileName = names.get(names.size() - 1);

        List<SecureDirectoryStream<Path>> opened = new ArrayList<>();
        try {
            SecureDirectoryStream<Path> parent = openParent(root, names, opened);
            if (parent == null) {
                return checkedAttributesWithoutSecureStream(root, path, names);
            }
            return checkedAttributes(path, readAttributes(parent, fileName));
        } finally {
            closeAll(opened);
        }
    }

    /**
     * Opens {@code path} for reading without following a symbolic link in any component below {@code root}. The returned
     * channel is blocking, so the caller must not read from it on a thread that must not block.
     *
     * @throws IOException if a component is a symbolic link, the file is not a regular file, or the file cannot be opened.
     */
    static FileChannel openForRead(Path root, Path path) throws IOException {
        List<Path> names = relativeNames(root, path);
        Path fileName = names.get(names.size() - 1);

        List<SecureDirectoryStream<Path>> opened = new ArrayList<>();
        try {
            SecureDirectoryStream<Path> parent = openParent(root, names, opened);
            if (parent == null) {
                checkedAttributesWithoutSecureStream(root, path, names);
                return FileChannel.open(path, READ_NOFOLLOW_OPTIONS);
            }

            checkedAttributes(path, readAttributes(parent, fileName));
            SeekableByteChannel channel = parent.newByteChannel(fileName, READ_NOFOLLOW_OPTIONS);
            if (!(channel instanceof FileChannel)) {
                channel.close();
                throw new IOException("Unable to open " + path + " for reading: the file system did not return a file "
                                      + "channel.");
            }
            return (FileChannel) channel;
        } finally {
            closeAll(opened);
        }
    }

    /**
     * Splits {@code path} into its name components below {@code root}, in order from the first directory below the root to
     * the file name. The path is not normalized.
     *
     * @throws IllegalArgumentException if {@code path} does not start with {@code root}, names {@code root} itself, or has an
     * empty, {@code "."} or {@code ".."} component below the root.
     */
    private static List<Path> relativeNames(Path root, Path path) {
        if (root == null || path == null || !path.startsWith(root) || path.getNameCount() <= root.getNameCount()) {
            throw new IllegalArgumentException("Path " + path + " is not below the directory " + root);
        }
        // Take the names below the root from the path as given. Path.relativize normalizes on Java 9 and later.
        Path relative = path.subpath(root.getNameCount(), path.getNameCount());
        // "." and ".." components are rejected, even when the path they name is below the root, because each component is
        // resolved one at a time. uploadDirectory only passes paths from its walk, which never hold these components.
        List<Path> names = new ArrayList<>(relative.getNameCount());
        for (Path name : relative) {
            String nameString = name.toString();
            if (nameString.isEmpty() || ".".equals(nameString) || "..".equals(nameString)) {
                throw new IllegalArgumentException("Path " + path + " has a \".\" or \"..\" component below the directory "
                                                   + root + ", which is not supported");
            }
            names.add(name);
        }
        return names;
    }

    /**
     * Opens the root and every intermediate directory, without following links below the root. Returns the stream of the
     * file's parent directory, or {@code null} if the file system does not support {@link SecureDirectoryStream}. Every
     * stream opened is added to {@code opened}, so the caller can close it.
     */
    @SuppressWarnings("unchecked")
    private static SecureDirectoryStream<Path> openParent(Path root, List<Path> names,
                                                          List<SecureDirectoryStream<Path>> opened) throws IOException {
        DirectoryStream<Path> rootStream = Files.newDirectoryStream(root);
        if (!(rootStream instanceof SecureDirectoryStream)) {
            rootStream.close();
            return null;
        }

        SecureDirectoryStream<Path> current = (SecureDirectoryStream<Path>) rootStream;
        opened.add(current);
        for (int i = 0; i < names.size() - 1; i++) {
            current = current.newDirectoryStream(names.get(i), LinkOption.NOFOLLOW_LINKS);
            opened.add(current);
        }
        return current;
    }

    /**
     * Reads the attributes of {@code fileName} relative to the open directory {@code parent}, without following a symbolic
     * link. For a link, the attributes returned are those of the link itself.
     *
     * @throws IOException if the file system has no basic attribute view for the entry, or the attributes cannot be read.
     */
    private static BasicFileAttributes readAttributes(SecureDirectoryStream<Path> parent, Path fileName) throws IOException {
        BasicFileAttributeView view = parent.getFileAttributeView(fileName, BasicFileAttributeView.class,
                                                                  LinkOption.NOFOLLOW_LINKS);
        if (view == null) {
            throw new IOException("Unable to read the attributes of " + fileName);
        }
        return view.readAttributes();
    }

    /**
     * Fallback for a file system without {@link SecureDirectoryStream}. Checks, by path and without following links, that
     * every intermediate component below {@code root} is a directory and that {@code path} is a regular file, and returns the
     * file's attributes.
     *
     * @throws IOException if an intermediate component is not a directory, the file is not a regular file, or a component
     * is a symbolic link.
     */
    private static BasicFileAttributes checkedAttributesWithoutSecureStream(Path root, Path path, List<Path> names)
            throws IOException {
        Path current = root;
        for (int i = 0; i < names.size() - 1; i++) {
            current = current.resolve(names.get(i));
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class,
                                                                  LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory()) {
                throw new IOException(current + " is not a directory or is a symbolic link");
            }
        }
        return checkedAttributes(path, Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
    }

    /**
     * Returns {@code attributes} if they describe a regular file. Attributes read without following links describe a
     * symbolic link as a link, so a link also fails this check.
     *
     * @throws IOException if {@code attributes} do not describe a regular file.
     */
    private static BasicFileAttributes checkedAttributes(Path path, BasicFileAttributes attributes) throws IOException {
        if (!attributes.isRegularFile()) {
            throw new IOException(path + " is not a regular file or is a symbolic link");
        }
        return attributes;
    }

    /**
     * Closes every stream in {@code streams}. A failure to close one stream is logged at debug level and does not stop the
     * others from being closed.
     */
    private static void closeAll(List<SecureDirectoryStream<Path>> streams) {
        for (SecureDirectoryStream<Path> stream : streams) {
            try {
                stream.close();
            } catch (IOException e) {
                log.debug(() -> "Failed to close directory stream", e);
            }
        }
    }
}
