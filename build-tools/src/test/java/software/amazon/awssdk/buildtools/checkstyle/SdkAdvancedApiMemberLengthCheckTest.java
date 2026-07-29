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

package software.amazon.awssdk.buildtools.checkstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SdkAdvancedApiMemberLengthCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void guidanceWithinLimit_passes() throws Exception {
        String source = classWithMembers(repeat("a", 50), repeat("b", 50));
        assertEquals(Collections.emptyList(), runCheck(source, 1000));
    }

    @Test
    void guidanceOverLimit_fails() throws Exception {
        String source = classWithMembers(repeat("a", 1001), repeat("b", 50));
        List<String> violations = runCheck(source, 1000);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("guidance"), violations.get(0));
        assertTrue(violations.get(0).contains("1001"), violations.get(0));
    }

    @Test
    void saferAlternativeOverLimit_fails() throws Exception {
        String source = classWithMembers(repeat("a", 50), repeat("b", 1001));
        List<String> violations = runCheck(source, 1000);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("saferAlternative"), violations.get(0));
    }

    @Test
    void multiLineConcatenation_sumsAllFragments() throws Exception {
        String guidance = "\"" + repeat("a", 400) + "\"\n"
                          + "        + \"" + repeat("b", 400) + "\"\n"
                          + "        + \"" + repeat("c", 300) + "\"";
        String source = classWithRawGuidance(guidance);
        List<String> violations = runCheck(source, 1000);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("1100"), violations.get(0));
    }

    @Test
    void valueAtExactLimit_passes() throws Exception {
        String source = classWithMembers(repeat("a", 1000), repeat("b", 1000));
        assertEquals(Collections.emptyList(), runCheck(source, 1000));
    }

    private static String repeat(String s, int n) {
        return IntStream.range(0, n).mapToObj(i -> s).collect(Collectors.joining());
    }

    private static String classWithMembers(String guidance, String saferAlternative) {
        return classWithRawGuidanceAndSafer("\"" + guidance + "\"", "\"" + saferAlternative + "\"");
    }

    private static String classWithRawGuidance(String rawGuidanceExpr) {
        return classWithRawGuidanceAndSafer(rawGuidanceExpr, "\"safe\"");
    }

    private static String classWithRawGuidanceAndSafer(String rawGuidanceExpr, String rawSaferExpr) {
        return "package p;\n"
               + "@SdkAdvancedApi(\n"
               + "    caution = Caution.WHEN_IMPLEMENTED,\n"
               + "    guidance = " + rawGuidanceExpr + ",\n"
               + "    saferAlternative = " + rawSaferExpr + ")\n"
               + "public interface Foo {\n"
               + "}\n";
    }

    private List<String> runCheck(String source, int max) throws IOException, CheckstyleException {
        File file = tempDir.resolve("Foo.java").toFile();
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));

        DefaultConfiguration checkConfig = new DefaultConfiguration(SdkAdvancedApiMemberLengthCheck.class.getName());
        checkConfig.addAttribute("max", Integer.toString(max));

        DefaultConfiguration treeWalker = new DefaultConfiguration("TreeWalker");
        treeWalker.addChild(checkConfig);

        DefaultConfiguration checker = new DefaultConfiguration("Checker");
        checker.addAttribute("charset", "UTF-8");
        checker.addChild(treeWalker);

        Checker c = new Checker();
        c.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        c.configure(checker);

        List<String> messages = new ArrayList<>();
        c.addListener(new CollectingListener(messages));

        c.process(Collections.singletonList(file));
        c.destroy();
        return messages;
    }

    private static final class CollectingListener implements AuditListener {
        private final List<String> messages;

        private CollectingListener(List<String> messages) {
            this.messages = messages;
        }

        @Override
        public void addError(AuditEvent event) {
            messages.add(event.getMessage());
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            messages.add("EXCEPTION: " + throwable.getMessage());
        }

        @Override
        public void auditStarted(AuditEvent event) {
        }

        @Override
        public void auditFinished(AuditEvent event) {
        }

        @Override
        public void fileStarted(AuditEvent event) {
        }

        @Override
        public void fileFinished(AuditEvent event) {
        }
    }
}
