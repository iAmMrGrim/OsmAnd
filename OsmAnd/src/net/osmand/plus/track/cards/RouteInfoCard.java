package net.osmand.plus.track.cards;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;
import static net.osmand.plus.utils.AndroidUtils.getActivityTypeStringPropertyName;
import static net.osmand.plus.utils.AndroidUtils.getStringByProperty;
import static net.osmand.util.Algorithms.capitalizeFirstLetterAndLowercase;

public class RouteInfoCard extends MapBaseCard {

	private static final Map<String, Integer> TRANSLATABLE_KEYS = new HashMap<>();

	static {
		TRANSLATABLE_KEYS.put("name", R.string.shared_string_name);
		TRANSLATABLE_KEYS.put("alt_name", R.string.shared_string_alt_name);
		TRANSLATABLE_KEYS.put("symbol", R.string.shared_string_symbol);
	}


	private final RouteKey routeKey;
	private final GPXFile gpxFile;

	public RouteInfoCard(
			@NonNull MapActivity activity,
			@NonNull RouteKey routeKey,
			@NonNull GPXFile gpxFile
	) {
		super(activity);
		this.routeKey = routeKey;
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_info_card;
	}

	@Override
	public void updateContent() {
		LinearLayout container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		RouteKey routeKey = this.routeKey;
		String routeTypeName = routeKey.type.getTag();

		String routeTypeToDisplay = capitalizeFirstLetterAndLowercase(routeTypeName);
		routeTypeToDisplay = getActivityTypeStringPropertyName(app, routeTypeName, routeTypeToDisplay);
		addInfoRow(container, app.getString(R.string.layer_route), routeTypeToDisplay, false, false);

		for (TagsRow row : getRows()) {

			LinearLayout expandableView = null;
			int tagsCount = row.tags.size();

			for (int tagIndex = 0; tagIndex < tagsCount; tagIndex++) {
				RouteTag tag = row.tags.get(tagIndex);

				ViewGroup tagContainer = tagIndex == 0 ? container : expandableView;
				View view = addInfoRow(tagContainer, tag);

				if (tagIndex == 0 && tagsCount > 1) {
					expandableView = createExpandableView();
					container.addView(expandableView);
					setupViewExpand(view, expandableView);
				}
			}
		}
	}

	@NonNull
	private List<TagsRow> getRows() {
		List<TagsRow> rows = new ArrayList<>();
		Map<String, TagsRow> rowsByKey = new HashMap<>();

		for (String tag : routeKey.tags) {
			String key = routeKey.getKeyFromTag(tag);
			String value = routeKey.getValue(key);

			if (key.equals("name") || key.equals("type") || key.contains("osmc")) {
				continue;
			}

			RouteTag routeTag = new RouteTag(key, value);

			String keyBase = key.split(":")[0];
			if (TRANSLATABLE_KEYS.containsKey(keyBase)) {
				TagsRow row = rowsByKey.get(keyBase);
				if (row == null) {
					row = new TagsRow();
					rowsByKey.put(keyBase, row);
					rows.add(row);
				}
				row.tags.add(routeTag);
			} else {
				TagsRow row = new TagsRow();
				row.tags.add(routeTag);
				rows.add(row);
			}
		}

		for (TagsRow row : rowsByKey.values()) {
			row.sort(app);
		}

		Collections.sort(rows, (o1, o2) -> {
			if (o1.getOrder() != o2.getOrder()) {
				return o1.getOrder() - o2.getOrder();
			}

			String formattedKey1 = o1.tags.get(0).getFormattedKey(app);
			String formattedKey2 = o2.tags.get(0).getFormattedKey(app);
			return formattedKey1.compareTo(formattedKey2);
		});

		return rows;
	}

	@NonNull
	private View addInfoRow(@NonNull ViewGroup container, @NonNull RouteTag tag) {
		String formattedKey = tag.getFormattedKey(app);
		String formattedValue = tag.getFormattedValue(app, routeKey.type);
		boolean linkify = "website".equals(tag.key);

		View view = addInfoRow(container, formattedKey, formattedValue, linkify, true);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if ("wikipedia".equals(tag.key)) {
			if (Algorithms.isUrl(formattedValue) && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
				TextView tvContent = view.findViewById(R.id.title);
				tvContent.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
				view.setOnClickListener(v -> {
					WikiArticleHelper.askShowArticle(activity, nightMode, collectTrackPoints(), formattedValue);
				});
			}
		}

		return view;
	}

