package au.smap.fieldTask.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONObject;
import org.odk.collect.android.formentry.questions.QuestionDetails;
import org.odk.collect.android.widgets.QuestionWidget;

/*
 * Shows a conversation question, held as a json array of messages, as a read only chat
 * Messages are shown oldest first, received on the left and sent on the right
 * Matches the conversation display in the Smap console and webforms
 */
@SuppressLint("ViewConstructor")
public class ConversationWidget extends QuestionWidget {

    public ConversationWidget(Context context, QuestionDetails questionDetails, Dependencies dependencies) {
        super(context, dependencies, questionDetails);
        render();
    }

    @Override
    protected View onCreateWidgetView(Context context, FormEntryPrompt prompt, int answerFontSize) {
        LinearLayout chat = new LinearLayout(context);
        chat.setOrientation(LinearLayout.VERTICAL);

        String value = prompt.getAnswerText();
        if (value == null || value.trim().isEmpty()) {
            return chat;
        }

        JSONArray conv;
        try {
            conv = new JSONArray(value);
        } catch (Exception e) {
            // Not a conversation, show it as it is
            TextView text = new TextView(context);
            text.setText(value);
            chat.addView(text);
            return chat;
        }

        int maxWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.75);
        for (int i = 0; i < conv.length(); i++) {
            JSONObject msg = conv.optJSONObject(i);
            if (msg != null) {
                chat.addView(getBubble(context, msg, maxWidth, answerFontSize));
            }
        }
        return chat;
    }

    private View getBubble(Context context, JSONObject msg, int maxWidth, int answerFontSize) {
        boolean inbound = msg.optBoolean("inbound");
        String channel = msg.optString("channel", "sms");

        StringBuilder meta = new StringBuilder();
        meta.append(inbound ? "← " : "→ ")
                .append(channelName(context, channel));
        if (!msg.optString("ts").isEmpty()) {
            meta.append(" ").append(msg.optString("ts"));
        }
        if (!msg.optString("theirNumber").isEmpty()) {
            meta.append(" ").append(msg.optString("theirNumber"));
        }

        int[] colours = colours(inbound, channel);

        TextView metaView = new TextView(context);
        metaView.setText(meta.toString());
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        metaView.setTextColor(colours[1]);
        metaView.setContentDescription(context.getString(inbound
                ? org.odk.collect.strings.R.string.smap_conv_received
                : org.odk.collect.strings.R.string.smap_conv_sent) + " " + meta.substring(2));

        TextView msgView = new TextView(context);
        msgView.setText(msg.optString("msg"));
        msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, answerFontSize);
        msgView.setTextColor(colours[1]);
        msgView.setTextIsSelectable(true);

        int pad = dp(context, 8);
        LinearLayout bubble = new LinearLayout(context);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(pad, pad / 2, pad, pad / 2);
        GradientDrawable background = new GradientDrawable();
        background.setColor(colours[0]);
        background.setCornerRadius(dp(context, 10));
        bubble.setBackground(background);
        bubble.addView(metaView);
        bubble.addView(msgView);
        metaView.setMaxWidth(maxWidth);
        msgView.setMaxWidth(maxWidth);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = inbound ? Gravity.START : Gravity.END;
        params.bottomMargin = dp(context, 4);
        bubble.setLayoutParams(params);
        return bubble;
    }

    /*
     * Background and text colour, the same as the console
     */
    private int[] colours(boolean inbound, String channel) {
        switch (channel) {
            case "whatsapp":
                return inbound ? new int[]{Color.parseColor("#0b7a3e"), Color.WHITE}
                        : new int[]{Color.parseColor("#dcf8c6"), Color.BLACK};
            case "email":
                return inbound ? new int[]{Color.parseColor("#7a5a3c"), Color.WHITE}
                        : new int[]{Color.parseColor("#e5d3b3"), Color.BLACK};
            default:
                return inbound ? new int[]{Color.parseColor("#0a6fae"), Color.WHITE}
                        : new int[]{Color.parseColor("#7993a3"), Color.BLACK};
        }
    }

    private String channelName(Context context, String channel) {
        if (channel.equals("whatsapp")) {
            return "WhatsApp";
        } else if (channel.equals("email")) {
            return context.getString(org.odk.collect.strings.R.string.smap_conv_email);
        }
        return "SMS";
    }

    private int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public IAnswerData getAnswer() {
        // Read only, the conversation is returned unchanged
        return getFormEntryPrompt().getAnswerValue() == null
                ? null
                : new StringData(getFormEntryPrompt().getAnswerText());
    }

    @Override
    public void clearAnswer() {
        // Read only, a conversation is only changed by messages
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
    }
}
