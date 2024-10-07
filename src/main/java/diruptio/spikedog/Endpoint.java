package diruptio.spikedog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jetbrains.annotations.NotNull;

/** An annotation to mark a method as an endpoint. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Endpoint {
    /**
     * The path of the endpoint.
     *
     * @return The path
     */
    @NotNull
    String path();

    /**
     * The methods that the endpoint supports.
     *
     * @return The methods
     */
    @NotNull
    String[] methods() default {};
}
