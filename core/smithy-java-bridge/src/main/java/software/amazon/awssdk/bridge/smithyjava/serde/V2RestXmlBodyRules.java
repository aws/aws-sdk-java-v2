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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Applies v2's two rest-xml request-body rules that smithy-java's serializer does not follow.
 *
 * <p>Both come from {@code XmlProtocolMarshaller} in v2:
 *
 * <ol>
 *   <li><b>An empty payload is no payload.</b> Without an explicit {@code @httpPayload} member there is no
 *       wrapping root element, so when every body member is null v2 writes nothing — no body, no
 *       {@code Content-Type}. smithy-java writes the input shape's root element regardless, so a request
 *       whose body members are all null goes out as {@code <CopyObjectRequest></CopyObjectRequest>}. That is
 *       ledger 12.6: S3's {@code CopyObject}, whose only body members are customization-injected ones the
 *       request customizer has already consumed, sent an empty document where stock sends nothing.</li>
 *   <li><b>A modeled {@code Content-Type} wins.</b> v2 adds {@code application/xml} only if no
 *       {@code Content-Type} is already set. smithy-java appends its own, so an operation that models a
 *       {@code Content-Type} header member sends both (ledger 12.7).</li>
 * </ol>
 *
 * <p>Runs at {@code modifyBeforeRetryLoop}: after serialization, before signing, so the signature covers
 * the request that is actually sent. Only an in-memory body is inspected, and only a small one — the
 * serializer produces those, and a streamed payload is never a serialized document.
 */
@SdkProtectedApi
public final class V2RestXmlBodyRules implements ClientInterceptor {

    private static final V2RestXmlBodyRules INSTANCE = new V2RestXmlBodyRules();

    /** An XML document consisting of nothing but one empty element, optionally with a prolog. */
    private static final Pattern EMPTY_DOCUMENT = Pattern.compile(
        "\\s*(<\\?xml[^>]*\\?>)?\\s*<([A-Za-z_][\\w.-]*)(\\s[^>]*)?(/>|>\\s*</\\2>)\\s*");

    private static final String XML = "application/xml";
    private static final int LARGEST_EMPTY_DOCUMENT = 512;

    private V2RestXmlBodyRules() {
    }

    public static V2RestXmlBodyRules instance() {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <RequestT> RequestT modifyBeforeRetryLoop(RequestHook<?, ?, RequestT> hook) {
        if (!(hook.request() instanceof HttpRequest request)) {
            return hook.request();
        }
        List<String> contentTypes = request.headers().allValues("content-type");
        boolean emptyDocument = isEmptyDocument(request.body());
        boolean duplicateType = contentTypes.size() > 1 && contentTypes.contains(XML);
        if (!emptyDocument && !duplicateType) {
            return hook.request();
        }

        ModifiableHttpRequest modifiable = request.toModifiableCopy();
        List<String> kept = new ArrayList<>(contentTypes);
        kept.remove(XML);
        if (emptyDocument) {
            modifiable.setBody(DataStream.ofEmpty());
            modifiable.removeHeader("content-length");
        }
        if (kept.isEmpty()) {
            modifiable.removeHeader("content-type");
        } else {
            modifiable.setHeader("content-type", kept);
        }
        return (RequestT) modifiable;
    }

    private static boolean isEmptyDocument(DataStream body) {
        if (body == null || !body.isAvailable() || !body.hasKnownLength()
            || body.contentLength() == 0 || body.contentLength() > LARGEST_EMPTY_DOCUMENT) {
            return false;
        }
        ByteBuffer bytes = body.asByteBuffer().duplicate();
        String text = StandardCharsets.UTF_8.decode(bytes).toString();
        return EMPTY_DOCUMENT.matcher(text).matches();
    }
}
