package software.amazon.awssdk.codegen;

import software.amazon.awssdk.codegen.internal.Constant;

/**
 * Various convenience methods for strings manipulation required in Code Generator tests.
 */
public final class TestStringUtils {

    /**
     * Not intended to be constructed directly.
     */
    private TestStringUtils() {
    }

    /**
     * Replaces all the newline (line feed/LF) characters ('&#x5c;u000A' or '\n') with the platform-dependent line separator char
     * (e.g. "&#x5c;u000D&#x5c;u000A" or "\r\n" on Windows systems).
     *
     * @param str string, containing '\n' chars to be replaced
     * @return original string with '\n' chars replaced with platform-dependent line separator chars
     * @see Constant#LF
     */
    public static String toPlatformLfs(String str) {
        return str.replaceAll("\n", Constant.LF);
    }

}
