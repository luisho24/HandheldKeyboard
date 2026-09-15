package com.liskovsoft.leankeyboard.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.liskovsoft.leankeyboard.ime.PointerInputBridge;
import com.liskovsoft.leankeyboard.utils.HandheldPointerPreferences;
import com.liskovsoft.leankeyboard.utils.LeanKeyPreferences;
import com.liskovsoft.leankeyboard.utils.PointerChord;

import java.util.List;

/**
 * Opt-in system pointer for handheld controllers. It never reads accessibility window
 * content: the service only draws its own overlay and sends the action the user asks for.
 */
public class HandheldPointerAccessibilityService extends AccessibilityService
        implements PointerInputBridge.MotionSink {
    private static final float DEAD_ZONE = .16f;
    private static final int CURSOR_SIZE_DP = 46;
    private static final int DPAD_STEP_DP = 38;

    private WindowManager mWindowManager;
    private PointerView mPointerView;
    private WindowManager.LayoutParams mWindowParams;
    private int mCursorX;
    private int mCursorY;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean mOverlayAttached;
    private final PointerChord mPointerChord = new PointerChord();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        refreshScreenBounds();
        PointerInputBridge.register(this);
        // Pointer mode is deliberately session-only. A service reconnect must never leave
        // ordinary gamepad buttons captured after the cursor has disappeared.
        HandheldPointerPreferences.setPointerActive(this, false);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately empty: this pointer does not inspect user interface content or text.
    }

    @Override
    public void onInterrupt() {
        // Accessibility may interrupt feedback when windows change. The cursor is a visual
        // overlay, not feedback, so keep it attached while pointer mode remains active.
    }

    @Override
    public void onDestroy() {
        PointerInputBridge.unregister(this);
        hidePointer();
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!HandheldPointerPreferences.isControlsEnabled(this)) return false;
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        int chordFirst = HandheldPointerPreferences.getChordFirst(this);
        int chordSecond = HandheldPointerPreferences.getChordSecond(this);

        // The configured chord buttons are intentionally reserved. Handling their full
        // down/up lifecycle prevents a half chord reaching the keyboard or foreground app.
        if (mPointerChord.handles(keyCode, chordFirst, chordSecond)) {
            if (mPointerChord.onKeyEvent(keyCode, action == KeyEvent.ACTION_DOWN,
                    chordFirst, chordSecond)) {
                setPointerActive(!HandheldPointerPreferences.isPointerActive(this));
            }
            return true;
        }

        if (!HandheldPointerPreferences.isPointerActive(this)) return false;
        // Do not consume a controller button unless the corresponding cursor is visible.
        // This preserves normal gamepad input when an OEM temporarily rejects the overlay.
        if (!ensurePointerVisible()) return false;
        if (isDpad(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN) moveByDpad(keyCode);
            return true;
        }

        if (keyCode == HandheldPointerPreferences.getClickButton(this)) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) tap();
            return true;
        }
        if (keyCode == HandheldPointerPreferences.getBackButton(this)) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                pulse(false);
            }
            return true;
        }
        if (keyCode == HandheldPointerPreferences.getScrollUpButton(this)) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) scroll(false);
            return true;
        }
        if (keyCode == HandheldPointerPreferences.getScrollDownButton(this)) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) scroll(true);
            return true;
        }
        return false;
    }

    /** Called by the IME while it is visible. Only right-stick axes are accepted. */
    @Override
    public boolean onPointerMotion(MotionEvent event) {
        if (!HandheldPointerPreferences.isControlsEnabled(this) ||
                !HandheldPointerPreferences.isPointerActive(this) ||
                event.getActionMasked() != MotionEvent.ACTION_MOVE) return false;
        // Retroid and other Android handhelds may expose their D-pad as HAT axes and
        // their sticks as X/Y, Z/RZ, or RX/RY. Accept all of these while pointer mode is
        // active so the input cannot fall through to keyboard navigation.
        float x = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float y = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        int speed = HandheldPointerPreferences.getSpeed(this);
        if (Math.abs(x) >= DEAD_ZONE || Math.abs(y) >= DEAD_ZONE) {
            int hatStep = dp(DPAD_STEP_DP + speed * 9);
            moveTo(mCursorX + Math.round(x * hatStep), mCursorY + Math.round(y * hatStep), true);
            return true;
        }
        x = event.getAxisValue(MotionEvent.AXIS_Z);
        y = event.getAxisValue(MotionEvent.AXIS_RZ);
        if (Math.abs(x) < DEAD_ZONE && Math.abs(y) < DEAD_ZONE) {
            x = event.getAxisValue(MotionEvent.AXIS_RX);
            y = event.getAxisValue(MotionEvent.AXIS_RY);
        }
        if (Math.abs(x) < DEAD_ZONE && Math.abs(y) < DEAD_ZONE) {
            x = event.getAxisValue(MotionEvent.AXIS_X);
            y = event.getAxisValue(MotionEvent.AXIS_Y);
        }
        if (Math.abs(x) < DEAD_ZONE && Math.abs(y) < DEAD_ZONE) return false;
        int step = dp(speed == 0 ? 8 : speed == 2 ? 22 : 14);
        moveTo(mCursorX + Math.round(x * step), mCursorY + Math.round(y * step), false);
        return true;
    }

    private void setPointerActive(boolean active) {
        HandheldPointerPreferences.setPointerActive(this, active);
        if (active) {
            showPointer(true);
            if (!mOverlayAttached) {
                // Do not leave the controller in a hidden input-capturing state.
                HandheldPointerPreferences.setPointerActive(this, false);
                Toast.makeText(this, getString(com.liskovsoft.leankeykeyboard.R.string.handheld_pointer_unavailable),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            hidePointer();
        }
        Toast.makeText(this, getString(active
                        ? com.liskovsoft.leankeykeyboard.R.string.handheld_pointer_on
                        : com.liskovsoft.leankeykeyboard.R.string.handheld_pointer_off),
                Toast.LENGTH_SHORT).show();
    }

    private boolean ensurePointerVisible() {
        if (!mOverlayAttached) showPointer(false);
        return mOverlayAttached;
    }

    private void showPointer(boolean entering) {
        if (mWindowManager == null) return;
        refreshScreenBounds();
        if (mPointerView == null) {
            mPointerView = new PointerView(this);
            int size = dp(CURSOR_SIZE_DP);
            mWindowParams = new WindowManager.LayoutParams(size, size,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT);
            mWindowParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            mCursorX = mScreenWidth / 2;
            mCursorY = mScreenHeight / 2;
        }
        mPointerView.refreshStyle();
        if (!mOverlayAttached) {
            updateWindowPosition();
            try {
                mWindowManager.addView(mPointerView, mWindowParams);
                mOverlayAttached = true;
            } catch (RuntimeException ignored) {
                mOverlayAttached = false;
                return;
            }
        }
        if (entering && HandheldPointerPreferences.areAnimationsEnabled(this)) {
            mPointerView.setAlpha(0f);
            mPointerView.setScaleX(.55f);
            mPointerView.setScaleY(.55f);
            mPointerView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
    }

    private void hidePointer() {
        if (mOverlayAttached && mWindowManager != null && mPointerView != null) {
            mWindowManager.removeViewImmediate(mPointerView);
        }
        mOverlayAttached = false;
    }

    private void moveByDpad(int keyCode) {
        int step = dp(DPAD_STEP_DP + HandheldPointerPreferences.getSpeed(this) * 9);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: moveTo(mCursorX - step, mCursorY, true); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: moveTo(mCursorX + step, mCursorY, true); break;
            case KeyEvent.KEYCODE_DPAD_UP: moveTo(mCursorX, mCursorY - step, true); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: moveTo(mCursorX, mCursorY + step, true); break;
            default: break;
        }
    }

    private void moveTo(int x, int y, boolean dpad) {
        showPointer(false);
        int radius = dp(CURSOR_SIZE_DP) / 2;
        mCursorX = Math.max(radius, Math.min(mScreenWidth - radius, x));
        mCursorY = Math.max(radius, Math.min(mScreenHeight - radius, y));
        updateWindowPosition();
        if (dpad && HandheldPointerPreferences.areAnimationsEnabled(this)) {
            mPointerView.animate().cancel();
            mPointerView.setScaleX(.88f);
            mPointerView.setScaleY(.88f);
            mPointerView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }
    }

    private void updateWindowPosition() {
        if (mWindowParams == null || mPointerView == null || !mOverlayAttached && mWindowManager == null) return;
        int size = dp(CURSOR_SIZE_DP);
        mWindowParams.x = mCursorX - size / 2;
        mWindowParams.y = mCursorY - size / 2;
        if (mOverlayAttached) {
            try {
                mWindowManager.updateViewLayout(mPointerView, mWindowParams);
            } catch (RuntimeException ignored) {
                mOverlayAttached = false;
            }
        }
    }

    private void tap() {
        Path path = new Path();
        path.moveTo(mCursorX, mCursorY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 45)).build();
        dispatchGesture(gesture, null, null);
        pulse(true);
    }

    private void scroll(boolean down) {
        float startY = down ? mScreenHeight * .70f : mScreenHeight * .30f;
        float endY = down ? mScreenHeight * .30f : mScreenHeight * .70f;
        Path path = new Path();
        path.moveTo(mCursorX, startY);
        path.lineTo(mCursorX, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 220)).build();
        dispatchGesture(gesture, null, null);
        pulse(false);
    }

    private void pulse(boolean click) {
        if (mPointerView != null) mPointerView.pulse(click);
    }

    private void refreshScreenBounds() {
        if (mWindowManager == null) return;
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
        if (mScreenWidth == 0 || mScreenHeight == 0) {
            display.getSize(size);
            mScreenWidth = size.x;
            mScreenHeight = size.y;
        }
    }

    private boolean isDpad(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static boolean isEnabledInAndroid(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null &&
                    context.getPackageName().equals(info.getResolveInfo().serviceInfo.packageName) &&
                    HandheldPointerAccessibilityService.class.getName().equals(
                            info.getResolveInfo().serviceInfo.name)) return true;
        }
        return false;
    }

    private final class PointerView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int mAccent = Color.rgb(104, 235, 210);
        private int mRing = Color.WHITE;

        PointerView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void refreshStyle() {
            String theme = HandheldPointerPreferences.getTheme(HandheldPointerAccessibilityService.this);
            if (HandheldPointerPreferences.THEME_VIOLET.equals(theme)) mAccent = Color.rgb(183, 121, 255);
            else if (HandheldPointerPreferences.THEME_AMBER.equals(theme)) mAccent = Color.rgb(255, 184, 77);
            else if (HandheldPointerPreferences.THEME_MINT.equals(theme)) mAccent = Color.rgb(104, 235, 210);
            else mAccent = resolveKeyboardAccent();
            mRing = Color.argb(236, Color.red(mAccent), Color.green(mAccent), Color.blue(mAccent));
            invalidate();
        }

        private int resolveKeyboardAccent() {
            LeanKeyPreferences preferences = LeanKeyPreferences.instance(HandheldPointerAccessibilityService.this);
            String themeId = preferences.getCurrentTheme();
            if (LeanKeyPreferences.THEME_CUSTOM.equals(themeId)) {
                return preferences.getCustomThemeAccent();
            }
            int defaultAccent = getResources().getColor(com.liskovsoft.leankeykeyboard.R.color.candidate_font_color);
            if (LeanKeyPreferences.THEME_DEFAULT.equals(themeId)) return defaultAccent;
            int focusColor = getResources().getIdentifier("key_focus_color_" +
                    themeId.toLowerCase(java.util.Locale.ROOT), "color", getPackageName());
            int candidateColor = getResources().getIdentifier("candidate_font_color_" +
                    themeId.toLowerCase(java.util.Locale.ROOT), "color", getPackageName());
            return getResources().getColor(focusColor != 0 ? focusColor :
                    candidateColor != 0 ? candidateColor : com.liskovsoft.leankeykeyboard.R.color.candidate_font_color);
        }

        void pulse(boolean click) {
            if (!HandheldPointerPreferences.areAnimationsEnabled(HandheldPointerAccessibilityService.this)) return;
            animate().cancel();
            setScaleX(click ? 1.35f : 1.18f);
            setScaleY(click ? 1.35f : 1.18f);
            animate().scaleX(1f).scaleY(1f).setDuration(click ? 170 : 120).start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float center = getWidth() / 2f;
            float radius = getWidth() * .27f;
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.argb(45, Color.red(mAccent), Color.green(mAccent), Color.blue(mAccent)));
            mPaint.setShadowLayer(dp(5), 0, dp(1), Color.argb(170, 0, 0, 0));
            canvas.drawCircle(center, center, radius + dp(4), mPaint);
            mPaint.clearShadowLayer();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(dp(2));
            mPaint.setColor(mRing);
            canvas.drawCircle(center, center, radius, mPaint);
            mPaint.setStrokeWidth(dp(1));
            canvas.drawLine(center - radius - dp(5), center, center + radius + dp(5), center, mPaint);
            canvas.drawLine(center, center - radius - dp(5), center, center + radius + dp(5), mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mRing);
            canvas.drawCircle(center, center, dp(2), mPaint);
        }
    }
}
