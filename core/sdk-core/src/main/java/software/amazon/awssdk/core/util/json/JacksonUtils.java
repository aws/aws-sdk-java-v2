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

package software.amazon.awssdk.core.util.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeCodec;
import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import com.fasterxml.jackson.jr.stree.JrsValue;
import java.io.IOException;
import java.io.Writer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkProtectedApi
public final class JacksonUtils {
    private static final TreeCodec TREE_CODEC = new JacksonJrsTreeCodec();
    private static final JSON OBJECT_MAPPER = JSON.builder().register(JacksonAnnotationExtension.std)
            .treeCodec(TREE_CODEC).build();
    private static final JSON PRETTY_WRITER =
            JSON.builder().treeCodec(TREE_CODEC).register(JacksonAnnotationExtension.std)
                    .enable(JSON.Feature.PRETTY_PRINT_OUTPUT).build();

    private JacksonUtils() {
    }

    public static String toJsonPrettyString(Object value) {
        try {
            return PRETTY_WRITER.asString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String toJsonString(Object value) {
        try {
            return OBJECT_MAPPER.asString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the deserialized object from the given json string and target
     * class; or null if the given json string is null.
     */
    public static <T> T fromJsonString(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.beanFrom(clazz, json);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to parse Json String.").cause(e).build();
        }
    }

    /**
     * Returns the deserialized object from the given json string and target
     * class; or null if the given json string is null. Clears the JSON location in the event of an error
     */
    public static <T> T fromSensitiveJsonString(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.beanFrom(clazz, json);
        } catch (Exception e) {
            // If underlying exception is a json parsing issue, clear out the location so that the exception message
            // does not contain the raw json
            if (e instanceof JsonProcessingException) {
                ((JsonProcessingException) e).clearLocation();
            }
            throw SdkClientException.builder().message("Unable to parse Json String.").cause(e).build();
        }
    }

    public static JrsValue jsonNodeOf(String json) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeFrom(json);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to parse Json String.").cause(e).build();
        }
    }

    public static JrsValue sensitiveJsonNodeOf(String json) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeFrom(json);
        } catch (Exception e) {
            // If underlying exception is a json parsing issue, clear out the location so that the exception message
            // does not contain the raw json
            if (e instanceof JsonProcessingException) {
                ((JsonProcessingException) e).clearLocation();
            }
            throw SdkClientException.builder().message("Unable to parse Json String.").cause(e).build();
        }
    }

    public static JsonGenerator jsonGeneratorOf(Writer writer) throws IOException {
        return new JsonFactory().createGenerator(writer);
    }
}
