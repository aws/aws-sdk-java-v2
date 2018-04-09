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

package software.amazon.awssdk.auth.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Verify the functionality of {@link ProfileFileLocations}.
 */
public class ProfileFileLocationsTest {
    private final Map<String, String> savedEnvironmentVariableValues = new HashMap<>();

    private static final List<String> SAVED_ENVIRONMENT_VARIABLES = Arrays.asList("HOME",
                                                                                  "USERPROFILE",
                                                                                  "HOMEDRIVE",
                                                                                  "HOMEPATH");

    private EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    /**
     * Save the current state of the environment variables we're messing around with in these tests so that we can restore them
     * when we are done.
     */
    @Before
    public void saveEnvironment() throws Exception {
        // The tests in this file change the os.home for testing windows vs non-windows loading, and the static constructor for
        // ProfileFileLocations currently loads the file system separator based on the os.home. We need to call the static
        // constructor for ProfileFileLocations before changing the os.home so that it doesn't try to load the file system
        // separator during the test. If we don't, it'll complain that it doesn't recognize the file system.
        ProfileFileLocations.userHomeDirectory();

        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            savedEnvironmentVariableValues.put(variable, System.getenv(variable));
        }
    }

    /**
     * Reset the environment variables after each test.
     */
    @After
    public void restoreEnvironment() throws Exception {
        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            String savedValue = savedEnvironmentVariableValues.get(variable);

            if (savedValue == null) {
                ENVIRONMENT_VARIABLE_HELPER.remove(variable);
            } else {
                ENVIRONMENT_VARIABLE_HELPER.set(variable, savedValue);
            }
        }
    }

    @Test
    public void homeDirectoryResolutionPriorityIsCorrectOnWindows() throws Exception {
        String osName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 7");

            ENVIRONMENT_VARIABLE_HELPER.set("HOME", "home");
            ENVIRONMENT_VARIABLE_HELPER.set("USERPROFILE", "userprofile");
            ENVIRONMENT_VARIABLE_HELPER.set("HOMEDRIVE", "homedrive");
            ENVIRONMENT_VARIABLE_HELPER.set("HOMEPATH", "homepath");

            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo("home");

            ENVIRONMENT_VARIABLE_HELPER.remove("HOME");
            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo("userprofile");

            ENVIRONMENT_VARIABLE_HELPER.remove("USERPROFILE");
            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo("homedrivehomepath");

            ENVIRONMENT_VARIABLE_HELPER.remove("HOMEDRIVE");
            ENVIRONMENT_VARIABLE_HELPER.remove("HOMEPATH");

            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo(System.getProperty("user.home"));
        } finally {
            System.setProperty("os.name", osName);
        }
    }

    @Test
    public void homeDirectoryResolutionPriorityIsCorrectOnNonWindows() throws Exception {
        String osName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");

            ENVIRONMENT_VARIABLE_HELPER.set("HOME", "home");
            ENVIRONMENT_VARIABLE_HELPER.set("USERPROFILE", "userprofile");
            ENVIRONMENT_VARIABLE_HELPER.set("HOMEDRIVE", "homedrive");
            ENVIRONMENT_VARIABLE_HELPER.set("HOMEPATH", "homepath");

            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo("home");

            ENVIRONMENT_VARIABLE_HELPER.remove("HOME");
            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo(System.getProperty("user.home"));

            ENVIRONMENT_VARIABLE_HELPER.remove("USERPROFILE");
            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo(System.getProperty("user.home"));

            ENVIRONMENT_VARIABLE_HELPER.remove("HOMEDRIVE");
            ENVIRONMENT_VARIABLE_HELPER.remove("HOMEPATH");

            assertThat(ProfileFileLocations.userHomeDirectory()).isEqualTo(System.getProperty("user.home"));
        } finally {
            System.setProperty("os.name", osName);
        }
    }
}
