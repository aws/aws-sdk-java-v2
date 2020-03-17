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

package software.amazon.awssdk.codegen.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

public final class Jackson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final ObjectMapper FAIL_ON_UNKNOWN_PROPERTIES_MAPPER = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private static final ObjectWriter WRITER = MAPPER
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .writerWithDefaultPrettyPrinter();

    private Jackson() {
    }

    public static <T> T load(Class<T> clazz, File file) throws IOException {
        return load(clazz, new FileInputStream(file));
    }

    public static <T> T load(Class<T> clazz, File file, boolean failOnUnknownProperties) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            if (failOnUnknownProperties) {
                return FAIL_ON_UNKNOWN_PROPERTIES_MAPPER.readValue(fis, clazz);
            } else {
                return load(clazz, fis);
            }
        }
    }

    public static <T> T load(Class<T> clazz, InputStream is) throws IOException {
        return MAPPER.readValue(is, clazz);
    }

    public static <T> T load(Class<T> clazz, String fileLocation) throws IOException {
        InputStream is = Jackson.class.getClassLoader().getResourceAsStream(
            fileLocation);
        return MAPPER.readValue(is, clazz);
    }

    public static void write(Object value, Writer w) throws IOException {
        WRITER.writeValue(w, value);
    }

}
