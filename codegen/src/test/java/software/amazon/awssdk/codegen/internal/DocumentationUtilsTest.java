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
package software.amazon.awssdk.codegen.internal;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;


public class DocumentationUtilsTest {

    @Test
    public void strip_html_tags_null_or_empty_input_returns_empty_string() {

        Assert.assertThat(DocumentationUtils.stripHtmlTags(null), Matchers
                .isEmptyString());
        Assert.assertThat(DocumentationUtils.stripHtmlTags(""), Matchers
                .isEmptyString());

    }

    @Test
    public void html_tags_at_start_of_string_are_removed() {
        Assert.assertEquals("foo", DocumentationUtils.stripHtmlTags
                ("<bar>foo</bar>"));
    }

    @Test
    public void empty_html_tags_at_start_are_removed() {
        Assert.assertThat(DocumentationUtils.stripHtmlTags("<p></p>"), Matchers
                .isEmptyString());
        Assert.assertThat(DocumentationUtils.stripHtmlTags("<p/>"), Matchers
                .isEmptyString());
    }
}
