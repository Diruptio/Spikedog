package diruptio.spikedog;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A HTTP response. */
public class HttpResponse {
    private final String version;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private Map<String, String> headers = new LinkedHashMap<>();
    private ByteBuf content = Unpooled.buffer();
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Creates a new HTTP response.
     *
     * @param version The HTTP version
     */
    public HttpResponse(@NotNull String version) {
        this.version = version;
        headers.put("Content-Type", "text/plain");
    }

    /**
     * Gets the version of the response.
     *
     * @return The HTTP version
     */
    public @NotNull String version() {
        return version;
    }

    /**
     * Gets the status code of the response.
     *
     * @return The status code
     */
    public @NotNull HttpResponseStatus status() {
        return status;
    }

    /**
     * Sets the status code of the response.
     *
     * @param status The status code
     */
    public void status(@NotNull HttpResponseStatus status) {
        this.status = status;
    }

    /**
     * Gets the headers of the response.
     *
     * @return The headers
     */
    public @NotNull Map<String, String> headers() {
        return headers;
    }

    /**
     * Sets the headers of the response.
     *
     * @param headers The headers
     */
    public void headers(@NotNull Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Gets a header value by its name.
     *
     * @param name The header name
     * @return The header value, or {@code null} if not found
     */
    public @Nullable String header(@NotNull String name) {
        return headers.get(name);
    }

    /**
     * Sets a header.
     *
     * @param name The header name
     * @param value The header value
     */
    public void header(@NotNull String name, @NotNull String value) {
        headers.put(name, value);
    }

    /**
     * Gets the content of the response.
     *
     * @return The content
     */
    public @NotNull ByteBuf content() {
        return content;
    }

    /**
     * Sets the content of the response.
     *
     * @param content The content
     */
    public void content(@NotNull ByteBuf content) {
        this.content = content;
    }

    /**
     * Sets the content of the response.
     *
     * @param content The content
     */
    public void content(@NotNull CharSequence content) {
        this.content = Unpooled.copiedBuffer(content, charset);
    }

    /**
     * Gets the charset of the response.
     *
     * @return The charset
     */
    public @Nullable Charset charset() {
        return charset;
    }

    /**
     * Sets the charset of the response.
     *
     * @param charset The charset
     */
    public void charset(@Nullable Charset charset) {
        this.charset = charset;
    }

    /**
     * Gets the length of the content.
     *
     * @return The content length
     */
    public int getContentLength() {
        return content.readableBytes();
    }
}