	@NonNull
	private View addInfoRow(@NonNull ViewGroup container, @NonNull String key, @NonNull String value, boolean needLinks, boolean showDivider) {
		LayoutInflater inflater = UiUtilities.getInflater(container.getContext(), nightMode);
		View view = inflater.inflate(R.layout.list_item_with_descr, container, false);

		TextView tvLabel = view.findViewById(R.id.description);
		TextView tvContent = view.findViewById(R.id.title);

		tvContent.setText(value);
		tvLabel.setText(key);

		OsmAndAppCustomization customization = app.getAppCustomization();
		if (needLinks && customization.isFeatureEnabled(CONTEXT_MENU_LINKS_ID) && Linkify.addLinks(tvContent, Linkify.ALL)) {
			tvContent.setMovementMethod(null);
			tvContent.setLinkTextColor(ColorUtilities.getActiveColor(app, nightMode));
			tvContent.setOnTouchListener(new ClickableSpanTouchListener());
			AndroidUtils.removeLinkUnderline(tvContent);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), showDivider);
		container.addView(view);
		return view;
	}

	@NonNull
	private LinearLayout createExpandableView() {
		LinearLayout view = new LinearLayout(this.view.getContext());
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(View.GONE);
		LinearLayout.LayoutParams layoutParams = new LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		view.setLayoutParams(layoutParams);
		return view;
	}

	private void setupViewExpand(@NonNull View viewToClick, @NonNull View expandableView) {
		ImageView expandIcon = viewToClick.findViewById(R.id.expand_button);
		expandIcon.setVisibility(View.VISIBLE);
		viewToClick.setOnClickListener(new OnClickListener() {

			private boolean expanded = false;

			@Override
			public void onClick(View v) {
				expanded = !expanded;

				int iconId = expanded ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
				expandIcon.setImageDrawable(getIcon(iconId));

				AndroidUiHelper.updateVisibility(expandableView, expanded);
			}
		});
	}

	private List<LatLon> collectTrackPoints() {
		List<LatLon> points = new ArrayList<>();
		if (gpxFile != null) {
			for (WptPt wptPt : gpxFile.getAllPoints()) {
				points.add(new LatLon(wptPt.lat, wptPt.lon));
			}
		}
		return points;
	}

	private static class RouteTag {

		@NonNull
		private final String key;
		@NonNull
		private final String value;

		@Nullable
		private final PoiType poiType;

		public RouteTag(@NonNull String key, @NonNull String value) {
			this.key = key;
			this.value = value;

			AbstractPoiType abstractPoiType = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key);
			poiType = abstractPoiType instanceof PoiType
					? ((PoiType) abstractPoiType)
					: null;
		}

		@NonNull
		public String getFormattedKey(@NonNull OsmandApplication app) {
			for (Map.Entry<String, Integer> translatableKey : TRANSLATABLE_KEYS.entrySet()) {
				String keyStart = translatableKey.getKey() + ":";
				if (key.startsWith(keyStart)) {
					String nameStr = app.getString(translatableKey.getValue());
					String langId = key.substring(keyStart.length());
					String displayLanguage = new Locale(langId).getDisplayLanguage();
					return app.getString(R.string.ltr_or_rtl_combine_via_colon, nameStr, displayLanguage);
				}
			}

			return poiType != null ? poiType.getTranslation() : capitalizeFirstLetterAndLowercase(key);
		}

		@NonNull
		public String getFormattedValue(@NonNull OsmandApplication app, @NonNull RouteType routeType) {
			switch (key) {
				case "network":
					String network = getStringByProperty(app, "poi_route_" + routeType.getTag() + "_" + value + "_poi");
					return Algorithms.isEmpty(network) ? value : network;
				case "wikipedia":
					return WikiAlgorithms.getWikiUrl(value);
				default:
					return value;
			}
		}

		public int getOrder() {
			return poiType != null ? poiType.getOrder() : PoiType.DEFAULT_ORDER;
		}
	}

	private static class TagsRow {

		private final List<RouteTag> tags = new ArrayList<>();

		public void sort(@NonNull OsmandApplication app) {
			List<String> localeIds = new ArrayList<>();
			boolean hasNativeName = false;
			for (RouteTag tag : tags) {
				String[] keySplit = tag.key.split(":");
				if (keySplit.length == 1) {
					hasNativeName = true;
				} else {
					localeIds.add(keySplit[1]);
				}
			}

			Locale preferredLocale = AmenityMenuBuilder.getPreferredNameLocale(app, localeIds);
			String preferredLocaleId = preferredLocale != null ? preferredLocale.getLanguage() : null;
			boolean finalHasNativeName = hasNativeName;

			Collections.sort(tags, (o1, o2) -> {
				if (preferredLocaleId != null) {
					if (o1.key.contains(preferredLocaleId)) {
						return -1;
					} else if (o2.key.contains(preferredLocaleId)) {
						return 1;
					}
				} else if (finalHasNativeName) {
					if (!o1.key.contains(":")) {
						return -1;
					} else if (!o2.key.contains(":")) {
						return 1;
					}
				}

				return o1.getFormattedKey(app).compareTo(o2.getFormattedKey(app));
			});
		}

		public int getOrder() {
			return tags.get(0).getOrder();
		}
	}
}
