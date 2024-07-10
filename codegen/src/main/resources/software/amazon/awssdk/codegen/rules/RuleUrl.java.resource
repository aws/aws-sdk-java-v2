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

        return new RuleUrl(parsed.getProtocol(), parsed.getAuthority(), path, normalizedPath, isIpAddr);
    }
}
