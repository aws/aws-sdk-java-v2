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

package software.amazon.awssdk.v2migration;

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


public class ChangeRegionTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-region-types.yml")) {
            spec.recipe(stream, "software.amazon.awssdk.v2migration.ChangeRegionTypes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeRegions() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-core")),
            java(
                "import com.amazonaws.regions.Regions;\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        Regions region = Regions.fromName(\"us-east-1\");\n" +
                "        Regions region2 = Regions.US_EAST_1;\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.regions.Region;\n" +
                "\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        Region region = Region.of(\"us-east-1\");\n" +
                "        Region region2 = Region.US_EAST_1;\n" +
                "    }\n" +
                "}\n"
            )
        );
    }
}
