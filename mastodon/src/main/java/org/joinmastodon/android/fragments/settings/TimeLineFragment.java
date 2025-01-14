package org.joinmastodon.android.fragments.settings;

import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.PopupMenu;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;

import java.util.ArrayList;

public class TimeLineFragment extends SettingsBaseFragment{

    private SwitchItem showNewPostsItem, compactReblogReplyLineItem;
    @Override
    public void addItems(ArrayList<Item> items) {
        if (getInstance().pleroma != null) {
            items.add(new ButtonItem(R.string.sk_settings_reply_visibility, R.drawable.ic_fluent_chat_24_regular, b->{
                PopupMenu popupMenu=new PopupMenu(getActivity(), b, Gravity.CENTER_HORIZONTAL);
                popupMenu.inflate(R.menu.reply_visibility);
                popupMenu.setOnMenuItemClickListener(item -> this.onReplyVisibilityChanged(item, b));
                b.setOnTouchListener(popupMenu.getDragToOpenListener());
                b.setOnClickListener(v->popupMenu.show());
                b.setText(GlobalUserPreferences.replyVisibility == null ?
                        R.string.sk_settings_reply_visibility_all :
                        switch(GlobalUserPreferences.replyVisibility){
                            case "following" -> R.string.sk_settings_reply_visibility_following;
                            case "self" -> R.string.sk_settings_reply_visibility_self;
                            default -> R.string.sk_settings_reply_visibility_all;
                        });
            }));
        }
        items.add(new SwitchItem(R.string.sk_settings_show_replies, R.drawable.ic_fluent_chat_multiple_24_regular, GlobalUserPreferences.showReplies, i->{
            GlobalUserPreferences.showReplies=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.sk_settings_show_boosts, R.drawable.ic_fluent_arrow_repeat_all_24_regular, GlobalUserPreferences.showBoosts, i->{
            GlobalUserPreferences.showBoosts=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.sk_settings_load_new_posts, R.drawable.ic_fluent_arrow_sync_24_regular, GlobalUserPreferences.loadNewPosts, i->{
            GlobalUserPreferences.loadNewPosts=i.checked;
            showNewPostsItem.enabled = i.checked;
            if (!i.checked) {
                GlobalUserPreferences.showNewPostsButton = false;
                showNewPostsItem.checked = false;
            }
            if (list.findViewHolderForAdapterPosition(items.indexOf(showNewPostsItem)) instanceof SwitchViewHolder svh) svh.rebind();
            GlobalUserPreferences.save();
        }));
        items.add(showNewPostsItem = new SwitchItem(R.string.sk_settings_show_new_posts_button, R.drawable.ic_fluent_arrow_up_24_regular, GlobalUserPreferences.showNewPostsButton, i->{
            GlobalUserPreferences.showNewPostsButton=i.checked;
            GlobalUserPreferences.save();
        }));

        items.add(new SwitchItem(R.string.sk_settings_show_alt_indicator, R.drawable.ic_fluent_scan_text_24_regular, GlobalUserPreferences.showAltIndicator, i->{
            GlobalUserPreferences.showAltIndicator=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_show_no_alt_indicator, R.drawable.ic_fluent_important_24_regular, GlobalUserPreferences.showNoAltIndicator, i->{
            GlobalUserPreferences.showNoAltIndicator=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_collapse_long_posts, R.drawable.ic_fluent_chevron_down_24_regular, GlobalUserPreferences.collapseLongPosts, i->{
            GlobalUserPreferences.collapseLongPosts=i.checked;
            GlobalUserPreferences.save();
        }));
        items.add(new SwitchItem(R.string.sk_settings_hide_fab, R.drawable.ic_fluent_edit_24_regular, GlobalUserPreferences.autoHideFab, i->{
            GlobalUserPreferences.autoHideFab=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_reply_line_above_avatar, R.drawable.ic_fluent_arrow_reply_24_regular, GlobalUserPreferences.replyLineAboveHeader, i->{
            GlobalUserPreferences.replyLineAboveHeader=i.checked;
            GlobalUserPreferences.compactReblogReplyLine=i.checked;
            compactReblogReplyLineItem.enabled=i.checked;
            compactReblogReplyLineItem.checked= GlobalUserPreferences.replyLineAboveHeader;
            if (list.findViewHolderForAdapterPosition(items.indexOf(compactReblogReplyLineItem)) instanceof SwitchViewHolder svh) svh.rebind();
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(compactReblogReplyLineItem=new SwitchItem(R.string.sk_compact_reblog_reply_line, R.drawable.ic_fluent_re_order_24_regular, GlobalUserPreferences.compactReblogReplyLine, i->{
            GlobalUserPreferences.compactReblogReplyLine=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_show_interaction_counts, R.string.mo_setting_interaction_count_summary, R.drawable.ic_fluent_number_row_24_regular, GlobalUserPreferences.showInteractionCounts, i->{
            GlobalUserPreferences.showInteractionCounts=i.checked;
            GlobalUserPreferences.save();
        }));
        compactReblogReplyLineItem.enabled=GlobalUserPreferences.replyLineAboveHeader;
        items.add(new SwitchItem(R.string.sk_settings_hide_interaction, R.drawable.ic_fluent_eye_24_regular, GlobalUserPreferences.spectatorMode, i->{
            GlobalUserPreferences.spectatorMode=i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
        items.add(new SwitchItem(R.string.mo_enable_dividers, R.drawable.ic_fluent_timeline_24_regular, GlobalUserPreferences.showDividers, i->{
            GlobalUserPreferences.showDividers =i.checked;
            GlobalUserPreferences.save();
            needAppRestart=true;
        }));
    }

    private boolean onReplyVisibilityChanged(MenuItem item, Button btn){
        String pref = null;
        int id = item.getItemId();

        if (id == R.id.reply_visibility_following) pref = "following";
        else if (id == R.id.reply_visibility_self) pref = "self";

        GlobalUserPreferences.replyVisibility=pref;
        GlobalUserPreferences.save();
        btn.setText(GlobalUserPreferences.replyVisibility == null ?
                R.string.sk_settings_reply_visibility_all :
                switch(GlobalUserPreferences.replyVisibility){
                    case "following" -> R.string.sk_settings_reply_visibility_following;
                    case "self" -> R.string.sk_settings_reply_visibility_self;
                    default -> R.string.sk_settings_reply_visibility_all;
                });
        return true;
    }
}
