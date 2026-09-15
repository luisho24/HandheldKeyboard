package com.liskovsoft.leankeyboard.ime;

import android.view.MotionEvent;

/**
 * Tiny process-local bridge from the IME to the optional pointer accessibility service.
 * It keeps the pointer's right-stick input separate from keyboard navigation.
 */
public final class PointerInputBridge {
    public interface MotionSink {
        boolean onPointerMotion(MotionEvent event);
    }

    private static volatile MotionSink sMotionSink;

    private PointerInputBridge() { }

    public static void register(MotionSink motionSink) {
        sMotionSink = motionSink;
    }

    public static void unregister(MotionSink motionSink) {
        if (sMotionSink == motionSink) sMotionSink = null;
    }

    public static boolean forward(MotionEvent event) {
        MotionSink sink = sMotionSink;
        return sink != null && sink.onPointerMotion(event);
    }
}
