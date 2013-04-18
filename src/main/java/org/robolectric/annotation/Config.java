package org.robolectric.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Indicate that robolectric should look for values that is specific by those qualifiers
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Config {

    /**
     * The Android SDK level to emulate. If not specified, Robolectric defaults to the targetSdkVersion in your app's manifest.
     */
    int emulateSdk() default -1;

    /**
     * Qualifiers for the resource resolution, such as "fr-normal-port-hdpi".
     */
    String qualifiers() default "";

    /**
     * The Android SDK level to report in Build.VERSION.SDK_INT.
     */
    int reportSdk() default -1;

    /**
     * A list of shadow classes to enable, in addition to those that are already present.
     */
    Class<?>[] shadows() default {};

    public class Implementation implements Config {
        private final int emulateSdk;
        private final String qualifiers;
        private final int reportSdk;
        private final Class<?>[] shadows;

        public Implementation(int emulateSdk, String qualifiers, int reportSdk, Class<?>[] shadows) {
            this.emulateSdk = emulateSdk;
            this.qualifiers = qualifiers;
            this.reportSdk = reportSdk;
            this.shadows = shadows;
        }

        public Implementation(Config baseConfig, Config overlayConfig) {
            this.emulateSdk = pick(baseConfig.emulateSdk(), overlayConfig.emulateSdk(), -1);
            this.qualifiers = pick(baseConfig.qualifiers(), overlayConfig.qualifiers(), "");
            this.reportSdk = pick(baseConfig.reportSdk(), overlayConfig.reportSdk(), -1);
            ArrayList<Class<?>> shadows = new ArrayList<Class<?>>();
            shadows.addAll(Arrays.asList(baseConfig.shadows()));
            shadows.addAll(Arrays.asList(overlayConfig.shadows()));
            this.shadows = shadows.toArray(new Class[shadows.size()]);
        }

        private <T> T pick(T baseValue, T overlayValue, T nullValue) {
            return overlayValue.equals(nullValue) ? baseValue : overlayValue;
        }

        @Override public int emulateSdk() {
            return emulateSdk;
        }

        @Override public String qualifiers() {
            return qualifiers;
        }

        @Override public int reportSdk() {
            return reportSdk;
        }

        @Override public Class<?>[] shadows() {
            return shadows;
        }

        @NotNull @Override public Class<? extends Annotation> annotationType() {
            return Config.class;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Implementation other = (Implementation) o;

            if (emulateSdk != other.emulateSdk) return false;
            if (reportSdk != other.reportSdk) return false;
            if (!qualifiers.equals(other.qualifiers)) return false;
            if (!Arrays.equals(shadows, other.shadows)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = emulateSdk;
            result = 31 * result + qualifiers.hashCode();
            result = 31 * result + reportSdk;
            result = 31 * result + Arrays.hashCode(shadows);
            return result;
        }

    }
}
