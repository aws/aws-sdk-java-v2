/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public class UriEncodeFn extends SingleArgFn {
    public static final String ID = "uriEncode";
    private static final String[] ENCODED_CHARACTERS = new String[] { "+", "*", "%7E" };
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = new String[] { "%20", "%2A", "~" };

    public UriEncodeFn(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    protected Value evalArg(Value arg) {
        String url = arg.expectString();
        try {
            String encoded = URLEncoder.encode(url, "UTF-8");
            for (int i = 0; i < ENCODED_CHARACTERS.length; i++) {
                encoded = encoded.replace(ENCODED_CHARACTERS[i], ENCODED_CHARACTERS_REPLACEMENTS[i]);
            }
            return Value.fromStr(encoded);
        } catch (UnsupportedEncodingException e) {
            throw SdkClientException.create("Unable to URI encode value: " + url, e);
        }
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitUriEncode(this);
    }
}
