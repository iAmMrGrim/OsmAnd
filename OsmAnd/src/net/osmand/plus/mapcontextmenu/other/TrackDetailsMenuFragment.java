package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;

public class TrackDetailsMenuFragment extends BaseOsmAndFragment {
	public static final String TAG = "TrackDetailsMenuFragment";

	private TrackDetailsMenu menu;
	private View mainView;
	private boolean paused = true;
	private boolean nightMode;

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@NonNull
	private MapActivity requireMapActivity() {
		return (MapActivity) requireMyActivity();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		MapActivity mapActivity = requireMapActivity();
		menu = mapActivity.getTrackDetailsMenu();
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		ContextThemeWrapper context =
				new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		View view = LayoutInflater.from(context).inflate(R.layout.track_details, container, false);
		if (!AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		}
		if (menu == null || menu.getGpxItem() == null) {
			return view;
		}

		mainView = view.findViewById(R.id.main_view);

		TextView topBarTitle = (TextView) mainView.findViewById(R.id.top_bar_title);
		if (topBarTitle != null) {
			if (menu.getGpxItem().group != null) {
				topBarTitle.setText(menu.getGpxItem().group.getGpxName());
			} else {
				topBarTitle.setText(R.string.rendering_category_details);
			}
		}

		ImageButton backButton = (ImageButton) mainView.findViewById(R.id.top_bar_back_button);
		ImageButton closeButton = (ImageButton) mainView.findViewById(R.id.top_bar_close_button);
		if (backButton != null) {
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						activity.onBackPressed();
					}
				}
			});
		}
		if (closeButton != null) {
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.hide(false);
				}
			});
		}

		MapContextMenu contextMenu = mapActivity.getContextMenu();
		final boolean forceFitTrackOnMap;
		if (contextMenu.isActive()) {
			forceFitTrackOnMap = !(contextMenu.getPointDescription() != null && contextMenu.getPointDescription().isGpxPoint());
		} else {
			forceFitTrackOnMap = true;
		}
		updateInfo(forceFitTrackOnMap);

		ViewTreeObserver vto = mainView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
				if (getMapActivity() != null) {
					updateInfo(forceFitTrackOnMap);
				}
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu == null || menu.getGpxItem() == null) {
			dismiss(false);
		} else {
			menu.onShow();
		}
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (menu != null) {
			menu.onDismiss();
		}
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_gradient;
	}

	public boolean isPaused() {
		return paused;
	}

	public int getHeight() {
		if (mainView != null) {
			return mainView.getHeight();
		} else {
			return 0;
		}
	}

	public int getWidth() {
		if (mainView != null) {
			return mainView.getWidth();
		} else {
			return 0;
		}
	}

	public void updateInfo() {
		updateInfo(true);
	}

	public void updateInfo(boolean forceFitTrackOnMap) {
		menu.updateInfo(mainView, forceFitTrackOnMap);
		applyDayNightMode();
	}

	public void show(MapActivity mapActivity) {
		mapActivity.getSupportFragmentManager().beginTransaction()
				.add(R.id.routeMenuContainer, this, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
	}

	public void dismiss(boolean backPressed) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
				mapActivity.getMapRouteInfoMenu().onDismiss(this, 0, null, backPressed);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	public void applyDayNightMode() {
		MapActivity ctx = getMapActivity();
		if (ctx != null) {
			boolean portraitMode = AndroidUiHelper.isOrientationPortrait(ctx);
			boolean landscapeLayout = !portraitMode;
			if (!landscapeLayout) {
				AndroidUtils.setBackground(ctx, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
			} else {
				final TypedValue typedValueAttr = new TypedValue();
				int bgAttrId = AndroidUtils.isLayoutRtl(ctx) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
				ctx.getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
				mainView.setBackgroundResource(typedValueAttr.resourceId);
			}

			AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.y_axis_title), nightMode);
			AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.x_axis_title), nightMode);

			ImageView yAxisArrow = (ImageView) mainView.findViewById(R.id.y_axis_arrow);
			ImageView xAxisArrow = (ImageView) mainView.findViewById(R.id.x_axis_arrow);
			yAxisArrow.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_drop_down));
			xAxisArrow.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_drop_down));

			ImageButton backButton = (ImageButton) mainView.findViewById(R.id.top_bar_back_button);
			if (backButton != null) {
				Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(ctx), R.color.color_white);
				backButton.setImageDrawable(icBack);
			}
		}
	}

	public static boolean showInstance(final MapActivity mapActivity) {
		try {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			TrackDetailsMenuFragment fragment = new TrackDetailsMenuFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(portrait ? R.id.bottomFragmentContainer : R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}
}
