package org.yamcs.http.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.yamcs.http.api.archive.ArchiveTableRestHandler;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Routes.class)
public @interface Route {

    /**
     * Currently must be an absolute path. Specify route params by preceding them with a colon, followed by their
     * identifying name.
     */
    String path() default "";

    /**
     * Reference to a Protobuf rpc service method in the format SERVICE.METHOD.
     */
    String rpc() default "";

    /**
     * HTTP method or methods by which this rule is available. By default set to "GET".
     * <p>
     * Implementation note: can't use netty's HttpMethod because it's not an enum
     */
    String[] method() default { "GET" };

    /**
     * Whether this route must be checked before any other route. Used to differentiate a route when it's identifying
     * part overlaps with the route params of another route.
     * <p>
     * Use this as a last resort only. It's generally better to come up with a route scheme where you don't need this
     * flag. We have it here mostly for legacy purposes that need rework.
     */
    boolean priority() default false;

    /**
     * Data load routes expect to receive a large body and they receive it piece by piece in HttpContent objects.
     * 
     * See {@link ArchiveTableRestHandler } for an example on how to implement this.
     * 
     * For the normal routes (where dataLoad=false) the body is limited to {@link #maxBodySize()} bytes provided to the
     * HttpObjectAgregator.
     */
    boolean dataLoad() default false;

    /**
     * Set true if the execution of the route is expected to take a long time (more than 0.5 seconds). It will be
     * executed on another thread.
     * 
     * Leave false if the execution uses its own off thread mechanism (most of the routes should do that).
     * 
     * @return
     */
    boolean offThread() default false;

    int maxBodySize() default Router.MAX_BODY_SIZE;
}
