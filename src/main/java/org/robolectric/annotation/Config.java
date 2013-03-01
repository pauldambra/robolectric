package org.robolectric.annotation;

import org.robolectric.AndroidManifest;
import org.robolectric.Configurer;
import org.robolectric.res.AndroidSdkFinder;
import org.robolectric.res.ResourcePath;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Config {
    Type type() default Type.APP;

    String manifest() default "AndroidManifest.xml";

    Class<? extends Configurer> configurer() default Configurer.class;

    enum Type {
        APP,
        LIBRARY
    }

}
