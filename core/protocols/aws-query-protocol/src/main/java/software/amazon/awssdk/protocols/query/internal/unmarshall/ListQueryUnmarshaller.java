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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

@SdkInternalApi
public final class ListQueryUnmarshaller implements QueryUnmarshaller<List<?>> {

    @Override
    public List<?> unmarshall(QueryUnmarshallerContext context, List<XmlElement> content, SdkField<List<?>> field) {
        ListTrait listTrait = field.getTrait(ListTrait.class);
        List<Object> list = new ArrayList<>();
        getMembers(content, listTrait).forEach(member -> {
            QueryUnmarshaller unmarshaller = context.getUnmarshaller(listTrait.memberFieldInfo().location(),
                                                                     listTrait.memberFieldInfo().marshallingType());
            list.add(unmarshaller.unmarshall(context, singletonList(member), listTrait.memberFieldInfo()));
        });
        return list;
    }

    private List<XmlElement> getMembers(List<XmlElement> content, ListTrait listTrait) {
        return listTrait.isFlattened() ?
               content :
               // There have been cases in EC2 where the member name is not modeled correctly so we just grab all
               // direct children instead and don't care about member name. See TT0124273367 for more information.
               content.get(0).children();
    }
}
