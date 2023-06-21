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

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.MapUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Function to parse a URI from a string.
 */
@SdkInternalApi
public class ParseUrl extends SingleArgFn {
    public static final String ID = "parseURL";

    public static final Identifier SCHEME = Identifier.of("scheme");
    public static final Identifier AUTHORITY = Identifier.of("authority");
    public static final Identifier PATH = Identifier.of("path");
    public static final Identifier NORMALIZED_PATH = Identifier.of("normalizedPath");
    public static final Identifier IS_IP = Identifier.of("isIp");

    public ParseUrl(FnNode fnNode) {
        super(fnNode);
    }

    public static ParseUrl ofExprs(Expr expr) {
        return new ParseUrl(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitParseUrl(this);
    }

    @Override
    protected Value evalArg(Value arg) {
        String url = arg.expectString();
        try {
            URL parsed = new URL(url);
            String path = parsed.getPath();
            if (parsed.getQuery() != null) {
                return Value.none();

            }
            boolean isIpAddr = false;
            String host = parsed.getHost();
            if (host.startsWith("[") && host.endsWith("]")) {
                isIpAddr = true;
            }
            String[] dottedParts = host.split("\\.");
            if (dottedParts.length == 4) {
                if (Arrays.stream(dottedParts).allMatch(part -> {
                    try {
                        int value = Integer.parseInt(part);
                        return value >= 0 && value <= 255;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })) {
                    isIpAddr = true;
                }
            }
            String normalizedPath;
            if (StringUtils.isBlank(path)) {
                normalizedPath = "/";
            } else {
                StringBuilder builder = new StringBuilder();
                if (!path.startsWith("/")) {
                    builder.append("/");
                }
                builder.append(path);
                if (!path.endsWith("/")) {
                    builder.append("/");
                }
                normalizedPath = builder.toString();
            }
            return Value.fromRecord(MapUtils.of(SCHEME, Value.fromStr(parsed.getProtocol()), AUTHORITY,
                    Value.fromStr(parsed.getAuthority()), PATH, Value.fromStr(path), NORMALIZED_PATH,
                    Value.fromStr(normalizedPath), IS_IP, Value.fromBool(isIpAddr)));
        } catch (MalformedURLException e) {
            return Value.none();
        }
    }
}
