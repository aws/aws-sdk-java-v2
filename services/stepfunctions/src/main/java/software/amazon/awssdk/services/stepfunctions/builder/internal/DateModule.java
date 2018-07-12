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

package software.amazon.awssdk.services.stepfunctions.builder.internal;

import static software.amazon.awssdk.utils.DateUtils.formatIso8601Date;
import static software.amazon.awssdk.utils.DateUtils.parseIso8601Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Date;

/**
 * Contains Jackson module for serializing dates to ISO8601 format per the <a href="https://states-language.net/spec.html#timestamps">spec</a>.
 */
public final class DateModule {

    public static final SimpleModule INSTANCE = new SimpleModule();

    static {
        INSTANCE.addSerializer(Date.class, new StdSerializer<Date>(Date.class) {
            @Override
            public void serialize(Date date,
                                  JsonGenerator jsonGenerator,
                                  SerializerProvider serializerProvider) throws
                                                                         IOException {
                jsonGenerator.writeString(formatIso8601Date(date.toInstant()));
            }
        });
        INSTANCE.addDeserializer(Date.class, new StdDeserializer<Date>(Date.class) {
            @Override
            public Date deserialize(JsonParser jsonParser,
                                    DeserializationContext deserializationContext) throws IOException {

                return fromJson(jsonParser.getValueAsString());
            }
        });
    }

    private DateModule() {
    }

    public static Date fromJson(String jsonText) {
        return Date.from(parseIso8601Date(jsonText));
    }

}
