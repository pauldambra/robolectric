package org.robolectric.shadows;

import android.os.Looper;
import org.robolectric.Robolectric;
import org.robolectric.internal.Implementation;
import org.robolectric.internal.Implements;
import org.robolectric.internal.RealObject;
import org.robolectric.util.Scheduler;

import java.lang.ref.SoftReference;

import static org.robolectric.Robolectric.shadowOf;

/**
 * Shadow for {@code Looper} that enqueues posted {@link Runnable}s to be run (on this thread) later. {@code Runnable}s
 * that are scheduled to run immediately can be triggered by calling {@link #idle()}
 * todo: provide better support for advancing the clock and running queued tasks
 */

@SuppressWarnings({"UnusedDeclaration"})
@Implements(Looper.class)
public class ShadowLooper {
    private static final Thread MAIN_THREAD = Thread.currentThread();
    private static ThreadLocal<SoftReference<Looper>> looperForThread = makeThreadLocalLoopers();
    private Scheduler scheduler = new Scheduler();
    private Thread myThread = Thread.currentThread();
    private @RealObject Looper realObject;

    boolean quit;

    private static ThreadLocal<SoftReference<Looper>> makeThreadLocalLoopers() {
        return new ThreadLocal<SoftReference<Looper>>() {
            @Override
            protected SoftReference<Looper> initialValue() {
                return new SoftReference<Looper>(createLooper());
            }
        };
    }

    private static Looper createLooper() {
        return Robolectric.Reflection.newInstanceOf(Looper.class);
    }

    public static synchronized void resetThreadLoopers() {
        if (Thread.currentThread() != MAIN_THREAD) {
            throw new RuntimeException("you should only be calling this from the main thread!");
        }

        Looper mainLooper = myLooper();
        looperForThread = makeThreadLocalLoopers();
        looperForThread.set(new SoftReference<Looper>(mainLooper));
        shadowOf(mainLooper).reset();
    }

    @Implementation
    public static Looper getMainLooper() {
        return Robolectric.getShadowApplication().getMainLooper();
    }

    @Implementation
    public static void loop() {
        shadowOf(myLooper()).doLoop();
    }

    @Implementation
    public static synchronized Looper myLooper() {
        // Blech. We need to share the main looper because somebody might refer to it in a static
        // field. We also need to keep it in a soft reference so we don't max out permgen.
        SoftReference<Looper> looperSoftReference = looperForThread.get();
        Looper looper = looperSoftReference.get();
        if (looper == null) {
            looper = createLooper();
            looperForThread.set(new SoftReference<Looper>(looper));
        }
        return looper;
    }

    public void __constructor__() {
    }

    private void doLoop() {
        if (this != shadowOf(getMainLooper())) {
            synchronized (realObject) {
                while (!quit) {
                    try {
                        realObject.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    @Implementation
    public void quit() {
        if (this == shadowOf(getMainLooper())) throw new RuntimeException("Main thread not allowed to quit");
        synchronized (realObject) {
            quit = true;
            scheduler.reset();
            realObject.notifyAll();
        }
    }

    @Implementation
    public Thread getThread() {
    	return myThread;
    }
    
    public boolean hasQuit() {
        synchronized (realObject) {
            return quit;
        }
    }

    public static void pauseLooper(Looper looper) {
        shadowOf(looper).pause();
    }

    public static void unPauseLooper(Looper looper) {
        shadowOf(looper).unPause();
    }

    public static void pauseMainLooper() {
        pauseLooper(Looper.getMainLooper());
    }

    public static void unPauseMainLooper() {
        unPauseLooper(Looper.getMainLooper());
    }

    public static void idleMainLooper(long interval) {
        shadowOf(Looper.getMainLooper()).idle(interval);
    }


    public static void idleMainLooperConstantly(boolean shouldIdleConstantly) {
        shadowOf(Looper.getMainLooper()).idleConstantly(shouldIdleConstantly);
    }

    /**
     * Causes {@link Runnable}s that have been scheduled to run immediately to actually run. Does not advance the
     * scheduler's clock;
     */
    public void idle() {
        scheduler.advanceBy(0);
    }

    /**
     * Causes {@link Runnable}s that have been scheduled to run within the next {@code intervalMillis} milliseconds to
     * run while advancing the scheduler's clock.
     *
     * @param intervalMillis milliseconds to advance
     */
    public void idle(long intervalMillis) {
        scheduler.advanceBy(intervalMillis);
    }

    public void idleConstantly(boolean shouldIdleConstantly) {
        scheduler.idleConstantly(shouldIdleConstantly);
    }

    /**
     * Causes all of the {@link Runnable}s that have been scheduled to run while advancing the scheduler's clock to the
     * start time of the last scheduled {@link Runnable}.
     */
    public void runToEndOfTasks() {
        scheduler.advanceToLastPostedRunnable();
    }

    /**
     * Causes the next {@link Runnable}(s) that have been scheduled to run while advancing the scheduler's clock to its
     * start time. If more than one {@link Runnable} is scheduled to run at this time then they will all be run.
     */
    public void runToNextTask() {
        scheduler.advanceToNextPostedRunnable();
    }

    /**
     * Causes only one of the next {@link Runnable}s that have been scheduled to run while advancing the scheduler's
     * clock to its start time. Only one {@link Runnable} will run even if more than one has ben scheduled to run at the
     * same time.
     */
    public void runOneTask() {
        scheduler.runOneTask();
    }

    /**
     * Enqueue a task to be run later.
     *
     * @param runnable    the task to be run
     * @param delayMillis how many milliseconds into the (virtual) future to run it
     */
    public boolean post(Runnable runnable, long delayMillis) {
        if (!quit) {
            scheduler.postDelayed(runnable, delayMillis);
            return true;
        } else {
            return false;
        }
    }

    public boolean postAtFrontOfQueue(Runnable runnable) {
        if (!quit) {
            scheduler.postAtFrontOfQueue(runnable);
            return true;
        } else {
            return false;
        }
    }

    public void pause() {
        scheduler.pause();
    }

    public void unPause() {
        scheduler.unPause();
    }

    /**
     * Causes all enqueued tasks to be discarded, and pause state to be reset
     */
    public void reset() {
        scheduler = new Scheduler();
        quit = false;
    }

    /**
     * Returns the {@link org.robolectric.util.Scheduler} that is being used to manage the enqueued tasks.
     *
     * @return the {@link org.robolectric.util.Scheduler} that is being used to manage the enqueued tasks.
     */
    public Scheduler getScheduler() {
        return scheduler;
    }
}
