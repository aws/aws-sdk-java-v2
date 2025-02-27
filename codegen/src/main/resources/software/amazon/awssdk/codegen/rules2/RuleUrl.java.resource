import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Represents a URL to be used internally for endpoint resolution.
 */
@SdkInternalApi
public final class RuleUrl {
    private final String scheme;
    private final String authority;
    private final String path;
    private final String normalizedPath;
    private final boolean isIp;

    RuleUrl(String scheme, String authority, String path, String normalizedPath, boolean isIp) {
        this.scheme = scheme;
        this.authority = authority;
        this.path = path;
        this.normalizedPath = normalizedPath;
        this.isIp = isIp;
    }

    public String scheme() {
        return scheme;
    }

    public String authority() {
        return authority;
    }

    public String path() {
        return path;
    }

    public String normalizedPath() {
        return normalizedPath;
    }

    public boolean isIp() {
        return isIp;
    }

    public static RuleUrl parse(String url) throws MalformedURLException {
        URL parsed = new URL(url);
        String path = parsed.getPath();
        if (parsed.getQuery() != null) {
            return null;
        }
        String host = parsed.getHost();
        boolean isIpAddr = (host.startsWith("[") && host.endsWith("]")) || isIpAddr(host);
        String normalizedPath;
        if (StringUtils.isBlank(path)) {
            normalizedPath = "/";
        } else if (path.startsWith("/") && path.endsWith("/")) {
            normalizedPath = path;
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

        return new RuleUrl(parsed.getProtocol(), parsed.getAuthority(), path, normalizedPath, isIpAddr);
    }

    private static boolean isIpAddr(String host) {
        int from = 0;
        int segments = 0;
        boolean done = false;
        while (!done) {
            int index = host.indexOf('.', from);
            if (index == -1) {
                if (segments != 3) {
                    // E.g., 123.com
                    return false;
                }
                index = host.length();
                done = true;
            } else if (segments == 3) {
                // E.g., 1.2.3.4.5
                return false;
            }
            int length = index - from;
            if (length == 1) {
                char ch0 = host.charAt(from);
                if (ch0 < '0' || ch0 > '9') {
                    return false;
                }
            } else if (length == 2) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                if ((ch0 < '0' || ch0 > '9') || (ch1 < '0' || ch1 > '9')) {
                    return false;
                }
            } else if (length == 3) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                char ch2 = host.charAt(from + 2);
                if ((ch0 < '0' || ch0 > '9')
                        || (ch1 < '0' || ch1 > '9')
                        || (ch2 < '0' || ch2 > '9')
                ) {
                    return false;
                }
                int value = (host.charAt(from) - '0') * 100
                        + (host.charAt(from + 1) - '0') * 10
                        + (host.charAt(from + 2) - '0');
                if (value > 255) {
                    return false;
                }
            } else {
                return false;
            }
            from = index + 1;
            segments += 1;
        }
        return true;
    }
}
