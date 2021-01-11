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

package software.amazon.awssdk.protocols.query.internal.marshall;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.MapTrait;

@SdkInternalApi
public class MapQueryMarshaller implements QueryMarshaller<Map<String, ?>> {

    @Override
    public void marshall(QueryMarshallerContext context, String path, Map<String, ?> val, SdkField<Map<String, ?>> sdkField) {
        MapTrait mapTrait = sdkField.getTrait(MapTrait.class);
        AtomicInteger entryNum = new AtomicInteger(1);
        val.forEach((key, value) -> {

            String mapKeyPath = resolveMapPath(path, mapTrait, entryNum, mapTrait.keyLocationName());

            context.request().putRawQueryParameter(mapKeyPath, key);

            String mapValuePath = resolveMapPath(path, mapTrait, entryNum, mapTrait.valueLocationName());

            QueryMarshaller<Object> marshaller = context.marshallerRegistry()
                .getMarshaller(((SdkField<?>) mapTrait.valueFieldInfo()).marshallingType(), val);
            marshaller.marshall(context, mapValuePath, value, mapTrait.valueFieldInfo());
            entryNum.incrementAndGet();
        });
    }

    private static String resolveMapPath(String path, MapTrait mapTrait, AtomicInteger entryNum, String s) {
        return mapTrait.isFlattened() ?
               String.format("%s.%d.%s", path, entryNum.get(), s) :
               String.format("%s.entry.%d.%s", path, entryNum.get(), s);
    }
}
