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

package software.amazon.awssdk.s3.wiretap.testarchive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.s3.wiretap.client.feature.CategoryGroup;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Features.class)
public @interface Feature {

    Class<? extends software.amazon.awssdk.s3.wiretap.client.feature.Feature<?>> feature();

    Class<? extends Category<?>>[] values() default { };

    Class<? extends CategoryGroup<?>> group() default NoGroup.class;

    final class NoGroup implements CategoryGroup<Object> {
        @Override
        public List<Category<Object>> categories() {
            throw new UnsupportedOperationException("sentinel");
        }
    }
}
