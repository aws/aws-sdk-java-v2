/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static software.amazon.awssdk.core.internal.util.UriResourcePathUtils.updateUriHost;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;

public class UriResourcePathUtilsTest {

    @Test
    public void testUpdateUriHost() throws URISyntaxException {
        Assert.assertThat(updateUriHost(new URI("https://s3.amazonaws.com/index.html"), "foobar-"),
                          equalTo(new URI("https://foobar-s3.amazonaws.com/index.html")));

        Assert.assertThat(updateUriHost(new URI("http://user:pass@oldhostname/index.html"), "foobar."),
                          equalTo(new URI("http://user:pass@foobar.oldhostname/index.html")));
    }
}
