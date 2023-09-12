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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testimmutables;

public class EntityEnvelopeImmutable<T> {
    private final T entity;

    public EntityEnvelopeImmutable(T entity) {
        this.entity = entity;
    }

    public T entity() {
        return this.entity;
    }

    public static class Builder<T> {
        private T entity;

        public void setEntity(T entity) {
            this.entity = entity;
        }

        public EntityEnvelopeImmutable<T> build() {
            return new EntityEnvelopeImmutable<>(this.entity);
        }
    }
}

