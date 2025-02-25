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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class TransferManagerToV2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new TransferManagerToV2())
            .parser(Java8Parser.builder().classpath("s3"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldTransformImports() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.transfer.Download;\n" +
                "import com.amazonaws.services.s3.transfer.MultipleFileDownload;\n" +
                "import com.amazonaws.services.s3.transfer.MultipleFileUpload;\n" +
                "import com.amazonaws.services.s3.transfer.PersistableDownload;\n" +
                "import com.amazonaws.services.s3.transfer.TransferManager;\n" +
                "import com.amazonaws.services.s3.transfer.TransferManagerBuilder;\n" +
                "import java.io.File;\n" +
                "\n" +
                "class Test {\n" +
                "    static void tm() {\n" +
                "        TransferManager tm = TransferManagerBuilder.defaultTransferManager();\n" +
                "        Download download = tm.download(\"bucket\", \"key\", new File(\"path/to/file.txt\"));\n" +
                "        PersistableDownload persistableDownload = download.pause();\n" +
                "        MultipleFileDownload multipleFileDownload = tm.downloadDirectory(\"bucket\", \"prefix\", new File(\"path/to/dir\"));\n" +
                "        MultipleFileUpload multipleFileUpload = tm.uploadDirectory(\"bucket\", \"prefix\", new File(\"path/to/dir\"), true);\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.transfer.s3.S3TransferManager;\n" +
                "import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;\n" +
                "import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;\n" +
                "import software.amazon.awssdk.transfer.s3.model.Download;\n" +
                "import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;\n" +
                "\n" +
                "import java.io.File;\n" +
                "\n" +
                "class Test {\n" +
                "    static void tm() {\n" +
                "        TransferManager tm = TransferManagerBuilder.defaultTransferManager();\n" +
                "        Download download = tm.download(\"bucket\", \"key\", new File(\"path/to/file.txt\"));\n" +
                "        PersistableDownload persistableDownload = download.pause();\n" +
                "        MultipleFileDownload multipleFileDownload = tm.downloadDirectory(\"bucket\", \"prefix\", new File(\"path/to/dir\"));\n" +
                "        MultipleFileUpload multipleFileUpload = tm.uploadDirectory(\"bucket\", \"prefix\", new File(\"path/to/dir\"), true);\n" +
                "    }\n" +
                "}\n"
            )
        );
    }
}
