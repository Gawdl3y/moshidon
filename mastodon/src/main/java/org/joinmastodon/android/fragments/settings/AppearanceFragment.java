package org.joinmastodon.android.fragments.settings;

import android.os.Build;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.model.ContentType;

import java.util.ArrayList;

public class AppearanceFragment extends SettingsBaseFragment {

    @Override
    public void addItems(ArrayList<Item> items) {
        themeItem = new ThemeItem();
        items.add(themeItem);
        items.add(new ButtonItem(R.string.sk_settings_color_palette, R.drawable.ic_fluent_color_24_regular, b -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), b, Gravity.CENTER_HORIZONTAL);
            popupMenu.inflate(R.menu.color_palettes);
            popupMenu.getMenu().findItem(R.id.m3_color).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
            popupMenu.setOnMenuItemClickListener(this::onColorPreferenceClick);

            Menu colorTheme = popupMenu.getMenu();
            colorTheme.findItem(getColorThemeRes(GlobalUserPreferences.color)).setChecked(true);

            b.setOnTouchListener(popupMenu.getDragToOpenListener());
            b.setOnClickListener(v -> popupMenu.show());
            b.setText(switch (GlobalUserPreferences.color) {
                case MATERIAL3 -> R.string.sk_color_palette_material3;
                case PINK -> R.string.sk_color_palette_pink;
                case PURPLE -> R.string.sk_color_palette_purple;
                case GREEN -> R.string.sk_color_palette_green;
                case BLUE -> R.string.sk_color_palette_blue;
                case BROWN -> R.string.sk_color_palette_brown;
                case RED -> R.string.sk_color_palette_red;
                case YELLOW -> R.string.sk_color_palette_yellow;
                case NORD -> R.string.mo_color_palette_nord;
            });
        }));
        items.add(new SwitchItem(R.string.theme_true_black, R.string.mo_setting_true_black_summary, R.drawable.ic_fluent_dark_theme_24_regular, GlobalUserPreferences.trueBlackTheme, this::onTrueBlackThemeChanged));
        items.add(new SwitchItem(R.string.sk_disable_marquee, R.drawable.ic_fluent_text_more_24_regular, GlobalUserPreferences.disableMarquee, i -> {
            GlobalUserPreferences.disableMarquee = i.checked;
            GlobalUserPreferences.save();
            needAppRestart = true;
        }));
        items.add(new SwitchItem(R.string.sk_settings_reduce_motion, R.string.mo_setting_reduced_motion_summary, R.drawable.ic_fluent_star_emphasis_24_regular, GlobalUserPreferences.reduceMotion, i -> {
            GlobalUserPreferences.reduceMotion = i.checked;
            GlobalUserPreferences.save();
            needAppRestart = true;
        }));
    }

    public static int getColorThemeRes(@Nullable GlobalUserPreferences.ColorPreference colorPreference) {
        return colorPreference == null ? R.id.content_type_null : switch(colorPreference) {
            case MATERIAL3 -> R.id.m3_color;
            case PINK -> R.id.pink_color;
            case PURPLE -> R.id.purple_color;
            case GREEN -> R.id.green_color;
            case BLUE -> R.id.blue_color;
            case BROWN -> R.id.brown_color;
            case RED -> R.id.red_color;
            case YELLOW -> R.id.yellow_color;
            case NORD -> R.id.nord_color;
        };
    }

    private boolean onColorPreferenceClick(MenuItem item) {
            GlobalUserPreferences.ColorPreference pref = null;
            int id = item.getItemId();

            if (id == R.id.m3_color) pref = GlobalUserPreferences.ColorPreference.MATERIAL3;
            else if (id == R.id.pink_color) pref = GlobalUserPreferences.ColorPreference.PINK;
            else if (id == R.id.purple_color) pref = GlobalUserPreferences.ColorPreference.PURPLE;
            else if (id == R.id.green_color) pref = GlobalUserPreferences.ColorPreference.GREEN;
            else if (id == R.id.blue_color) pref = GlobalUserPreferences.ColorPreference.BLUE;
            else if (id == R.id.brown_color) pref = GlobalUserPreferences.ColorPreference.BROWN;
            else if (id == R.id.red_color) pref = GlobalUserPreferences.ColorPreference.RED;
            else if (id == R.id.yellow_color) pref = GlobalUserPreferences.ColorPreference.YELLOW;
            else if (id == R.id.nord_color) pref = GlobalUserPreferences.ColorPreference.NORD;

            if (pref == null) return false;

            GlobalUserPreferences.color=pref;
            GlobalUserPreferences.save();
            restartActivityToApplyNewTheme();
            return true;
    }
}

