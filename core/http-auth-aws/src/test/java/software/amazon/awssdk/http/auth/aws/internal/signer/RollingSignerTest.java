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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class RollingSignerTest {

    @Test
    public void sign_shouldRollSignature() {
        RollingSigner signer = new RollingSigner("key".getBytes(StandardCharsets.UTF_8), "seed");

        String first = signer.sign(signature -> signature + "a");
        String second = signer.sign(signature -> signature + "b");
        String third = signer.sign(signature -> signature + "c");

        assertEquals("878b44846ec83a43c3932578c5311fa8b289ae194ccc0c06244c366f3b949012", first);
        assertEquals("08f47cc56d186cee34fa7662128d5c9187eb9483938df45282e922d902e85a27", second);
        assertEquals("10da3ca4a393be5f8368823cb73a777426db24428bd8374fbf54a64a808ff00a", third);
    }

    @Test
    public void reset_shouldResetSeed() {
        RollingSigner signer = new RollingSigner("key".getBytes(StandardCharsets.UTF_8), "seed");

        String first = signer.sign(signature -> signature + "a");
        String second = signer.sign(signature -> signature + "b");

        signer.reset();

        String firstAgain = signer.sign(signature -> signature + "a");

        assertEquals("878b44846ec83a43c3932578c5311fa8b289ae194ccc0c06244c366f3b949012", first);
        assertEquals("08f47cc56d186cee34fa7662128d5c9187eb9483938df45282e922d902e85a27", second);
        assertEquals("878b44846ec83a43c3932578c5311fa8b289ae194ccc0c06244c366f3b949012", firstAgain);
    }
}
