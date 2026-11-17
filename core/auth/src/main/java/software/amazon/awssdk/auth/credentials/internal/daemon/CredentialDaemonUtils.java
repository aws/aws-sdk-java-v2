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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;

/**
 * Static helpers for the credential daemon provider.
 */
@SdkInternalApi
public final class CredentialDaemonUtils {
    /**
     * Where the documentation of the credential daemon will be. Kept as a placeholder until the documentation is published.
     */
    static final String CREDENTIAL_DAEMON_DOC = "<CREDENTIAL_DAEMON_DOC>";

    static final String START_HINT = "Start the credential daemon and sign in; see " + CREDENTIAL_DAEMON_DOC + ".";

    private static final Pattern CREDENTIAL_NAME = Pattern.compile("^[A-Za-z0-9_-][A-Za-z0-9._-]{0,63}$");

    private static final Pattern RFC_3339_TIMESTAMP =
        Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,9}))?Z$");

    private CredentialDaemonUtils() {
    }

    /**
     * Builds a {@link CredentialDaemonException} with the given error code, message and cause. The cause may be null.
     */
    public static CredentialDaemonException error(String errorCode, String message, Throwable cause) {
        return CredentialDaemonException.builder().errorCode(errorCode).message(message).cause(cause).build();
    }

    /**
     * Builds a {@link CredentialDaemonException#PROTOCOL_ERROR} with the given cause, which may be null. The cause must not
     * carry any of the response's content.
     */
    static CredentialDaemonException protocolError(String problem, Throwable cause) {
        return error(CredentialDaemonException.PROTOCOL_ERROR,
                     "The credential daemon returned a response that does not follow the protocol: " + problem + ".",
                     cause);
    }

    /**
     * Removes every control character (C0, DEL and C1) from text that came from the daemon.
     */
    public static String strip(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().filter(c -> !Character.isISOControl(c)).forEach(result::appendCodePoint);
        return result.toString();
    }

    /**
     * Returns whether the value is a valid credential name: 1 to 64 characters from {@code A-Z a-z 0-9 . _ -}, not starting
     * with {@code .}.
     */
    public static boolean isValidCredentialName(String name) {
        return name != null && CREDENTIAL_NAME.matcher(name).matches();
    }

    /**
     * Returns whether the profile properties configure SSO, with {@code sso_session} or {@code sso_start_url}.
     */
    public static boolean isSsoProfile(Map<String, String> properties) {
        return properties.containsKey(ProfileSection.SSO_SESSION.getPropertyKeyName())
               || properties.containsKey(ProfileProperty.SSO_START_URL);
    }

    /**
     * Returns whether the SSO session of the profile is held in the credential daemon, so no bearer token can be loaded for
     * it from files.
     */
    public static boolean daemonHoldsSsoSession(Profile profile) {
        if (profile == null) {
            return false;
        }
        Map<String, String> properties = profile.properties();
        return properties.containsKey(ProfileProperty.DAEMON_CREDENTIAL)
               && properties.containsKey(ProfileProperty.SSO_ACCOUNT_ID)
               && properties.containsKey(ProfileProperty.SSO_ROLE_NAME);
    }

    /**
     * Returns the message of the error raised when bearer tokens are requested for a profile whose SSO session is held in
     * the credential daemon.
     */
    public static String bearerTokenUnavailableMessage(String profileName) {
        return "Profile " + profileName + " does not have a usable sso_session: its SSO session is held in the credential "
               + "daemon (daemon_credential). Services that use bearer tokens need an SSO session stored in files; configure a "
               + "profile with sso_session and sign in with a file-based SSO login.";
    }

    /**
     * Parses an RFC 3339 timestamp in UTC with a {@code Z} suffix and 0 to 9 fractional digits. It does not use
     * {@code DateTimeFormatter.ISO_INSTANT}, whose offset handling differs between Java 8 and later releases.
     *
     * @throws IllegalArgumentException if the value is not an RFC 3339 UTC timestamp with a {@code Z} suffix.
     */
    public static Instant parseRfc3339(String value) {
        Matcher matcher = RFC_3339_TIMESTAMP.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not an RFC 3339 UTC timestamp.");
        }
        String fraction = matcher.group(7);
        int nanos = 0;
        if (fraction != null) {
            StringBuilder padded = new StringBuilder(fraction);
            while (padded.length() < 9) {
                padded.append('0');
            }
            nanos = Integer.parseInt(padded.toString());
        }
        try {
            return LocalDateTime.of(Integer.parseInt(matcher.group(1)),
                                    Integer.parseInt(matcher.group(2)),
                                    Integer.parseInt(matcher.group(3)),
                                    Integer.parseInt(matcher.group(4)),
                                    Integer.parseInt(matcher.group(5)),
                                    Integer.parseInt(matcher.group(6)),
                                    nanos)
                                .toInstant(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Not a valid date and time.", e);
        }
    }
}
