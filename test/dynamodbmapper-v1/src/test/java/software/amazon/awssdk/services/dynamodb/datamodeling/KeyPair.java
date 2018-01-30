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

package software.amazon.awssdk.services.dynamodb.datamodeling;

public class KeyPair {
    private Object hashKey;
    private Object rangeKey;

    public KeyPair withHashKey(Object hashkey) {
        this.hashKey = hashkey;
        return this;
    }

    public KeyPair withRangeKey(Object rangeKey) {
        this.rangeKey = rangeKey;
        return this;
    }

    public Object getHashKey() {
        return this.hashKey;
    }

    public void setHashKey(Object hashKey) {
        this.hashKey = hashKey;
    }

    public Object getRangeKey() {
        return this.rangeKey;
    }

    public void setRangeKey(Object rangeKey) {
        this.rangeKey = rangeKey;
    }
}
