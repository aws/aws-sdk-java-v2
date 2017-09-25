/*
 * Copyright (c) 2017. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.jsonprotocoltests.model;


import javax.annotation.Generated;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class EnumType {
    private EnumType(EnumType.BuilderImpl builder) {
    }

    @Override
    public EnumType.Builder toBuilder() {
        return new EnumType.BuilderImpl(this);
    }

    public static EnumType.Builder builder() {
        return new EnumType.BuilderImpl();
    }

    public static Class<? extends EnumType.Builder> serializableBuilderClass() {
        return EnumType.BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EnumType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }
}