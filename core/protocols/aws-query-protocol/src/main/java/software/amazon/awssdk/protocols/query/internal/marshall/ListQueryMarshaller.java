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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.TraitType;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

/**
 * Marshaller for list types.
 */
@SdkInternalApi
public class ListQueryMarshaller implements QueryMarshaller<List<?>> {
    private static final PathResolver AWS_QUERY_PATH_RESOLVER = (path, i, listTrait) ->
        listTrait.isFlattened() ?
        path + "." + (i + 1) :
        path + "." + listTrait.memberFieldInfo().locationName() + "." + (i + 1);
    private static final PathResolver EC2_QUERY_PATH_RESOLVER = (path, i, listTrait) -> path + "." + (i + 1);

    private static final EmptyListMarshaller AWS_QUERY_EMPTY_LIST_MARSHALLER =
        (context, path) -> context.request().putRawQueryParameter(path, "");
    private static final EmptyListMarshaller EC2_QUERY_EMPTY_LIST_MARSHALLER = (context, path) -> { };

    private final PathResolver pathResolver;
    private final EmptyListMarshaller emptyListMarshaller;

    private ListQueryMarshaller(PathResolver pathResolver, EmptyListMarshaller emptyListMarshaller) {
        this.pathResolver = pathResolver;
        this.emptyListMarshaller = emptyListMarshaller;
    }

    @Override
    public void marshall(QueryMarshallerContext context, String path, List<?> val, SdkField<List<?>> sdkField) {
        // Explicitly empty lists are marshalled as a query param with empty value in AWS/Query, but not in EC2/Query
        if (val.isEmpty() && !(val instanceof SdkAutoConstructList)) {
            emptyListMarshaller.marshall(context, path);
            return;
        }
        for (int i = 0; i < val.size(); i++) {
            ListTrait listTrait = sdkField.getTrait(ListTrait.class, TraitType.LIST_TRAIT);
            String listPath = pathResolver.resolve(path, i, listTrait);
            QueryMarshaller<Object> marshaller = context.marshallerRegistry().getMarshaller(
                ((SdkField<?>) listTrait.memberFieldInfo()).marshallingType(), val);
            marshaller.marshall(context, listPath, val.get(i), listTrait.memberFieldInfo());
        }
    }

    @FunctionalInterface
    private interface PathResolver {

        String resolve(String path, int i, ListTrait listTrait);
    }

    @FunctionalInterface
    private interface EmptyListMarshaller {
        void marshall(QueryMarshallerContext context, String path);
    }

    /**
     * Wires up the {@link ListQueryMarshaller} with a {@link PathResolver} that respects the flattened trait.
     *
     * @return ListQueryMarshaller.
     */
    public static ListQueryMarshaller awsQuery() {
        return new ListQueryMarshaller(AWS_QUERY_PATH_RESOLVER, AWS_QUERY_EMPTY_LIST_MARSHALLER);
    }

    /**
     * Wires up the {@link ListQueryMarshaller} with a {@link PathResolver} that always flattens lists. The EC2 protocol
     * always flattens lists for inputs even when the 'flattened' trait is not present.
     *
     * @return ListQueryMarshaller.
     */
    public static ListQueryMarshaller ec2Query() {
        return new ListQueryMarshaller(EC2_QUERY_PATH_RESOLVER, EC2_QUERY_EMPTY_LIST_MARSHALLER);
    }
}
