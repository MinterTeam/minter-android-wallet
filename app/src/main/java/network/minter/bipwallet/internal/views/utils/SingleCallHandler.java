package network.minter.bipwallet.internal.views.utils;

import android.os.SystemClock;

import java.util.HashMap;
import java.util.Map;

public class SingleCallHandler {
    private final static Object sMapLock = new Object();
    /**
     * Milliseconds
     * Default: 600 of 1000 (0.6 seconds)
     */
    public static long THRESHOLD = 1000;
    private static Map<String, SingleCallHandler> sHandlers = new HashMap<>();
    private long mLastCall;

    public static void call(Object tag, long threshold, CallHandler handler) {
        call(String.format("%s:%d", tag.getClass().getName(), tag.hashCode()), threshold, handler);
    }

    public static void call(Object tag, CallHandler handler) {
        call(String.format("%s:%d", tag.getClass().getName(), tag.hashCode()), THRESHOLD, handler);
    }

    public static void call(String tag, CallHandler handler) {
        call(tag, THRESHOLD, handler);
    }

    public static void call(String tag, long threshold, CallHandler handler) {
        final long currentCallTime = SystemClock.uptimeMillis();

        boolean haveHandler;
        synchronized (sMapLock) {
            haveHandler = sHandlers.containsKey(tag);
        }

        // if we have handler, check for last time in it
        if (haveHandler) {
            SingleCallHandler h;
            // getting existing handler
            synchronized (sMapLock) {
                h = sHandlers.get(tag);
            }

            //is this possible?
            if (h == null) {
                sHandlers.remove(tag);
                call(tag, threshold, handler);
                return;
            }

            // calculating elapsed time to compare with THRESHOLD
            long elapsedTime = currentCallTime - h.mLastCall;
            // if time doesn't up, do nothing
            if (elapsedTime <= threshold) {
                return;
            }

            // otherwise settings new last click time
            h.mLastCall = currentCallTime;
            // call
            handler.onCall();
            return;
        }

        // Handler never called
        SingleCallHandler h = new SingleCallHandler();
        // setting current time
        h.mLastCall = currentCallTime;
        synchronized (sMapLock) {
            sHandlers.put(tag, h);
        }
        // calling first time
        handler.onCall();
    }

    public interface CallHandler {
        void onCall();
    }


}
