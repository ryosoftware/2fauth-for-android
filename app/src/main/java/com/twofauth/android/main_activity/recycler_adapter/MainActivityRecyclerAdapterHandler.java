package com.twofauth.android.main_activity.recycler_adapter;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.twofauth.android.main_activity.MainActivityRecyclerAdapter;

import org.jetbrains.annotations.NotNull;

public class MainActivityRecyclerAdapterHandler extends Handler {
    public static final int REDRAW_ITEM_EACH_TIME_TO_TIME = 1;

    private static class MainActivityRecyclerAdapterHandlerObject {
        public final MainActivityRecyclerAdapter adapter;

        MainActivityRecyclerAdapterHandlerObject(@NotNull final MainActivityRecyclerAdapter _adapter) {
            adapter = _adapter;
        }
    }

    private static class MainActivityRecyclerAdapterHandlerForRedrawItems extends MainActivityRecyclerAdapterHandlerObject {
        public final int[] items;

        MainActivityRecyclerAdapterHandlerForRedrawItems(@NotNull final MainActivityRecyclerAdapter _adapter, @NotNull final int[] _items) {
            super(_adapter);
            System.arraycopy(_items, 0, items = new int[_items.length], 0, _items.length);
        }
    }

    private static class MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime extends MainActivityRecyclerAdapterHandlerObject {
        public final int item;

        public long endTime;

        MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime(@NotNull final MainActivityRecyclerAdapter _adapter, final int _item, final long end_time) {
            super(_adapter);
            item = _item;
            endTime = end_time;
        }
    }

    public MainActivityRecyclerAdapterHandler() {
        super();
    }

    public void handleMessage(@NotNull final Message message) {
        if (message.what == REDRAW_ITEM_EACH_TIME_TO_TIME) {
            final MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime redraw_item_data = (MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime) message.obj;
            if ( SystemClock.elapsedRealtime() > redraw_item_data.endTime)  {
                redraw_item_data.adapter.onClick(redraw_item_data.item);
            }
            else {
                redraw_item_data.adapter.notifyItemChanged(redraw_item_data.item);
                sendRedrawItemTimeToTimeMessage(redraw_item_data);
            }
        }
    }

    private void sendRedrawItemTimeToTimeMessage(@NotNull final MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime redraw_item_data) {
        sendMessage(obtainMessage(REDRAW_ITEM_EACH_TIME_TO_TIME, redraw_item_data));

    }
    public void sendRedrawItemTimeToTimeMessage(@NotNull final MainActivityRecyclerAdapter adapter, final int item, final long time) {
        sendRedrawItemTimeToTimeMessage(new MainActivityRecyclerAdapterHandlerForRedrawItemsTimeToTime(adapter, item, SystemClock.elapsedRealtime() + time));
    }

    public void removeRedrawItemEachTimeToTimeMessages() {
        removeMessages(REDRAW_ITEM_EACH_TIME_TO_TIME);
    }

    public void removeAllMessages() {
        removeRedrawItemEachTimeToTimeMessages();
    }
}
