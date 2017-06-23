/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.utils;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

/**
 * Simple struct of two values, possibly of different types.
 *
 * @param <LeftT>  Left type
 * @param <RightT> Right Type
 */
public final class Pair<LeftT, RightT> {

    private final LeftT left;
    private final RightT right;

    public Pair(LeftT left, RightT right) {
        this.left = paramNotNull(left, "left");
        this.right = paramNotNull(right, "right");
    }

    /**
     * @return Left value
     */
    public LeftT left() {
        return this.left;
    }

    /**
     * @return Right value
     */
    public RightT right() {
        return this.right;
    }
}
