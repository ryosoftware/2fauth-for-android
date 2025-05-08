package com.twofauth.android;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RepeatingEvents {
    public interface OnTickListener {
        public abstract void onTick(int identifier, long start_time, long end_time, long elapsed_time, Object data);
    }

    private static class RepeatingEventData {
        private final OnTickListener listener;

        private final long startTime;
        private final long repeatInterval;
        private final long endTime;

        private final Object mData;

        RepeatingEventData(@NotNull final OnTickListener _listener, final long repeat_interval, final long end_time, @Nullable final Object data) {
            listener = _listener;
            startTime = SystemClock.elapsedRealtime();
            repeatInterval = repeat_interval;
            endTime = end_time;
            mData = data;
        }

        public boolean onTick(final int identifier) {
            final long current_time = SystemClock.elapsedRealtime();
            listener.onTick(identifier, startTime, endTime, current_time - startTime, mData);
            return ((endTime > 0) && (current_time > endTime));
        }
    }

    private static class RepeatingEventsHandler extends Handler {
        RepeatingEventsHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(@NotNull final Message message) {
            synchronized (mData) {
                RepeatingEventData repeating_event_data = mData.get(message.what);
                if (repeating_event_data != null) {
                    if (repeating_event_data.onTick(message.what)) {
                        cancel(message.what);
                    }
                    else {
                        sendEmptyMessageDelayed(message.what, repeating_event_data.repeatInterval);
                    }
                }
            }
        }
    }

    private static final RepeatingEventsHandler mHandler = new RepeatingEventsHandler();
    private static final Map<Integer, RepeatingEventData> mData = new HashMap<Integer, RepeatingEventData>();
    private static int mIdentifier = 1;

    public static void cancel(final int identifier) {
        synchronized (mData) {
            mData.remove(identifier);
            mHandler.removeMessages(identifier);
        }
    }

    public static void start(final int identifier, @NotNull final OnTickListener listener, final long repeat_interval, final long end_time, @Nullable final Object data) {
        synchronized (mData) {
            cancel(identifier);
            mData.put(identifier, new RepeatingEventData(listener, repeat_interval, end_time > 0 ? SystemClock.elapsedRealtime() + end_time : 0, data));
            mHandler.sendEmptyMessageDelayed(identifier, repeat_interval);
        }
    }

    public static void start(final int identifier, @NotNull final OnTickListener listener, final long repeat_interval, @Nullable final Object data) {
        start(identifier, listener, repeat_interval, 0, data);
    }

    public static int obtainIdentifier() {
        synchronized (mData) {
            final int identifier = mIdentifier;
            mIdentifier ++;
            return identifier;
        }
    }
}
