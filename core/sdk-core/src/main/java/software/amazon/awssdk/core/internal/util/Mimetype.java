/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class that maintains a listing of known Mimetypes, and determines the
 * mimetype of files based on file extensions.
 * <p>
 * This class is obtained with the {#link {@link #getInstance()} method that
 * recognizes loaded mime types from the file <code>mime.types</code> if this
 * file is available at the root of the classpath. The mime.types file format,
 * and most of the content, is taken from the Apache HTTP server's mime.types
 * file.
 * <p>
 * The format for mime type setting documents is:
 * <code>mimetype + extension (+ extension)*</code>. Any
 * blank lines in the file are ignored, as are lines starting with
 * <code>#</code> which are considered comments.
 *
 * @see <a href="https://github.com/apache/httpd/blob/trunk/docs/conf/mime.types">mime.types</a>
 */
@SdkInternalApi
public final class Mimetype {

    /** The default XML mimetype: application/xml */
    public static final String MIMETYPE_XML = "application/xml";

    /** The default HTML mimetype: text/html */
    public static final String MIMETYPE_HTML = "text/html";

    /** The default binary mimetype: application/octet-stream */
    public static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";

    /** The default gzip mimetype: application/x-gzip */
    public static final String MIMETYPE_GZIP = "application/x-gzip";

    public static final String MIMETYPE_TEXT_PLAIN = "text/plain; charset=UTF-8";

    private static final Logger LOG = LoggerFactory.getLogger(Mimetype.class);

    private static final String MIME_TYPE_PATH = "software/amazon/awssdk/core/util/mime.types";

    private static final ClassLoader CLASS_LOADER = ClassLoaderHelper.classLoader();

    private static volatile Mimetype mimetype;

    /**
     * Map that stores file extensions as keys, and the corresponding mimetype as values.
     */
    private final Map<String, String> extensionToMimetype = new HashMap<>();

    private Mimetype() {
        Optional.ofNullable(CLASS_LOADER).map(loader -> loader.getResourceAsStream(MIME_TYPE_PATH)).ifPresent(
            stream -> {
                try {
                    loadAndReplaceMimetypes(stream);
                } catch (IOException e) {
                    LOG.debug("Failed to load mime types from file in the classpath: mime.types", e);
                } finally {
                    IoUtils.closeQuietly(stream, null);
                }
            }
        );
    }

    /**
     * Loads MIME type info from the file 'mime.types' in the classpath, if it's available.
     */
    public static Mimetype getInstance() {
        if (mimetype == null) {
            synchronized (Mimetype.class) {
                if (mimetype == null) {
                    mimetype = new Mimetype();
                }
            }
        }

        return mimetype;
    }

    /**
     * Determines the mimetype of a file by looking up the file's extension in an internal listing
     * to find the corresponding mime type. If the file has no extension, or the extension is not
     * available in the listing contained in this class, the default mimetype
     * <code>application/octet-stream</code> is returned.
     * <p>
     * A file extension is one or more characters that occur after the last period (.) in the file's name.
     * If a file has no extension,
     * Guesses the mimetype of file data based on the file's extension.
     *
     * @param path the file whose extension may match a known mimetype.
     * @return the file's mimetype based on its extension, or a default value of
     * <code>application/octet-stream</code> if a mime type value cannot be found.
     */
    public String getMimetype(Path path) {
        Validate.notNull(path, "path");
        Path file = path.getFileName();

        if (file != null) {
            return getMimetype(file.toString());
        }
        return null;
    }

    /**
     * Determines the mimetype of a file by looking up the file's extension in an internal listing
     * to find the corresponding mime type. If the file has no extension, or the extension is not
     * available in the listing contained in this class, the default mimetype
     * <code>application/octet-stream</code> is returned.
     * <p>
     * A file extension is one or more characters that occur after the last period (.) in the file's name.
     * If a file has no extension,
     * Guesses the mimetype of file data based on the file's extension.
     *
     * @param file the file whose extension may match a known mimetype.
     * @return the file's mimetype based on its extension, or a default value of
     * <code>application/octet-stream</code> if a mime type value cannot be found.
     */
    public String getMimetype(File file) {
        return getMimetype(file.toPath());
    }

    /**
     * Determines the mimetype of a file by looking up the file's extension in
     * an internal listing to find the corresponding mime type. If the file has
     * no extension, or the extension is not available in the listing contained
     * in this class, the default mimetype <code>application/octet-stream</code>
     * is returned.
     * <p>
     * A file extension is one or more characters that occur after the last
     * period (.) in the file's name. If a file has no extension, Guesses the
     * mimetype of file data based on the file's extension.
     *
     * @param fileName The name of the file whose extension may match a known
     * mimetype.
     * @return The file's mimetype based on its extension, or a default value of
     * {@link #MIMETYPE_OCTET_STREAM} if a mime type value cannot
     * be found.
     */
    String getMimetype(String fileName) {
        int lastPeriodIndex = fileName.lastIndexOf('.');
        if (lastPeriodIndex > 0 && lastPeriodIndex + 1 < fileName.length()) {
            String ext = StringUtils.lowerCase(fileName.substring(lastPeriodIndex + 1));
            if (extensionToMimetype.containsKey(ext)) {
                return extensionToMimetype.get(ext);
            }
        }
        return MIMETYPE_OCTET_STREAM;
    }

    /**
     * Reads and stores the mime type setting corresponding to a file extension, by reading
     * text from an InputStream. If a mime type setting already exists when this method is run,
     * the mime type value is replaced with the newer one.
     */
    private void loadAndReplaceMimetypes(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        br.lines().filter(line -> !line.startsWith("#")).forEach(line -> {
            line = line.trim();

            StringTokenizer st = new StringTokenizer(line, " \t");
            if (st.countTokens() > 1) {
                String mimetype = st.nextToken();
                while (st.hasMoreTokens()) {
                    String extension = st.nextToken();
                    extensionToMimetype.put(StringUtils.lowerCase(extension), mimetype);
                }
            }
        });
    }
}
