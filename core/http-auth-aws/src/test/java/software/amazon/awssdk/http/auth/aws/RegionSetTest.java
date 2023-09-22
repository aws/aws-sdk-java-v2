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

package software.amazon.awssdk.http.auth.aws;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;

public class RegionSetTest {

    private static final List<String> stringList = Arrays.asList(
        "*", "us-west-2", "us-east-1,us-west-2", " us-west-2 ", " us-east-1, us-west-2  ", " a,b  ,c ,d ,e ,f ,  g "
    );

    private static final List<Collection<String>> collectionList = Arrays.asList(
        Arrays.asList("us-west-1"), Arrays.asList("us-west-1", "us-west-2"), Arrays.asList("us-west-2", "us-west-2"),
        Arrays.asList("*"), Arrays.asList("us-west-2", "us-west-2", "*")
    );

    private static List<String> stringInputs() {
        return stringList;
    }

    private static List<Collection<String>> collectionInputs() {
        return collectionList;
    }

    @ParameterizedTest
    @MethodSource("stringInputs")
    public void create_withStringInput_succeeds(String input) {
        RegionSet.create(input);
    }

    @ParameterizedTest
    @MethodSource("collectionInputs")
    public void create_withCollectionInput_succeeds(Collection<String> input) {
        RegionSet.create(input);
    }
}
