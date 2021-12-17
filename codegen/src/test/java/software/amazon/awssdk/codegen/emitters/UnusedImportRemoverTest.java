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

package software.amazon.awssdk.codegen.emitters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class UnusedImportRemoverTest {

    private static final String FILE_WITH_UNUSED_IMPORTS_JAVA = "content-before-unused-import-removal.txt";
    private static final String FILE_WITHOUT_UNUSED_IMPORTS_JAVA = "content-after-unused-import-removal.txt";

    private final UnusedImportRemover sut = new UnusedImportRemover();

    @Test
    public void unusedImportsAreRemoved() throws Exception {
        String content = loadFileContents(FILE_WITH_UNUSED_IMPORTS_JAVA);
        String expected = loadFileContents(FILE_WITHOUT_UNUSED_IMPORTS_JAVA);
        String result = sut.apply(content);
        assertThat(result, equalToIgnoringWhiteSpace(expected));
    }

    @Test
    public void nonJavaContentIsIgnored() throws Exception {
        String jsonContent = "{ \"SomeKey\": 4, \"ADict\": { \"SubKey\": \"Subvalue\" } }";
        String result = sut.apply(jsonContent);
        assertThat(result, equalTo(jsonContent));
    }

    private static String loadFileContents(String filename) throws Exception {
        return new String(Files.readAllBytes(Paths.get(UnusedImportRemoverTest.class.getResource(filename).toURI())));
    }

}
