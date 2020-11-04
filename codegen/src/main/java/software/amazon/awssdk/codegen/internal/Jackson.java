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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import com.fasterxml.jackson.jr.stree.JrsValue;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public final class Jackson {
    private static final JSON MAPPER;
    private static final JSON FAIL_ON_UNKNOWN_PROPERTIES_MAPPER;
    private static volatile ObjectWriter OBJECT_MAPPER;

    static {
        JSON.Builder mapperBuilder = JSON.builder()
                                         .disable(JSON.Feature.FAIL_ON_UNKNOWN_BEAN_PROPERTY)
                                         .enable(JSON.Feature.PRETTY_PRINT_OUTPUT)
                                         .enable(JSON.Feature.READ_JSON_ARRAYS_AS_JAVA_ARRAYS)
                                         .treeCodec(new JacksonJrsTreeCodec());

        mapperBuilder.streamFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);

        MAPPER = mapperBuilder.build();

        mapperBuilder.enable(JSON.Feature.FAIL_ON_UNKNOWN_BEAN_PROPERTY);

        FAIL_ON_UNKNOWN_PROPERTIES_MAPPER = mapperBuilder.build();
    }

    private Jackson() {
    }

    public static JrsValue readJrsValue(String input) throws IOException {
        return MAPPER.beanFrom(JrsValue.class, input);
    }

    public static <T> T load(Class<T> clazz, File file) throws IOException {
        return MAPPER.beanFrom(clazz, file);
    }

    public static <T> T load(Class<T> clazz, File file, boolean failOnUnknownProperties) throws IOException {
        if (failOnUnknownProperties) {
            return FAIL_ON_UNKNOWN_PROPERTIES_MAPPER.beanFrom(clazz, file);
        } else {
            return load(clazz, file);
        }
    }

    public static void writeWithObjectMapper(Object value, Writer w) throws IOException {
        if (OBJECT_MAPPER == null) {
            synchronized (Jackson.class) {
                if (OBJECT_MAPPER == null) {
                    OBJECT_MAPPER = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                                                      .writerWithDefaultPrettyPrinter();
                }
            }
        }

        OBJECT_MAPPER.writeValue(w, value);
    }

}
