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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TableKeysAndAttributesTest {

    @Test
    public void testNameSetConsistency() {
        TableKeysAndAttributes t = new TableKeysAndAttributes("myTable")
                .withHashAndRangeKeys(
                        // specify the hash key name and range key name once
                        "foo", "bar",
                        // followed by multiple values
                        123, 1,
                        123, 2,
                        456, 1,
                        456, 2,
                        456, 3);
        List<PrimaryKey> keys = t.getPrimaryKeys();
        Assert.assertTrue(5 == keys.size());
        for (PrimaryKey key : keys) {
            Assert.assertTrue(key.getComponentNameSet().contains("foo"));
            Assert.assertTrue(key.getComponentNameSet().contains("bar"));
            System.out.println(key);
        }
        System.out.println(keys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameSetInConsistency() {
        new TableKeysAndAttributes("myTable")
                .withPrimaryKeys(
                        new PrimaryKey("foo", 123, "bar", 345),
                        new PrimaryKey("foo", 123, "ba", 345));
    }
}
