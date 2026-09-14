package com.liskovsoft.leankeyboard.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Small synthesized sound bank for controller navigation and key actions. */
public final class KeyboardSoundPlayer {
    private static final int SAMPLE_RATE = 22050;
    private static final int SAMPLE_BYTES = 2;
    private static final int MAX_CHOICES = 5; // Default, tap, double, chime, bleep.
    private static KeyboardSoundPlayer sInstance;

    private final Context mContext;
    private final KeyboardSoundSettings mSettings;
    private SoundPool mSoundPool;
    private int[][] mSoundIds;
    private final Set<Integer> mLoadedIds = new HashSet<>();
    private int mLoadedStyle = -1;
    private int mLastStreamId;
    private int mPendingPreviewSoundId;
    private long mLastNavigationSoundAt;

    private KeyboardSoundPlayer(Context context) {
        mContext = context.getApplicationContext();
        mSettings = new KeyboardSoundSettings(mContext);
    }

    public static synchronized KeyboardSoundPlayer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyboardSoundPlayer(context);
        }
        return sInstance;
    }

    /** Prepares samples after settings change so the first keyboard input is not lost to loading. */
    public synchronized void prepare() {
        ensureLoaded(mSettings.getStyle());
    }

    public synchronized void playEvent(int event) {
        playEvent(event, false);
    }

    /** Preview is allowed from the settings screen even while effect playback is disabled. */
    public synchronized void previewEvent(int event) {
        playEvent(event, true);
    }

    private void playEvent(int event, boolean preview) {
        if (event < 0 || event >= KeyboardSoundSettings.EVENT_COUNT || (!preview && !mSettings.isEnabled())) {
            return;
        }

        int choice = mSettings.getSound(event);
        if (choice == KeyboardSoundSettings.SOUND_SILENT) {
            stopLastStream();
            return;
        }

        ensureLoaded(mSettings.getStyle());
        if (mSoundPool == null || mSoundIds == null) {
            return;
        }

        int choiceIndex = toChoiceIndex(choice);
        int soundId = mSoundIds[event][choiceIndex];
        if (soundId == 0) {
            return;
        }
        if (!mLoadedIds.contains(soundId)) {
            if (preview) {
                mPendingPreviewSoundId = soundId;
            }
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (event == KeyboardSoundSettings.EVENT_NAVIGATION && now - mLastNavigationSoundAt < 42) {
            return;
        }
        if (event == KeyboardSoundSettings.EVENT_NAVIGATION) {
            mLastNavigationSoundAt = now;
        }

        startSound(soundId, mSettings.getVolume());
    }

    private void startSound(int soundId, int volumePercent) {
        if (mSoundPool == null) {
            return;
        }
        stopLastStream();
        float volume = Math.max(0, Math.min(100, volumePercent)) / 100f;
        mLastStreamId = mSoundPool.play(soundId, volume, volume, 1, 0, 1.0f);
    }

    private void ensureLoaded(int style) {
        if (mSoundPool != null && mLoadedStyle == style) {
            return;
        }

        releasePool();
        try {
            // The legacy constructor supports the app's API 14 minimum.
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
            final SoundPool pool = mSoundPool;
            mLoadedStyle = style;
            mSoundIds = new int[KeyboardSoundSettings.EVENT_COUNT][MAX_CHOICES];
            mLoadedIds.clear();
            pool.setOnLoadCompleteListener((loadedPool, soundId, status) -> {
                synchronized (KeyboardSoundPlayer.this) {
                    if (loadedPool == mSoundPool && status == 0) {
                        mLoadedIds.add(soundId);
                        if (mPendingPreviewSoundId == soundId) {
                            mPendingPreviewSoundId = 0;
                            startSound(soundId, mSettings.getVolume());
                        }
                    }
                }
            });

            File directory = new File(mContext.getCacheDir(), "keyboard-sound-bank");
            if (!directory.exists() && !directory.mkdirs()) {
                releasePool();
                return;
            }

            for (int event = 0; event < KeyboardSoundSettings.EVENT_COUNT; event++) {
                for (int choiceIndex = 0; choiceIndex < MAX_CHOICES; choiceIndex++) {
                    File sample = new File(directory, "s" + style + "_e" + event + "_c" + choiceIndex + ".wav");
                    if (!sample.exists() || sample.length() == 0) {
                        writeSample(sample, style, event, choiceIndex);
                    }
                    if (sample.length() > 0) {
                        mSoundIds[event][choiceIndex] = pool.load(sample.getAbsolutePath(), 1);
                    }
                }
            }
        } catch (RuntimeException | IOException error) {
            releasePool();
        }
    }

    private void releasePool() {
        if (mSoundPool != null) {
            try {
                mSoundPool.setOnLoadCompleteListener(null);
                mSoundPool.release();
            } catch (RuntimeException ignored) {
                // Audio can disappear while the IME is being reconfigured.
            }
        }
        mSoundPool = null;
        mSoundIds = null;
        mLoadedStyle = -1;
        mLoadedIds.clear();
        mLastStreamId = 0;
        mPendingPreviewSoundId = 0;
    }

    private void stopLastStream() {
        if (mSoundPool != null && mLastStreamId != 0) {
            try {
                mSoundPool.stop(mLastStreamId);
            } catch (RuntimeException ignored) {
                // A stream may finish between adjacent key events.
            }
            mLastStreamId = 0;
        }
    }

    private static int toChoiceIndex(int choice) {
        switch (choice) {
            case KeyboardSoundSettings.SOUND_TAP:
                return 1;
            case KeyboardSoundSettings.SOUND_DOUBLE:
                return 2;
            case KeyboardSoundSettings.SOUND_CHIME:
                return 3;
            case KeyboardSoundSettings.SOUND_BLEEP:
                return 4;
            case KeyboardSoundSettings.SOUND_DEFAULT:
            default:
                return 0;
        }
    }

    private static void writeSample(File file, int style, int event, int choiceIndex) throws IOException {
        int choice = choiceIndex == 0 ? KeyboardSoundSettings.SOUND_DEFAULT : choiceIndex;
        int pattern = choice == KeyboardSoundSettings.SOUND_DEFAULT ? defaultPattern(event) : choice;
        float baseFrequency = baseFrequency(event);
        float duration;
        switch (pattern) {
            case KeyboardSoundSettings.SOUND_DOUBLE:
                duration = 0.145f;
                break;
            case KeyboardSoundSettings.SOUND_CHIME:
                duration = 0.155f;
                break;
            case KeyboardSoundSettings.SOUND_BLEEP:
                duration = 0.145f;
                break;
            case KeyboardSoundSettings.SOUND_TAP:
            default:
                duration = event == KeyboardSoundSettings.EVENT_NAVIGATION ? 0.055f : 0.080f;
                break;
        }

        int sampleCount = Math.max(1, (int) (SAMPLE_RATE * duration));
        byte[] pcm = new byte[sampleCount * SAMPLE_BYTES];
        for (int index = 0; index < sampleCount; index++) {
            float time = index / (float) SAMPLE_RATE;
            float amplitude = sampleAmplitude(time, pattern, event, style, baseFrequency);
            short value = (short) (Math.max(-1f, Math.min(1f, amplitude)) * 24000);
            pcm[index * 2] = (byte) (value & 0xff);
            pcm[index * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            writeAscii(output, "RIFF");
            writeInt(output, 36 + pcm.length);
            writeAscii(output, "WAVEfmt ");
            writeInt(output, 16);
            writeShort(output, 1); // PCM.
            writeShort(output, 1); // Mono.
            writeInt(output, SAMPLE_RATE);
            writeInt(output, SAMPLE_RATE * SAMPLE_BYTES);
            writeShort(output, SAMPLE_BYTES);
            writeShort(output, 16);
            writeAscii(output, "data");
            writeInt(output, pcm.length);
            output.write(pcm);
        }
    }

    private static float sampleAmplitude(float time, int pattern, int event, int style, float baseFrequency) {
        int pulseCount;
        float pulseLength;
        float gap;
        switch (pattern) {
            case KeyboardSoundSettings.SOUND_DOUBLE:
                pulseCount = 2;
                pulseLength = 0.050f;
                gap = 0.025f;
                break;
            case KeyboardSoundSettings.SOUND_CHIME:
                pulseCount = 2;
                pulseLength = 0.072f;
                gap = 0.004f;
                break;
            case KeyboardSoundSettings.SOUND_BLEEP:
                pulseCount = 3;
                pulseLength = 0.031f;
                gap = 0.014f;
                break;
            case KeyboardSoundSettings.SOUND_TAP:
            default:
                pulseCount = 1;
                pulseLength = time + 0.01f;
                gap = 0;
                break;
        }

        float cycle = pulseLength + gap;
        int pulse = Math.min(pulseCount - 1, (int) (time / cycle));
        float localTime = time - pulse * cycle;
        if (localTime > pulseLength) {
            return 0;
        }

        float frequency = baseFrequency;
        if (pattern == KeyboardSoundSettings.SOUND_CHIME && pulse == 1) {
            frequency *= event == KeyboardSoundSettings.EVENT_DELETE ? 0.79f : 1.26f;
        } else if (pattern == KeyboardSoundSettings.SOUND_DOUBLE && pulse == 1 &&
                event == KeyboardSoundSettings.EVENT_SHIFT) {
            frequency *= 1.22f;
        } else if (pattern == KeyboardSoundSettings.SOUND_BLEEP && pulse > 0) {
            frequency *= pulse == 1 ? 1.08f : 0.88f;
        }

        double phase = 2.0 * Math.PI * frequency * localTime;
        double sine = Math.sin(phase);
        double wave;
        switch (style) {
            case KeyboardSoundSettings.STYLE_ARCADE:
                wave = sine >= 0 ? 0.8 : -0.8;
                break;
            case KeyboardSoundSettings.STYLE_GLASS:
                wave = 0.72 * sine + 0.28 * Math.sin(phase * 2.0);
                break;
            case KeyboardSoundSettings.STYLE_MECHANICAL:
                wave = 0.65 * (2.0 * (phase / (2.0 * Math.PI) - Math.floor(phase / (2.0 * Math.PI) + 0.5))) +
                        0.35 * Math.sin(phase * 2.0);
                break;
            case KeyboardSoundSettings.STYLE_SOFT:
            default:
                wave = sine;
                break;
        }

        float attack = Math.min(1f, localTime / 0.004f);
        float envelope = attack * (float) Math.exp(-localTime * (pattern == KeyboardSoundSettings.SOUND_TAP ? 20 : 12));
        return (float) (wave * envelope * 0.72);
    }

    private static int defaultPattern(int event) {
        switch (event) {
            case KeyboardSoundSettings.EVENT_CONFIRM:
                return KeyboardSoundSettings.SOUND_CHIME;
            case KeyboardSoundSettings.EVENT_DELETE:
                return KeyboardSoundSettings.SOUND_BLEEP;
            case KeyboardSoundSettings.EVENT_SHIFT:
                return KeyboardSoundSettings.SOUND_DOUBLE;
            case KeyboardSoundSettings.EVENT_NAVIGATION:
            default:
                return KeyboardSoundSettings.SOUND_TAP;
        }
    }

    private static float baseFrequency(int event) {
        switch (event) {
            case KeyboardSoundSettings.EVENT_CONFIRM:
                return 740f;
            case KeyboardSoundSettings.EVENT_DELETE:
                return 440f;
            case KeyboardSoundSettings.EVENT_SHIFT:
                return 587f;
            case KeyboardSoundSettings.EVENT_NAVIGATION:
            default:
                return 988f;
        }
    }

    private static void writeAscii(FileOutputStream output, String value) throws IOException {
        output.write(value.getBytes("US-ASCII"));
    }

    private static void writeInt(FileOutputStream output, int value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private static void writeShort(FileOutputStream output, int value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }
}
