import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public class UriEncodeFn extends SingleArgFn {
    public static final String ID = "uriEncode";
    private static final String[] ENCODED_CHARACTERS = new String[]{"+", "*", "%7E"};
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = new String[]{"%20", "%2A", "~"};

    public UriEncodeFn(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    protected Value evalArg(Value arg) {
        String url = arg.expectString();
        try {
            String encoded = URLEncoder.encode(url, "UTF-8");
            for (int i = 0; i < ENCODED_CHARACTERS.length; i++) {
                encoded = encoded.replace(ENCODED_CHARACTERS[i], ENCODED_CHARACTERS_REPLACEMENTS[i]);
            }
            return Value.fromStr(encoded);
        } catch (UnsupportedEncodingException e) {
            throw SdkClientException.create("Unable to URI encode value: " + url, e);
        }
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitUriEncode(this);
    }
}
