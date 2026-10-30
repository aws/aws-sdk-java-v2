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

package software.amazon.awssdk.s3.wiretap.client.feature;

import java.util.List;
import java.util.Set;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilder;
import software.amazon.awssdk.s3.wiretap.client.ClientKind;

public interface Feature<T> {

    String name();

    Class<T> valueType();

    void apply(ClientBuilder builder, Category<T> category);

    Set<ClientKind> supportedKinds();

    List<Category<T>> allCategories();

    Category<T> defaultCategory(Class<?> paramType);

    default boolean alwaysShowInLabel() {
        return false;
    }
}
