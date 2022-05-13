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

package software.amazon.awssdk.buildtools.findbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.bcel.Const;

/**
 * Blocks usage of disallowed methods in the SDK.
 */
public class DisallowMethodCall extends OpcodeStackDetector {
    private static final Set<Entry<String, String>> PROHIBITED_METHODS = new HashSet<>();
    private final BugReporter bugReporter;

    static {
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpHeaders", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpResponse", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpRequest", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullRequest", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullResponse", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullRequest$Builder", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullResponse$Builder", "headers"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpRequest", "rawQueryParameters"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullRequest", "rawQueryParameters"));
        PROHIBITED_METHODS.add(new SimpleEntry<>("software/amazon/awssdk/http/SdkHttpFullRequest$Builder", "rawQueryParameters"));
    }

    public DisallowMethodCall(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int code) {
        switch (code) {
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
            case Const.INVOKEINTERFACE:
                handleMethodCall(code);
                return;
            default:
                // Ignore - not a method call.
        }
    }

    private void handleMethodCall(int code) {
        MethodDescriptor method = getMethodDescriptorOperand();
        SignatureParser signature = new SignatureParser(method.getSignature());
        Entry<String, String> calledMethod = new SimpleEntry<>(method.getSlashedClassName(), method.getName());
        if (PROHIBITED_METHODS.contains(calledMethod) && signature.getNumParameters() == 0) {
            bugReporter.reportBug(new BugInstance(this, "SDK_BAD_METHOD_CALL", NORMAL_PRIORITY)
                                      .addClassAndMethod(this)
                                      .addSourceLine(this, getPC()));
        }
    }
}
