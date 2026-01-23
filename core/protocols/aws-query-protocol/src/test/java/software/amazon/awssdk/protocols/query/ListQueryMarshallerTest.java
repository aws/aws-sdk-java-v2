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

package software.amazon.awssdk.protocols.query;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.query.internal.marshall.ListQueryMarshaller;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryMarshaller;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryMarshallerContext;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryMarshallerRegistry;


public class ListQueryMarshallerTest {

    @Test
    public void localeSetAsNe_ParsedCorrectly() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("ne"));

        ListQueryMarshaller marshaller = ListQueryMarshaller.ec2Query();

        QueryMarshallerContext context = createTestContext();
        List<String> testList = Arrays.asList("value1", "value2");
        SdkField<List<?>> field = createTestField();

        marshaller.marshall(context, "TestParam", testList, field);

        Map<String, List<String>> params = context.request().rawQueryParameters();

        assertEquals(Collections.singletonList("value1"), params.get("TestParam.1"));
        assertEquals(Collections.singletonList("value2"), params.get("TestParam.2"));
        Locale.setDefault(defaultLocale);
    }

    private QueryMarshallerContext createTestContext() {

        QueryMarshaller<String> stringMarshaller = (context, path, val, field) ->
            context.request().putRawQueryParameter(path, val);

        QueryMarshallerRegistry registry = QueryMarshallerRegistry.builder()
                                                                  .marshaller(MarshallingType.STRING, stringMarshaller)
                                                                  .build();

        return QueryMarshallerContext.builder()
                                     .request(SdkHttpFullRequest.builder())
                                     .marshallerRegistry(registry)
                                     .build();
    }



    private SdkField<List<?>> createTestField() {
        SdkField<?> memberField = SdkField.builder(MarshallingType.STRING)
                                          .memberName("item")
                                          .traits(LocationTrait.builder()
                                                               .location(MarshallLocation.PAYLOAD)
                                                               .locationName("item")
                                                               .build())
                                          .build();

        ListTrait listTrait = ListTrait.builder()
                                       .memberFieldInfo(memberField)
                                       .memberLocationName("item")
                                       .isFlattened(true)
                                       .build();

        return SdkField.builder(MarshallingType.LIST)
                       .memberName("TestParam")
                       .traits(LocationTrait.builder()
                                            .location(MarshallLocation.QUERY_PARAM)
                                            .locationName("TestParam")
                                            .build(),listTrait)
                       .build();
    }
}
