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
package software.amazon.awssdk.regions.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.regions.internal.util.RegionScope;

@RunWith(Parameterized.class)
public class RegionScopeTest {

    private final TestCase testCase;

    public RegionScopeTest(TestCase testCase) {
        this.testCase = testCase;
    }

    @Test
    public void validateRegionScope() {
        try {
            RegionScope regionScope = RegionScope.of(testCase.regionScope);
            assertThat(regionScope.id()).isEqualTo(testCase.regionScope);
        } catch (RuntimeException e) {
            if (testCase.expectedException == null) {
                throw e;
            }
            assertThat(e).isInstanceOf(testCase.expectedException);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<TestCase> testCases() {
        List<TestCase> cases = new ArrayList<>();

        cases.add(new TestCase().setCaseName("Standard Region accepted").setRegionScope("eu-north-1"));
        cases.add(new TestCase().setCaseName("Wildcard last segment accepted").setRegionScope("eu-north-*"));
        cases.add(new TestCase().setCaseName("Wildcard middle segment accepted").setRegionScope("eu-*"));
        cases.add(new TestCase().setCaseName("Global Wildcard accepted").setRegionScope("*"));

        cases.add(new TestCase().setCaseName("Null string fails").setRegionScope(null).setExpectedException(NullPointerException.class));

        cases.add(new TestCase().setCaseName("Empty string fails")
                                .setRegionScope("")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Blank string fails")
                                .setRegionScope(" ")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Wildcard mixed last segment fails")
                                .setRegionScope("eu-north-45*")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Wildcard mixed middle segment fails")
                                .setRegionScope("eu-north*")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Wildcard mixed global fails")
                                .setRegionScope("eu*")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Wildcard not at end fails")
                                .setRegionScope("*-north-1")
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("Double wildcard fails")
                                .setRegionScope("**")
                                .setExpectedException(IllegalArgumentException.class));

        return cases;
    }

    private static class TestCase {
        private String caseName;
        private String regionScope;
        private Class<? extends RuntimeException> expectedException;

        public TestCase setCaseName(String caseName) {
            this.caseName = caseName;
            return this;
        }

        public TestCase setRegionScope(String regionScope) {
            this.regionScope = regionScope;
            return this;
        }

        public TestCase setExpectedException(Class<? extends RuntimeException> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        @Override
        public String toString() {
            return this.caseName + (regionScope == null ? "" : ": " + regionScope);
        }
    }

}
