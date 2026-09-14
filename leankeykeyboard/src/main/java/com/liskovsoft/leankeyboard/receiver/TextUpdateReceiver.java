package com.liskovsoft.leankeyboard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TextUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Vendor IME broadcasts may contain user-entered text; do not log intent data.
    }
}
