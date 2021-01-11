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

package software.amazon.awssdk.codegen.model.intermediate;


import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.TestStringUtils.toPlatformLfs;

import org.junit.Test;
import software.amazon.awssdk.codegen.docs.DocumentationBuilder;

public class DocumentationBuilderTest {

    @Test
    public void javadocFormattedCorrectly() {
        String docs = new DocumentationBuilder()
            .description("Some service docs")
            .param("paramOne", "param one docs")
            .param("paramTwo", "param two docs")
            .returns("This returns something")
            .syncThrows("FooException", "Thrown when foo happens")
            .syncThrows("BarException", "Thrown when bar happens")
            .tag("sample", "FooService.FooOperation")
            .see("this thing")
            .see("this other thing")
            .build();
        assertThat(docs).isEqualTo(toPlatformLfs("Some service docs\n" +
                                                 "\n" +
                                                 "@param paramOne param one docs\n" +
                                                 "@param paramTwo param two docs\n" +
                                                 "@return This returns something\n" +
                                                 "@throws FooException Thrown when foo happens\n" +
                                                 "@throws BarException Thrown when bar happens\n" +
                                                 "@sample FooService.FooOperation\n" +
                                                 "@see this thing\n" +
                                                 "@see this other thing\n"));
    }

    /**
     * For async methods that return a {@link java.util.concurrent.CompletableFuture} no exception is thrown from the method,
     * instead the future is completed exceptionally with the exception. As such we document this in the @returns section
     * of the Javadocs rather than having @throws for each exception.
     */
    @Test
    public void asyncReturns_FormatsExceptionsInUnorderedList() {
        String docs = new DocumentationBuilder()
            .description("Some service docs")
            .param("paramOne", "param one docs")
            .returns("CompletableFuture of success")
            .asyncThrows("FooException", "Foo docs")
            .asyncThrows("BarException", "Bar docs")
            .build();
        assertThat(docs).isEqualTo(toPlatformLfs("Some service docs\n" +
                                                 "\n" +
                                                 "@param paramOne param one docs\n" +
                                                 "@return CompletableFuture of success<br/>\n" +
                                                 "The CompletableFuture returned by this method can be completed exceptionally with the following exceptions.\n" +
                                                 "<ul>\n" +
                                                 "<li>FooException Foo docs</li>\n" +
                                                 "<li>BarException Bar docs</li>\n" +
                                                 "</ul>\n"));

    }

    @Test
    public void asyncReturnsWithoutDocsForSuccessReturn_FormatsExceptionsInUnorderedList() {
        String docs = new DocumentationBuilder()
            .description("Some service docs")
            .param("paramOne", "param one docs")
            .asyncThrows("FooException", "Foo docs")
            .asyncThrows("BarException", "Bar docs")
            .build();
        assertThat(docs).isEqualTo(toPlatformLfs("Some service docs\n" +
                                                 "\n" +
                                                 "@param paramOne param one docs\n" +
                                                 "@return A CompletableFuture indicating when result will be completed.<br/>\n" +
                                                 "The CompletableFuture returned by this method can be completed exceptionally with the following exceptions.\n" +
                                                 "<ul>\n" +
                                                 "<li>FooException Foo docs</li>\n" +
                                                 "<li>BarException Bar docs</li>\n" +
                                                 "</ul>\n"));
    }

    @Test
    public void missingValuesAreNotPresent() {
        String docs = new DocumentationBuilder()
            .description("Some service docs")
            .build();
        assertThat(docs).isEqualTo(toPlatformLfs("Some service docs\n\n"));
    }

    @Test
    public void allValuesMissing_ProducesEmptyDocString() {
        String docs = new DocumentationBuilder()
            .build();
        assertThat(docs).isEqualTo("");
    }

}
