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

package software.amazon.awssdk.codegen.emitters;

import java.util.regex.Pattern;

/**
 * Removes HTML "anchor" tags from a string. This is used to compare files during clobbering while ignoring their documentation
 * links, which will pretty much always differ.
 */
public class LinkRemover implements CodeTransformer {
    private static final Pattern LINK_PATTERN = Pattern.compile("(<a[ \n]*/>|<a[> \n].*?</a>)", Pattern.DOTALL);

    @Override
    public String apply(String input) {
        return LINK_PATTERN.matcher(input).replaceAll("");
    }
}
