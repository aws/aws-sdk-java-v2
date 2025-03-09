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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class UpgradeTransferManagerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-transfer-manager-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.ChangeTransferManagerTypes"),
                         new V1BuilderVariationsToV2Builder(),
                         new NewClassToBuilderPattern());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void transferManagerConstructors() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.transfer.TransferManager;\n" +
                "import com.amazonaws.services.s3.transfer.TransferManagerBuilder;\n" +
                "\n" +
                "class Test {\n" +
                "    static void tm() {\n" +
                "        TransferManager tm = new TransferManager();\n" +
                "        TransferManager tmBuilderDefault = TransferManagerBuilder.defaultTransferManager();\n" +
                "        TransferManager tmBuilderWithS3 = TransferManagerBuilder.standard().build();\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.transfer.s3.S3TransferManager;\n" +
                "\n" +
                "class Test {\n" +
                "    static void tm() {\n" +
                "        S3TransferManager tm = S3TransferManager.builder()\n" +
                "                .build();\n" +
                "        S3TransferManager tmBuilderDefault = S3TransferManager.create();\n" +
                "        S3TransferManager tmBuilderWithS3 = S3TransferManager.builder().build();\n" +
                "    }\n" +
                "}\n"
            )
        );
    }
}
