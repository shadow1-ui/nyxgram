package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Locale;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

/** Live native voice/round-video queue. No recording, copying or extra media downloads. */
public final class NyxVoiceQueue extends BottomSheet {
    private final MediaController media = MediaController.getInstance();
    private final int account;
    private final long owner;
    private final TextView now;
    private final TextView elapsed;
    private final TextView play;
    private final TextView next;
    private final TextView speed;
    private final SeekBar seek;
    private final QueueAdapter adapter = new QueueAdapter();
    private ArrayList<MessageObject> queue = new ArrayList<>();
    private MessageObject current;
    private boolean dragging;
    private boolean closed;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (closed) return;
            if (!refresh()) { dismiss(); return; }
            AndroidUtilities.runOnUIThread(this, 500);
        }
    };

    public static void show(Activity activity, Theme.ResourcesProvider resources) {
        MessageObject message = MediaController.getInstance().getPlayingMessageObject();
        if (!AndroidUtilities.isContextSafe(activity) || message == null
                || message.currentAccount != UserConfig.selectedAccount
                || !eligible(message)) return;
        NyxVoiceQueue sheet = new NyxVoiceQueue(activity, resources, message.currentAccount);
        sheet.show();
        sheet.ticker.run();
    }

    private static boolean eligible(MessageObject message) {
        return message != null && (message.isVoice() || message.isRoundVideo())
                && !message.isVoiceOnce() && !message.isRoundOnce() && !message.nyxDeleted;
    }

    private NyxVoiceQueue(Context context, Theme.ResourcesProvider resources, int account) {
        super(context, false, resources);
        this.account = account;
        owner = UserConfig.getInstance(account).getClientUserId();
        setTitle(getString(R.string.NyxVoiceQueue), true);
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(16));
        body.addView(label(getString(R.string.NyxQueueNow), 13, true));
        now = label("", 17, false);
        now.setTypeface(AndroidUtilities.bold());
        now.setMaxLines(2);
        now.setEllipsize(TextUtils.TruncateAt.END);
        body.addView(now, new LinearLayout.LayoutParams(-1, dp(60)));
        seek = new SeekBar(context);
        seek.setMax(1000);
        int accent = Theme.getColor(Theme.key_dialogTextBlue, resources);
        seek.setProgressTintList(ColorStateList.valueOf(accent));
        seek.setThumbTintList(ColorStateList.valueOf(accent));
        seek.setContentDescription(getString(R.string.NyxQueueSeek));
        body.addView(seek, new LinearLayout.LayoutParams(-1, dp(36)));
        elapsed = label("", 13, true);
        body.addView(elapsed);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) { dragging = true; }
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                if (validSession() && media.getPlayingMessageObject() == current) {
                    media.seekToProgress(current, bar.getProgress() / 1000f);
                }
            }
        });
        LinearLayout controls = new LinearLayout(context);
        speed = control("1×");
        play = control(getString(R.string.NyxQueuePause));
        next = control(getString(R.string.NyxQueueNext));
        for (TextView button : new TextView[]{speed, play, next}) {
            controls.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        body.addView(controls);
        play.setOnClickListener(v -> {
            if (!refresh()) { dismiss(); return; }
            if (media.isMessagePaused()) media.playMessage(current); else media.pauseMessage(current);
            refresh();
        });
        speed.setOnClickListener(v -> {
            if (!refresh()) { dismiss(); return; }
            float value = media.getPlaybackSpeed(false);
            media.setPlaybackSpeed(false, value < 1.25f ? 1.5f : value < 1.75f ? 2f : 1f);
            refresh();
        });
        next.setOnClickListener(v -> {
            if (!refresh()) { dismiss(); return; }
            int index = queue.indexOf(current) + 1;
            if (index > 0 && index < queue.size()) select(queue.get(index));
        });
        body.addView(label(getString(R.string.NyxQueueHint), 13, true));
        ListView list = new ListView(context);
        list.setDivider(null);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, row, position, id) -> {
            if (position < queue.size()) select(queue.get(position));
        });
        TextView empty = label(getString(R.string.NyxQueueEmpty), 14, true);
        body.addView(empty);
        list.setEmptyView(empty);
        body.addView(list, new LinearLayout.LayoutParams(-1, Math.min(dp(280), AndroidUtilities.displaySize.y / 3)));
        setCustomView(body);
    }

    private boolean validSession() {
        return !closed && account == UserConfig.selectedAccount
                && owner != 0 && owner == UserConfig.getInstance(account).getClientUserId();
    }

    private boolean refresh() {
        MessageObject playing = media.getPlayingMessageObject();
        if (!validSession() || !eligible(playing) || playing.currentAccount != account) return false;
        ArrayList<MessageObject> snapshot = media.getVoiceMessagesPlaylist();
        boolean changed = current != playing || !queue.equals(snapshot);
        current = playing;
        queue = snapshot;
        now.setText(title(current));
        play.setText(getString(media.isMessagePaused() ? R.string.NyxQueuePlay : R.string.NyxQueuePause));
        speed.setText(String.format(Locale.US, "%s×", media.getPlaybackSpeed(false)));
        int nextIndex = queue.indexOf(current) + 1;
        next.setEnabled(nextIndex > 0 && nextIndex < queue.size());
        next.setAlpha(next.isEnabled() ? 1f : .4f);
        if (!dragging) seek.setProgress(Math.round(current.audioProgress * 1000));
        String position = duration(current.audioProgressSec);
        String total = duration((int) current.getDuration());
        elapsed.setText(position + " / " + total);
        seek.setContentDescription(LocaleController.formatString(R.string.AccDescrPlayerDuration, position, total));
        if (changed) adapter.notifyDataSetChanged();
        return true;
    }

    private void select(MessageObject message) {
        if (!refresh()) { dismiss(); return; }
        if (message == current) { play.performClick(); return; }
        if (!media.playVoiceMessageFromQueue(message)) {
            Toast.makeText(getContext(), getString(R.string.NyxQueueUnavailable), Toast.LENGTH_SHORT).show();
        }
        if (!refresh()) dismiss();
    }

    private String title(MessageObject message) {
        return getString(message.isRoundVideo() ? R.string.NyxQueueRound : R.string.NyxQueueVoice)
                + " · " + LocaleController.formatDateAudio(message.messageOwner.date, true);
    }

    private static String duration(int seconds) {
        seconds = Math.max(0, seconds);
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private TextView label(CharSequence text, int size, boolean secondary) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(Theme.getColor(secondary ? Theme.key_dialogTextGray : Theme.key_dialogTextBlack, resourcesProvider));
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }

    private TextView control(CharSequence text) {
        TextView view = label(text, 14, false);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Theme.getColor(Theme.key_dialogTextBlue, resourcesProvider));
        view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), 7));
        return view;
    }

    @Override public void dismiss() {
        closed = true;
        AndroidUtilities.cancelRunOnUIThread(ticker);
        super.dismiss();
    }

    private final class QueueAdapter extends BaseAdapter {
        @Override public int getCount() { return queue.size(); }
        @Override public MessageObject getItem(int position) { return queue.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View reuse, ViewGroup parent) {
            TextView row = reuse instanceof TextView ? (TextView) reuse : label("", 14, false);
            MessageObject message = getItem(position);
            boolean active = message == current;
            row.setText((active ? "▶  " : "") + title(message) + "\n" + duration((int) message.getDuration()));
            row.setTextColor(Theme.getColor(active ? Theme.key_dialogTextBlue : Theme.key_dialogTextBlack, resourcesProvider));
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setMinHeight(dp(72));
            row.setMaxLines(3);
            row.setEllipsize(TextUtils.TruncateAt.END);
            row.setBackground(Theme.createRoundRectDrawable(dp(12), Theme.getColor(
                    active ? Theme.key_listSelector : Theme.key_dialogBackground, resourcesProvider)));
            row.setContentDescription((active ? getString(R.string.NyxQueueNow) + ", " : "") + row.getText());
            return row;
        }
    }
}
