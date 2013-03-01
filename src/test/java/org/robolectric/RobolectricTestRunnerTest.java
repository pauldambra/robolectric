package org.robolectric;

import android.app.Application;
import android.content.res.Resources;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.DisableStrictI18n;
import org.robolectric.annotation.EnableStrictI18n;
import org.robolectric.annotation.Values;
import org.robolectric.util.Transcript;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.robolectric.Robolectric.shadowOf;

public class RobolectricTestRunnerTest {
    @Test
    public void shouldInitializeAndBindApplicationButNotCallOnCreate() throws Exception {
        runAndAssertNoFailures(SomeAppTest.class);
    }

    @Config(manifest = "TestAndroidManifest.xml")
    public static class SomeAppTest {
        @Test
        public void shouldInitializeAndBindApplicationButNotCallOnCreate() throws Exception {
            assertNotNull(Robolectric.application);
            assertEquals(MyTestApplication.class, Robolectric.application.getClass());
            assertFalse(((MyTestApplication) Robolectric.application).onCreateWasCalled);
            assertNotNull(shadowOf(Robolectric.application).getResourceLoader());
            assertThat(1).isEqualTo(2);
        }
    }

    @Config(type = Config.Type.LIBRARY)
    public static class SomeLibraryTest {
        @Test
        public void firstTest() throws Exception {
        }

        @Test
        public void secondTest() throws Exception {
        }
    }

    @Test public void shouldSetUpSystemResources() throws Exception {
        assertNotNull(Resources.getSystem());
        assertEquals(Robolectric.application.getResources().getString(android.R.string.copy),
                Resources.getSystem().getString(android.R.string.copy));

        assertNotNull(Robolectric.application.getResources().getString(R.string.howdy));
        try {
            Resources.getSystem().getString(R.string.howdy);
            fail("should have thrown");
        } catch (Resources.NotFoundException e) {
        }
    }

    @Test
    public void setStaticValue_shouldIgnoreFinalModifier() {
        RobolectricContext.setStaticValue(android.os.Build.class, "MODEL", "expected value");

        assertEquals("expected value", android.os.Build.MODEL);
    }

    @Test
    @EnableStrictI18n
    public void internalBeforeTest_setsShadowApplicationStrictI18n() {
        assertTrue(Robolectric.getShadowApplication().isStrictI18n());
    }

    @Test
    @DisableStrictI18n
    public void internalBeforeTest_clearsShadowApplicationStrictI18n() {
        assertFalse(Robolectric.getShadowApplication().isStrictI18n());
    }

    @Test
    @Values(qualifiers = "fr")
    public void internalBeforeTest_testValuesResQualifiers() {
        assertEquals("fr", Robolectric.shadowOf(Robolectric.getShadowApplication().getResources().getConfiguration()).getQualifiers());
    }

    @Test
    public void internalBeforeTest_resetsValuesResQualifiers() {
        assertEquals("", Robolectric.shadowOf(Robolectric.getShadowApplication().getResources().getConfiguration()).getQualifiers());
    }

    @Test
    public void internalBeforeTest_doesNotSetI18nStrictModeFromSystemIfPropertyAbsent() {
        assertFalse(Robolectric.getShadowApplication().isStrictI18n());
    }

    @Test
    @EnableStrictI18n
    public void methodBlock_setsI18nStrictModeForClassHandler() {
        TextView tv = new TextView(Robolectric.application);
        try {
            tv.setText("Foo");
            fail("TextView#setText(String) should produce an i18nException");
        } catch (Exception e) {
            // Compare exception name because it was loaded in the instrumented classloader
            assertEquals("org.robolectric.util.I18nException", e.getClass().getName());
        }
    }


    private void runAndAssertNoFailures(Class<?> testClass) throws InitializationError {
        RobolectricTestRunner testRunner = new RobolectricTestRunner(testClass);
        MyRunListener listener = runTests(testRunner, new MyRunListener());
        assertNoFailures(listener);
        assertThat(listener.finished.getEvents().size()).isEqualTo(testRunner.testCount());
    }

    private void assertNoFailures(MyRunListener listener) {
        for (Failure failure : listener.failures) {
            System.err.println("Failure in " + failure.getDescription() + ":");
            failure.getException().printStackTrace();
            System.err.println("");
        }
        if (listener.failures.size() != 0) {
            fail("there were failures!");
        }
    }

    private <T extends RunListener> T runTests(RobolectricTestRunner testRunner, T listener) {
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(listener);
        testRunner.run(notifier);
        return listener;
    }

//    public static class RunnerForTesting extends TestRunners.WithDefaults {
//        public static RunnerForTesting instance;
//        private final AndroidManifest androidManifest;
//
//        public RunnerForTesting(Class<?> testClass) throws InitializationError {
//            super(testClass);
//            instance = this;
//            androidManifest = getRobolectricContext().getAppManifest();
//        }
//
//        @Override protected Application createApplication(AndroidManifest appManifest) {
//            return new MyTestApplication();
//        }
//    }

    public static class MyTestApplication extends Application {
        private boolean onCreateWasCalled;

        @Override public void onCreate() {
            this.onCreateWasCalled = true;
        }
    }

    private static class MyRunListener extends RunListener {
        private final Transcript finished = new Transcript();
        private final List<Failure> failures = new ArrayList<Failure>();

        @Override public void testFailure(Failure failure) throws Exception {
            failures.add(failure);
        }

        @Override public void testFinished(Description description) throws Exception {
            finished.add("testFinished " + description.getMethodName());
        }
    }
}
