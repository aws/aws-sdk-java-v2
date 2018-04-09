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

package software.amazon.awssdk.core.runtime;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Used in combination with the generated member copiers to implement deep
 * copies of shape members.
 */
@SdkInternalApi
public final class StandardMemberCopier {

    private StandardMemberCopier() {
    }

    public static String copy(String s) {
        return s;
    }

    public static Short copy(Short s) {
        return s;
    }

    public static Integer copy(Integer i) {
        return i;
    }

    public static Long copy(Long l) {
        return l;
    }

    public static Float copy(Float f) {
        return f;
    }

    public static Double copy(Double d) {
        return d;
    }

    public static BigDecimal copy(BigDecimal bd) {
        return bd;
    }

    public static Boolean copy(Boolean b) {
        return b;
    }

    public static InputStream copy(InputStream is) {
        return is;
    }

    public static Instant copy(Instant i) {
        return i;
    }

    public static ByteBuffer copy(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }

        return ByteBuffer.wrap(BinaryUtils.copyBytesFrom(bb)).asReadOnlyBuffer();
    }
}
