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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

@SdkInternalApi
public final class MapQueryUnmarshaller implements QueryUnmarshaller<Map<String, ?>> {

    @Override
    public Map<String, ?> unmarshall(QueryUnmarshallerContext context, List<XmlElement> content, SdkField<Map<String, ?>> field) {
        Map<String, Object> map = new HashMap<>();
        MapTrait mapTrait = field.getTrait(MapTrait.class);
        SdkField mapValueSdkField = mapTrait.valueFieldInfo();

        getEntries(content, mapTrait).forEach(entry -> {
            XmlElement key = entry.getElementByName(mapTrait.keyLocationName());
            XmlElement value = entry.getElementByName(mapTrait.valueLocationName());
            QueryUnmarshaller unmarshaller = context.getUnmarshaller(mapValueSdkField.location(),
                                                                     mapValueSdkField.marshallingType());
            map.put(key.textContent(),
                    unmarshaller.unmarshall(context, singletonList(value), mapValueSdkField));
        });
        return map;
    }

    private List<XmlElement> getEntries(List<XmlElement> content, MapTrait mapTrait) {
        return mapTrait.isFlattened() ?
               content :
               content.get(0).getElementsByName("entry");
    }
}
