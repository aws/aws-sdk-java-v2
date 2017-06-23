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

package software.amazon.awssdk.codegen.poet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static software.amazon.awssdk.codegen.poet.PoetUtils.buildJavaFile;

import java.io.IOException;
import java.util.Random;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ComparisonFailure;
import software.amazon.awssdk.codegen.emitters.CodeTransformer;
import software.amazon.awssdk.codegen.emitters.JavaCodeFormatter;
import software.amazon.awssdk.codegen.emitters.UnusedImportRemover;
import software.amazon.awssdk.utils.IoUtils;

public final class PoetMatchers {

    private static final CodeTransformer processor = CodeTransformer.chain(new CopyrightRemover(),
                                                                           new JavaCodeFormatter());

    public static Matcher<ClassSpec> generatesTo(String expectedTestFile) {
        return new TypeSafeMatcher<ClassSpec>() {
            @Override
            protected boolean matchesSafely(ClassSpec spec) {
                String expectedClass = getExpectedClass(spec, expectedTestFile);
                String actualClass = generateClass(spec);
                try {
                    assertThat(actualClass, equalToIgnoringWhiteSpace(expectedClass));
                } catch (AssertionError e) {
                    //Unfortunately for string comparisons Hamcrest doesn't really give us a nice diff. On the other hand
                    //IDEs know how to nicely display JUnit's ComparisonFailure - makes debugging tests much easier
                    throw new ComparisonFailure("Output class does not match expected", expectedClass, actualClass);
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                //Since we bubble an exception this will never actually get called
            }
        };
    }

    private static String getExpectedClass(ClassSpec spec, String testFile) {
        try {
            return processor.apply(IoUtils.toString(spec.getClass().getResourceAsStream(testFile)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateClass(ClassSpec spec) {
        StringBuilder output = new StringBuilder();
        try {
            buildJavaFile(spec).writeTo(output);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate class", e);
        }
        return processor.apply(output.toString());
    }

    private static class CopyrightRemover implements CodeTransformer {
        @Override
        public String apply(String input) {
            return input.substring(input.indexOf("package"));
        }
    }
}
