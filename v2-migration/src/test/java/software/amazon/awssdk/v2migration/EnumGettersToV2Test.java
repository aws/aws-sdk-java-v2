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
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class EnumGettersToV2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-enum-getters.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.EnumGettersToV2"),
                         new ChangeSdkType(),
                         new NewClassToBuilder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("sns"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeSingleItemEnumGetter() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sns.model.PhoneNumberInformation;\n" +
                "\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        PhoneNumberInformation phoneNumberInformation = new PhoneNumberInformation();\n" +
                "        String routeType = phoneNumberInformation.getRouteType();\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sns.model.PhoneNumberInformation;\n"
                + "\n"
                + "class Test {\n"
                + "    static void method() {\n"
                + "        PhoneNumberInformation phoneNumberInformation = PhoneNumberInformation.builder()\n"
                + "                .build();\n"
                + "        String routeType = phoneNumberInformation.routeTypeAsString();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeCollectionItemEnumGetter() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sns.model.PhoneNumberInformation;\n" +
                "\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        PhoneNumberInformation phoneNumberInformation = new PhoneNumberInformation();\n" +
                "        List<String> numberCapabilities = phoneNumberInformation.getNumberCapabilities();\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sns.model.PhoneNumberInformation;\n"
                + "\n"
                + "class Test {\n"
                + "    static void method() {\n"
                + "        PhoneNumberInformation phoneNumberInformation = PhoneNumberInformation.builder()\n"
                + "                .build();\n"
                + "        List<String> numberCapabilities = phoneNumberInformation.numberCapabilitiesAsStrings();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
