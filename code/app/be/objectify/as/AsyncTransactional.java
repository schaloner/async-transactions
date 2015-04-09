package be.objectify.as;

import play.mvc.With;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This has the same configuration options as {@link play.db.jpa.Transactional} and only exists so it can hook into
 * {@link be.objectify.as.AsyncTransactionalAction} conveniently.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
@With(AsyncTransactionalAction.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
public @interface AsyncTransactional
{
    String value() default "default";

    boolean readOnly() default false;
}
