package org.joinmastodon.android.fragments.settings;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.DomainManager;
import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.DomainDisplay;
import org.joinmastodon.android.fragments.InstanceInfoFragment;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.function.Consumer;

import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class SettingsBaseFragment extends MastodonToolbarFragment implements DomainDisplay {
	protected View view;
	protected UsableRecyclerView list;

	protected ImageView themeTransitionWindowView;

	protected ThemeItem themeItem;

	protected boolean needAppRestart;

	private Instance instance;
	private String instanceName;
	protected String title;

	protected NotificationPolicyItem notificationPolicyItem;

	protected PushSubscription pushSubscription;
	protected ArrayList<Item> items=new ArrayList<>();
	protected String accountID;
	protected AccountSession session;

	protected boolean needUpdateNotificationSettings;

	public abstract void addItems(ArrayList<Item> items);

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
		setTitle(R.string.settings);

		accountID=getArguments().getString("account");
		session = AccountSessionManager.getInstance().getAccount(accountID);
		instance = AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		instanceName = UiUtils.getInstanceName(accountID);
		DomainManager.getInstance().setCurrentDomain(session.domain + "/settings");

		if (GithubSelfUpdater.needSelfUpdating())
			E.register(this);



		addItems(items);
		title = getArguments().getString("title", getTitle().toString());
		items.add(0, new GiantHeaderItem(title));
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		list = new UsableRecyclerView(getActivity());
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(new SettingsAdapter());
		list.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.windowBackground));
		list.setPadding(0, V.dp(16), 0, V.dp(12));
		list.setClipToPadding(false);
		list.addItemDecoration(new RecyclerView.ItemDecoration() {
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
				// Add 32dp gaps between sections
				RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
				if ((holder instanceof HeaderViewHolder || holder instanceof FooterViewHolder) && holder.getAbsoluteAdapterPosition() > 1)
					outRect.top = V.dp(32);
			}
		});

		setTitle(title);

		list.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				LinearLayoutManager linearLayoutManager = (LinearLayoutManager) list.getLayoutManager();
				if (linearLayoutManager != null && linearLayoutManager.findFirstVisibleItemPosition() > 0) {
					showToolbarAnimated();
				} else {
					hideToolbarAnimated();
				}
			}
		});

		return list;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(12)+insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		this.view = view;
		hideToolbar();
		toolbarTitleView.setVisibility(View.INVISIBLE);
		setStatusBarColor(UiUtils.getThemeColor(getContext(), R.attr.colorWindowBackground));
	}

	protected void hideToolbar() {
		getToolbar().setElevation(0f);
        toolbarTitleView.setVisibility(View.INVISIBLE);
        TypedValue typedValue = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            getToolbar().setBackgroundColor(typedValue.data);
        }
	}

	protected void showToolbarAnimated(){
		AlphaAnimation fadeIn = new AlphaAnimation(0.0f , 1.0f ) ;
		fadeIn.setDuration(100);
		if(toolbarTitleView.getVisibility() != View.VISIBLE){
			getToolbar().setElevation(8.625f);
			toolbarTitleView.startAnimation(fadeIn);
			toolbarTitleView.setVisibility(View.VISIBLE);
		}
		getToolbar().setBackgroundColor(UiUtils.getThemeColor(getContext(), R.attr.colorBackgroundLight));
		setStatusBarColor(UiUtils.getThemeColor(getContext(), R.attr.colorBackgroundLight));
	}

	protected void hideToolbarAnimated(){
		AlphaAnimation fadeOut = new AlphaAnimation( 1.0f , 0.0f ) ;
		fadeOut.setDuration(100);
		if(toolbarTitleView.getVisibility() != View.INVISIBLE){
			getToolbar().setElevation(0f);
			toolbarTitleView.startAnimation(fadeOut);
			toolbarTitleView.setVisibility(View.INVISIBLE);
		}
		getToolbar().setBackgroundColor(UiUtils.getThemeColor(getContext(), R.attr.colorWindowBackground));
		setStatusBarColor(UiUtils.getThemeColor(getContext(), R.attr.colorWindowBackground));

	}

	protected Instance getInstance() {
		return instance;
	}

	protected String getInstanceName() {
		return instanceName;
	}


	static abstract class Item{
		public abstract int getViewType();
	}

	protected class HeaderItem extends Item{
		private String text;

		public HeaderItem(@StringRes int text){
			this.text=getString(text);
		}

		public HeaderItem(String text){
			this.text=text;
		}

		@Override
		public int getViewType(){
			return Type.HEADER.ordinal();
		}
	}

	protected class RedHeaderItem extends HeaderItem {

		public RedHeaderItem(int text){
			super(text);
		}

		public RedHeaderItem(String text){
			super(text);
		}

		@Override
		public int getViewType(){
			return Type.RED_HEADER.ordinal();
		}
	}


	protected class SwitchItem extends Item{
		private String title;
		private String summary;
		private int icon;
		boolean checked;
		private Consumer<SwitchItem> onChanged;
		protected boolean enabled=true;

		public SwitchItem(@StringRes int title, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged){
			this.title=getString(title);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
		}

		public SwitchItem(@StringRes int title, @StringRes int summary, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged){
			this.title=getString(title);
			this.summary=getString(summary);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
		}

		public SwitchItem(@StringRes int title, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged, boolean enabled){
			this.title=getString(title);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
			this.enabled=enabled;
		}

		@Override
		public int getViewType(){
			return Type.SWITCH.ordinal();
		}
	}

	protected class UpdateItem extends Item {

		@Override
		public int getViewType(){
			return Type.UPDATER.ordinal();
		}
	}

	protected static class ThemeItem extends Item {

		@Override
		public int getViewType(){
			return Type.THEME.ordinal();
		}
	}

	protected static class NotificationPolicyItem extends Item {

		@Override
		public int getViewType(){
			return Type.NOTIFICATION_POLICY.ordinal();
		}
	}


	protected class ButtonItem extends Item{
		private int title;
		private int summary;
		private int icon;
		private Consumer<Button> buttonConsumer;

		public ButtonItem(@StringRes int title, @DrawableRes int icon, Consumer<Button> buttonConsumer) {
			this.title = title;
			this.icon = icon;
			this.buttonConsumer = buttonConsumer;
		}

		public ButtonItem(@StringRes int title, @StringRes int summary, @DrawableRes int icon, Consumer<Button> buttonConsumer) {
			this.title = title;
			this.summary = summary;
			this.icon = icon;
			this.buttonConsumer = buttonConsumer;
		}

		@Override
		public int getViewType(){
			return Type.BUTTON.ordinal();
		}
	}

	protected class GiantHeaderItem extends Item {
		private String text;

		public GiantHeaderItem(String text) {
			this.text = text;
		}

		@Override
		public int getViewType() {
			return Type.GIANT_HEADER.ordinal();
		}
	}

	protected class SmallTextItem extends Item {
		private String text;

		public SmallTextItem(String text) {
			this.text = text;
		}

		@Override
		public int getViewType() {
			return Type.SMALL_TEXT.ordinal();
		}
	}

	protected class TextItem extends Item{
		private String text;
		protected String secondaryText;
		private Runnable onClick;
		protected boolean loading;
		private int icon;

		public TextItem(@StringRes int text, Runnable onClick) {
			this(text, null, onClick, false, 0);
		}

		public TextItem(@StringRes int text, Runnable onClick, @DrawableRes int icon) {
			this(text, null, onClick, false, icon);
		}

		public TextItem(@StringRes int text, String secondaryText, Runnable onClick, @DrawableRes int icon) {
			this(text, secondaryText, onClick, false, icon);
		}

		public TextItem(@StringRes int text, String secondaryText, Runnable onClick, boolean loading, @DrawableRes int icon){
			this.text=getString(text);
			this.onClick=onClick;
			this.loading=loading;
			this.icon=icon;
			this.secondaryText = secondaryText;
		}

		public TextItem(String text, Runnable onClick){
			this.text=text;
			this.onClick=onClick;
		}

		public TextItem(String text, Runnable onClick, @DrawableRes int icon){
			this.text=text;
			this.onClick=onClick;
			this.icon=icon;
		}

		@Override
		public int getViewType(){
			return Type.TEXT.ordinal();
		}
	}

	protected class SettingsCategoryItem extends Item{
		private String text;
		private int icon;

		private Class<? extends Fragment> fragmentClass;

        public SettingsCategoryItem(@StringRes int text, Class<? extends Fragment> fragmentClass, @DrawableRes int icon) {
            this.text = getString(text);
            this.fragmentClass=fragmentClass;
            this.icon=icon;
        }

        public SettingsCategoryItem(@StringRes int text, Class<? extends Fragment> fragmentClass) {
            this(text, fragmentClass, 0);
        }


		@Override
		public int getViewType(){
			return Type.SETTINGS_CATEGORY.ordinal();
		}
	}


	protected class FooterItem extends Item{
		private String text;
		private Runnable onClick;

		public FooterItem(String text, Runnable onClick){
			this.text=text;
			this.onClick=onClick;
		}

		@Override
		public int getViewType(){
			return Type.FOOTER.ordinal();
		}
	}

	public enum Type{
		HEADER,
		RED_HEADER,
		GIANT_HEADER,
		SWITCH,
		THEME,
		TEXT,
		NOTIFICATION_POLICY,
		FOOTER,
		BUTTON,
		SMALL_TEXT,
		UPDATER,
		SETTINGS_CATEGORY;
	}


	protected class SettingsAdapter extends RecyclerView.Adapter<BindableViewHolder<Item>>{

		@NonNull
		@Override
		public BindableViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			//noinspection unchecked
			return (BindableViewHolder<Item>) switch(Type.values()[viewType]){
				case HEADER -> new HeaderViewHolder();
				case RED_HEADER -> new HeaderViewHolder(true);
				case GIANT_HEADER -> new GiantHeaderViewHolder();
				case SWITCH -> new SwitchViewHolder();
				case THEME -> new ThemeViewHolder();
				case TEXT -> new TextViewHolder();
				case NOTIFICATION_POLICY -> new NotificationPolicyViewHolder();
				case FOOTER -> new FooterViewHolder();
				case BUTTON -> new ButtonViewHolder();
				case SMALL_TEXT -> new SmallTextViewHolder();
				case UPDATER -> new UpdateViewHolder();
				case SETTINGS_CATEGORY -> new SettingsCategoryViewHolder();
			};
		}

		@Override
		public void onBindViewHolder(@NonNull BindableViewHolder<Item> holder, int position){
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}

		@Override
		public int getItemViewType(int position){
			return items.get(position).getViewType();
		}
	}

	private class HeaderViewHolder extends BindableViewHolder<HeaderItem>{
		private final TextView text;
		public HeaderViewHolder(){
			super(getActivity(), R.layout.item_settings_header, list);
			text=(TextView) itemView;
		}

		public HeaderViewHolder(boolean red){
			super(getActivity(), R.layout.item_settings_header, list);
			text=(TextView) itemView;
			if(red)
				text.setTextColor(getResources().getColor(UiUtils.isDarkTheme() ? R.color.error_400 : R.color.error_700));
		}

		@Override
		public void onBind(HeaderItem item){
			text.setText(item.text);
		}
	}

	private class GiantHeaderViewHolder extends BindableViewHolder<GiantHeaderItem> {
		private final TextView text;

		public GiantHeaderViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
			text.setPaddingRelative(text.getPaddingStart(), 0, text.getPaddingEnd(), text.getPaddingBottom());
		}

		@Override
		public void onBind(GiantHeaderItem item){
			text.setText(item.text);
		}
	}


	protected void onThemePreferenceClick(GlobalUserPreferences.ThemePreference theme){
		GlobalUserPreferences.theme=theme;
		GlobalUserPreferences.save();
		restartActivityToApplyNewTheme();
	}

	protected void restartActivityToApplyNewTheme(){
		onDestroy();
		getActivity().recreate();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(needUpdateNotificationSettings && PushSubscriptionManager.arePushNotificationsAvailable()){
			AccountSessionManager.getInstance().getAccount(accountID).getPushSubscriptionManager().updatePushSettings(pushSubscription);
		}

		if (GithubSelfUpdater.needSelfUpdating())
			E.unregister(this);

		if(needAppRestart) UiUtils.restartApp();
	}


	protected void onTrueBlackThemeChanged(SwitchItem item){
		GlobalUserPreferences.trueBlackTheme=item.checked;
		GlobalUserPreferences.save();

		RecyclerView.ViewHolder themeHolder=list.findViewHolderForAdapterPosition(items.indexOf(themeItem));
		if(themeHolder!=null){
			((ThemeViewHolder)themeHolder).bindSubitems();
		}else{
			list.getAdapter().notifyItemChanged(items.indexOf(themeItem));
		}

		if(UiUtils.isDarkTheme()){
			restartActivityToApplyNewTheme();
		}
	}

	private class NotificationPolicyViewHolder extends BindableViewHolder<NotificationPolicyItem>{
		private final Button button;
		private final PopupMenu popupMenu;

		@SuppressLint("ClickableViewAccessibility")
		public NotificationPolicyViewHolder(){
			super(getActivity(), R.layout.item_settings_notification_policy, list);
			button=findViewById(R.id.button);
			popupMenu=new PopupMenu(getActivity(), button, Gravity.CENTER_HORIZONTAL);
			popupMenu.inflate(R.menu.notification_policy);
			popupMenu.setOnMenuItemClickListener(item->{
				PushSubscription.Policy policy;
				int id=item.getItemId();
				if(id==R.id.notify_anyone)
					policy=PushSubscription.Policy.ALL;
				else if(id==R.id.notify_followed)
					policy=PushSubscription.Policy.FOLLOWED;
				else if(id==R.id.notify_follower)
					policy=PushSubscription.Policy.FOLLOWER;
				else if(id==R.id.notify_none)
					policy=PushSubscription.Policy.NONE;
				else
					return false;
				onNotificationsPolicyChanged(policy);
				return true;
			});
			UiUtils.enablePopupMenuIcons(getActivity(), popupMenu);
			button.setOnTouchListener(popupMenu.getDragToOpenListener());
			button.setOnClickListener(v->popupMenu.show());
		}

		@Override
		public void onBind(NotificationPolicyItem item){
			button.setText(switch(getPushSubscription().policy){
				case ALL -> R.string.notify_anyone;
				case FOLLOWED -> R.string.notify_followed;
				case FOLLOWER -> R.string.notify_follower;
				case NONE -> R.string.notify_none;
			});
		}
	}

	private void onNotificationsPolicyChanged(PushSubscription.Policy policy){
		PushSubscription subscription=getPushSubscription();
		PushSubscription.Policy prevPolicy=subscription.policy;
		if(prevPolicy==policy)
			return;
		subscription.policy=policy;
		int index=items.indexOf(notificationPolicyItem);
		RecyclerView.ViewHolder policyHolder=list.findViewHolderForAdapterPosition(index);
		if(policyHolder!=null){
			((NotificationPolicyViewHolder)policyHolder).rebind();
		}else{
			list.getAdapter().notifyItemChanged(index);
		}
		if((prevPolicy==PushSubscription.Policy.NONE)!=(policy==PushSubscription.Policy.NONE)){
			boolean newState=policy!=PushSubscription.Policy.NONE;
			for(PushNotification.Type value : PushNotification.Type.values()){
				onNotificationsChanged(value, newState);
			}
			index++;
			while(items.size() > index && items.get(index) instanceof SwitchItem si){
				si.enabled=si.checked=newState;
				RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(index);
				if(holder!=null)
					((BindableViewHolder<?>)holder).rebind();
				else
					list.getAdapter().notifyItemChanged(index);
				index++;
			}
		}
		needUpdateNotificationSettings=true;
	}

	protected void onNotificationsChanged(PushNotification.Type type, boolean enabled){
		PushSubscription subscription=getPushSubscription();
		switch(type){
			case FAVORITE -> subscription.alerts.favourite=enabled;
			case FOLLOW -> subscription.alerts.follow=enabled;
			case REBLOG -> subscription.alerts.reblog=enabled;
			case MENTION -> subscription.alerts.mention=enabled;
			case POLL -> subscription.alerts.poll=enabled;
			case STATUS -> subscription.alerts.status=enabled;
			case UPDATE -> subscription.alerts.update=enabled;
		}
		needUpdateNotificationSettings=true;
	}

	protected PushSubscription getPushSubscription(){
		if(pushSubscription!=null)
			return pushSubscription;
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		if(session.pushSubscription==null){
			pushSubscription=new PushSubscription();
			pushSubscription.alerts=PushSubscription.Alerts.ofAll();
		}else{
			pushSubscription=session.pushSubscription.clone();
		}
		return pushSubscription;
	}

	protected class SwitchViewHolder extends BindableViewHolder<SwitchItem> implements UsableRecyclerView.DisableableClickable{
		private final TextView title;
		private final TextView summary;
		private final ImageView icon;
		private final Switch checkbox;

		public SwitchViewHolder(){
			super(getActivity(), R.layout.item_settings_switch, list);
			title=findViewById(R.id.title);
			summary=findViewById(R.id.summary);
			icon=findViewById(R.id.icon);
			checkbox=findViewById(R.id.checkbox);
		}

		@Override
		public void onBind(SwitchItem item){
			title.setText(item.title);

			if (item.summary != null) {
				summary.setText(item.summary);
				summary.setVisibility(View.VISIBLE);
			}

			if (item.icon == 0) {
				icon.setVisibility(View.GONE);
			} else {
				icon.setVisibility(View.VISIBLE);
				icon.setImageResource(item.icon);
			}
			checkbox.setChecked(item.checked && item.enabled);
			checkbox.setEnabled(item.enabled);
		}

		@Override
		public void onClick(){
			item.checked=!item.checked;
			checkbox.setChecked(item.checked);
			item.onChanged.accept(item);
		}

		@Override
		public boolean isEnabled(){
			return item.enabled;
		}
	}

	class ThemeViewHolder extends BindableViewHolder<ThemeItem>{
		private ThemeViewHolder.SubitemHolder autoHolder, lightHolder, darkHolder;

		public ThemeViewHolder(){
			super(getActivity(), R.layout.item_settings_theme, list);
			autoHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_auto));
			lightHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_light));
			darkHolder=new ThemeViewHolder.SubitemHolder(findViewById(R.id.theme_dark));
		}

		@Override
		public void onBind(ThemeItem item){
			bindSubitems();
		}

		public void bindSubitems(){
			autoHolder.bind(R.string.theme_auto, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_auto_trueblack : R.drawable.theme_auto, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO);
			lightHolder.bind(R.string.theme_light, R.drawable.theme_light, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.LIGHT);
			darkHolder.bind(R.string.theme_dark, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_dark_trueblack : R.drawable.theme_dark, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK);
		}

		private void onSubitemClick(View v){
			GlobalUserPreferences.ThemePreference pref;
			if(v.getId()==R.id.theme_auto)
				pref=GlobalUserPreferences.ThemePreference.AUTO;
			else if(v.getId()==R.id.theme_light)
				pref=GlobalUserPreferences.ThemePreference.LIGHT;
			else if(v.getId()==R.id.theme_dark)
				pref=GlobalUserPreferences.ThemePreference.DARK;
			else
				return;
			onThemePreferenceClick(pref);
		}

		private class SubitemHolder{
			public TextView text;
			public ImageView icon;
			public RadioButton checkbox;

			public SubitemHolder(View view){
				text=view.findViewById(R.id.text);
				icon=view.findViewById(R.id.icon);
				checkbox=view.findViewById(R.id.checkbox);
				view.setOnClickListener(ThemeViewHolder.this::onSubitemClick);

				icon.setClipToOutline(true);
				icon.setOutlineProvider(OutlineProviders.roundedRect(4));
			}

			public void bind(int text, int icon, boolean checked){
				this.text.setText(text);
				this.icon.setImageResource(icon);
				checkbox.setChecked(checked);
			}

			public void setChecked(boolean checked){
				checkbox.setChecked(checked);
			}
		}
	}

	private class SettingsCategoryViewHolder extends BindableViewHolder<SettingsCategoryItem> implements UsableRecyclerView.Clickable{
		private final ImageView icon;
		private final TextView text;

		@SuppressLint("ClickableViewAccessibility")
		public SettingsCategoryViewHolder(){
			super(getActivity(), R.layout.item_settings_category, list);
			text=findViewById(R.id.text);
			icon=findViewById(R.id.icon);
		}

		@Override
		public void onBind(SettingsCategoryItem item){
			text.setText(item.text);
			icon.setImageResource(item.icon);
		}

        @Override
        public void onClick() {
			Bundle args = getArguments();
			args.putString("title", item.text);
            Nav.go(getActivity(), item.fragmentClass, args);
        }
    }

	protected class ButtonViewHolder extends BindableViewHolder<ButtonItem> implements  UsableRecyclerView.Clickable{
		private final Button button;
		private final ImageView icon;
		private final TextView title;
		private final TextView summary;

		@SuppressLint("ClickableViewAccessibility")
		public ButtonViewHolder(){
			super(getActivity(), R.layout.item_settings_button, list);
			title=findViewById(R.id.title);
			summary=findViewById(R.id.summary);
			icon=findViewById(R.id.icon);
			button=findViewById(R.id.button);
		}

		@Override
		public void onBind(ButtonItem item){
			title.setText(item.title);

			if (item.summary != 0) {
				summary.setText(item.summary);
				summary.setVisibility(View.VISIBLE);
			}
			if (item.icon == 0) {
				icon.setVisibility(View.GONE);
			} else {
				icon.setImageResource(item.icon);
			}
			item.buttonConsumer.accept(button);
		}

		@Override
		public void onClick() {
			button.performClick();
		}
	}

	protected class TextViewHolder extends BindableViewHolder<TextItem> implements UsableRecyclerView.Clickable{
		private final TextView text, secondaryText;
		private final ProgressBar progress;
		private final ImageView icon;

		public TextViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			secondaryText = itemView.findViewById(R.id.secondary_text);
			progress = itemView.findViewById(R.id.progress);
			icon = itemView.findViewById(R.id.icon);
		}

		@Override
		public void onBind(TextItem item){
			icon.setVisibility(item.icon != 0 ? View.VISIBLE : View.GONE);
			secondaryText.setVisibility(item.secondaryText != null ? View.VISIBLE : View.GONE);

			text.setText(item.text);
			progress.animate().alpha(item.loading ? 1 : 0);
			icon.setImageResource(item.icon);
			secondaryText.setText(item.secondaryText);
		}

		@Override
		public void onClick(){
			item.onClick.run();
		}
	}

	private class SmallTextViewHolder extends BindableViewHolder<SmallTextItem> {
		private final TextView text;

		public SmallTextViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			text.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
			text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			text.setPaddingRelative(text.getPaddingStart(), 0, text.getPaddingEnd(), text.getPaddingBottom());
		}

		@Override
		public void onBind(SmallTextItem item){
			text.setText(item.text);
		}
	}

	private class FooterViewHolder extends BindableViewHolder<FooterItem> implements UsableRecyclerView.Clickable{
		private final TextView text;
		public FooterViewHolder(){
			super(getActivity(), R.layout.item_settings_footer, list);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(FooterItem item){
			text.setText(item.text);
		}

		@Override
		public void onClick(){
			item.onClick.run();
		}
	}

	protected class UpdateViewHolder extends BindableViewHolder<UpdateItem>{

		private final TextView text, changelog, changelogHeader;
		private final Button button;
		private final ImageButton cancelBtn;
		private final ProgressBar progress;

		private ObjectAnimator rotationAnimator;
		private Runnable progressUpdater=this::updateProgress;

		public UpdateViewHolder(){
			super(getActivity(), R.layout.item_settings_update, list);
			text=findViewById(R.id.text);
			changelog=findViewById(R.id.changelog);
			changelogHeader=findViewById(R.id.changelog_header);
			button=findViewById(R.id.button);
			cancelBtn=findViewById(R.id.cancel_btn);
			progress=findViewById(R.id.progress);
			button.setOnClickListener(v->{
				GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
				switch(updater.getState()){
					case UPDATE_AVAILABLE -> updater.downloadUpdate();
					case DOWNLOADED -> updater.installUpdate(getActivity());
				}
			});
			cancelBtn.setOnClickListener(v->GithubSelfUpdater.getInstance().cancelDownload());
			rotationAnimator=ObjectAnimator.ofFloat(progress, View.ROTATION, 0f, 360f);
			rotationAnimator.setInterpolator(new LinearInterpolator());
			rotationAnimator.setDuration(1500);
			rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		}

		@Override
		public void onBind(UpdateItem item){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			GithubSelfUpdater.UpdateState state=updater.getState();
			if (state == GithubSelfUpdater.UpdateState.CHECKING) return;
			GithubSelfUpdater.UpdateInfo info=updater.getUpdateInfo();
			if(state!=GithubSelfUpdater.UpdateState.DOWNLOADED){
				text.setText(getString(R.string.mo_update_available, info.version));
				button.setText(getString(R.string.download_update, UiUtils.formatFileSize(getActivity(), info.size, false)));
			}else{
				text.setText(getString(R.string.mo_update_ready, info.version));
				button.setText(R.string.install_update);
			}
			if(state==GithubSelfUpdater.UpdateState.DOWNLOADING){
				rotationAnimator.start();
				button.setVisibility(View.INVISIBLE);
				cancelBtn.setVisibility(View.VISIBLE);
				progress.setVisibility(View.VISIBLE);
				updateProgress();
			}else{
				rotationAnimator.cancel();
				button.setVisibility(View.VISIBLE);
				cancelBtn.setVisibility(View.GONE);
				progress.setVisibility(View.GONE);
				progress.removeCallbacks(progressUpdater);
			}
			changelog.setText(info.changelog);
			if(info.changelog.isEmpty()){
				changelogHeader.setVisibility(View.GONE);
				changelog.setVisibility(View.GONE);
			}
//			changelog.setText(getString(R.string.sk_changelog, info.changelog));
		}

		private void updateProgress(){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			if(updater.getState()!=GithubSelfUpdater.UpdateState.DOWNLOADING)
				return;
			int value=Math.round(progress.getMax()*updater.getDownloadProgress());
			if(Build.VERSION.SDK_INT>=24)
				progress.setProgress(value, true);
			else
				progress.setProgress(value);
			progress.postDelayed(progressUpdater, 1000);
		}
	}
}
