/*
 * Copyright (C) 2017  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.doubleyellow.scoreboard.feed;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.doubleyellow.android.task.DownloadImageTask;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.URLFeedTask;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.DateUtil;
import com.doubleyellow.util.JsonUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ShowTypesAdapter extends BaseAdapter implements ContentReceiver
{
    private static final String TAG = "SB." + ShowTypesAdapter.class.getSimpleName();

    private static LayoutInflater inflater = null;

    //private ContactClickListener contactClickListener = null;

    private Context                  context           = null;
    private FeedFeedSelector         feedFeedSelector  = null;
    private View.OnLongClickListener longClickListener = null;
    private View.OnClickListener     clickListener     = null;

    ShowTypesAdapter(FeedFeedSelector a, View.OnClickListener clickListener, View.OnLongClickListener longClickListener)
    {
        inflater = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context           = a;
        this.feedFeedSelector  = a;
        this.clickListener     = clickListener;
        this.longClickListener = longClickListener;
        load(true);
    }

    @Override public int getCount() {
        return lKeys.size();
    }
    @Override public Object getItem(int i) {
        /*Log.d(TAG, "getItem("   + i + ")");*/ return i;
    }
    @Override public long getItemId(int i) {
        /*Log.d(TAG, "getItemId(" + i + ")");*/ return i;
    }

    /**
     * Every time ListView needs to show a new row on screen, it will img the getView() method from its adapter.
     * getView() takes three arguments arguments: the row position, a convertView, and the parent ViewGroup.
     * The convertView argument is essentially a “ScrapView” as described earlier.
     * It will have a non-null value when ListView is asking you recycle the row layout.
     * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
     */
    @Override public View getView(int i, View view, ViewGroup viewGroupParent) {
        String sType = lKeys.get(i);  // eg. 'tournamentsoftware.com' and 'Manual'
      //Log.d(TAG, "getView(" + i + ") " + view + " " + sType);
        ViewHolder viewHolder = null;

        boolean bReUseView = false;
        if( view!=null && bReUseView ) {
            viewHolder = (ViewHolder) view.getTag();
          //Log.d(TAG, "re-using view from " + viewHolder.img.getTag() + " for " + sType);
        } else {
            view = inflater.inflate(R.layout.image_item, null /*viewGroupParent*/);
            // if using viewgroupparent i ran into
/*
            java.lang.UnsupportedOperationException: addView(View, LayoutParams) is not supported in AdapterView
            at android.widget.AdapterView.addView(AdapterView.java:487)
*/

/*
            if ( view instanceof RelativeLayout ) {
                RelativeLayout rl = (RelativeLayout) view;
                int minHeight = ViewUtil.getScreenHeightWidthMaximum(context) / 10;
                rl.setMinimumHeight(minHeight);
            }
*/

            // create viewholder for freshly inflated view
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView)    view.findViewById(R.id.image_item_text);
            viewHolder.img  = (ImageButton) view.findViewById(R.id.image_item_image);

            if ( bReUseView ) {
                view.setTag(viewHolder);
            } else {
                view.setTag(sType);
            }
          //Log.d(TAG, "create view for " + sType);
        }

        JSONObject joMeta = joMetaData.optJSONObject(sType);
        if ( joMeta == null ) {
            return view; // should not occur, if it does App will crash if null is returned (with unclear stacktrace!)
        }
        String sShortDescription = joMeta.optString(FeedKeys.ShortDescription.toString());
        String lang         = RWValues.getDeviceLanguage(context);
        String sDisplayName = joMeta.optString(FeedKeys.DisplayName.toString()  , sType);
               sDisplayName = joMeta.optString(FeedKeys.DisplayName.getLangSuffixed(lang), sDisplayName);

        viewHolder.text.setText(sDisplayName);
        //viewHolder.text.setClickable( iAdminLevel >= 2 );

        String sBitMap   = joMeta.optString(FeedKeys.Image   .toString());
               sBitMap   = joMeta.optString(FeedKeys.Image   .getLangSuffixed(lang), sBitMap);
               sBitMap   = joMeta.optString(FeedKeys.Image   .getBrandSuffixed()   , sBitMap);
        String sImageURL = joMeta.optString(FeedKeys.ImageURL.toString());
               sImageURL = joMeta.optString(FeedKeys.ImageURL.getLangSuffixed(lang), sImageURL);
               sImageURL = joMeta.optString(FeedKeys.ImageURL.getBrandSuffixed()   , sImageURL);
        boolean bImageFromURL = false;
        if ( StringUtil.isNotEmpty(sBitMap) ) {
            viewHolder.img.setVisibility(View.VISIBLE);
            if ( sBitMap.startsWith("R.drawable.") ) {
                String sName = sBitMap.replace("R.drawable.", "");
                int drawableId = context.getResources().getIdentifier(sName, "drawable", context.getPackageName());
                if ( drawableId != 0 ) {
                    Drawable drawable = context.getResources().getDrawable(drawableId);
                    viewHolder.img.setImageDrawable(drawable);
                }
            } else if (StringUtil.isNotEmpty(sImageURL) || (sBitMap != null && sBitMap.startsWith("http"))) {
                if ( StringUtil.isEmpty(sImageURL) ) {
                    sImageURL = sBitMap;
                }
              //DownloadImageTask downloadImageTask = new DownloadImageTask(viewHolder.img, sBitMap, sType);
                File fCache = new File(context.getCacheDir(), sType + sImageURL.replaceAll(".*(\\.\\w+)$", "$1"));
                DownloadImageTask downloadImageTask = new DownloadImageTask(context.getResources(), viewHolder.img, sImageURL, sType, ImageView.ScaleType.FIT_CENTER, fCache, 3000);
                downloadImageTask.execute();
                bImageFromURL = true;
            } else {
                Bitmap bitMapFromBase64 = ViewUtil.getBitMapFromBase64(sBitMap);
                viewHolder.img.setImageBitmap(bitMapFromBase64);
            }
        } else {
            viewHolder.img.setVisibility(View.INVISIBLE);
        }

        String sBGColor = joMeta.optString(FeedKeys.BGColor.toString()        , "#FFFFFF");
               sBGColor = joMeta.optString(FeedKeys.BGColor.getBrandSuffixed(), sBGColor);
        int    iBGColor = Color.parseColor(sBGColor);
        viewHolder.img.setBackgroundColor(iBGColor);
        viewHolder.text.setBackgroundColor(iBGColor);
        view.setBackgroundColor(iBGColor);

        String sTextColor = joMeta.optString(FeedKeys.TextColor.toString()        , "#000000");
               sTextColor = joMeta.optString(FeedKeys.TextColor.getBrandSuffixed(), sTextColor);
        int    iTxtColor  = Color.parseColor(sTextColor);
        viewHolder.text.setTextColor(iTxtColor);

        viewHolder.text.setOnLongClickListener(longClickListener);
        viewHolder.img.setOnLongClickListener(longClickListener);
        //view.setOnLongClickListener(longClickListener); // tag of this view is incorrect... the viewholder

        viewHolder.text.setOnClickListener(clickListener);
        viewHolder.img.setOnClickListener(clickListener);
        view.setOnClickListener(clickListener);

        viewHolder.text.setTag(sType);
        if ( bImageFromURL == false ) {
            viewHolder.img.setTag(sType);
        }

        return view;
    }

    /**
     * The View Holder pattern is about reducing the number of findViewById() calls in the adapter’s getView().
     * In practice, the View Holder is a lightweight inner class that holds direct references to all inner views from a row.
     * You store it as a tag in the row’s view after inflating it.
     * This way you’ll only have to use findViewById() when you first create the layout.
     */
    private static class ViewHolder {
        TextView    text; // can also be subclass Button
        ImageButton img;
    }

    public void load(boolean bUseCacheIfPresent) {
        String sURL = PreferenceValues.getFeedsFeedURL(context);
        sURL = URLFeedTask.prefixWithBaseIfRequired(sURL);

        if ( StringUtil.isEmpty(sURL) ) {
            this.receive(null, FetchResult.UnexpectedContent, 0, null);
            return;
        }

        URLTask task = new URLFeedTask(context, sURL);
        if ( bUseCacheIfPresent == false ) {
            task.setCacheFileToOld(true);
        }

        task.setContentReceiver(this);
        task.execute();
    }

    @Override public void receive(String sContent, FetchResult result, long lCacheAge, String sLastSuccessfulContent)
    {
        Log.i(TAG, String.format("Fetched (cache age %d, new size %d cached size %d)", lCacheAge, StringUtil.size(sContent), StringUtil.size(sLastSuccessfulContent)));

        // use try/catch here because getResources() may fail if user closed the activity before data was retrieved
        try {
            String sJsonContent = null;
            if ( (sContent == null) || (result.equals(FetchResult.OK) == false)) {
                String sUrl = PreferenceValues.getFeedsFeedURL(context);
                if ( sLastSuccessfulContent != null ) {

                    String sHeader = context.getResources().getString(R.string.Could_not_read_feed_x__y__Using_cached_content_aged_z_minutes, sUrl, result, DateUtil.convertToMinutes(lCacheAge));
                    Toast.makeText(context, sHeader, Toast.LENGTH_LONG).show();

                    sJsonContent = sLastSuccessfulContent;
                } else {
                    // invalid feed of feeds url?
                    String sHeader = context.getResources().getString(R.string.could_not_load_feed_x, StringUtil.capitalize(result));
                    //super.addItem(sHeader, sUrl);
                    if ( ScoreBoard.isInSpecialMode() ) {
                        ContentUtil.placeOnClipboard(context, "Squore feeds", sUrl);
                    }

                    if ( FetchResult.UnexpectedContent.equals(result) ) {
                        //super.addItem(sHeader, context.getString(R.string.possible_cause) + context.getString(R.string.no_fully_functional_connection_error));
                    }
                }
            } else {
                sJsonContent = sContent;
            }
            if ( StringUtil.isNotEmpty(sJsonContent) ) {
                fillWithRootEntries(sJsonContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyDataSetChanged();

        feedFeedSelector.changeStatus(Status.SelectType);
    }

    JSONObject joRoot     = null;
    JSONObject joMetaData = null;
    private List<String> lKeys = new ArrayList<String>();
    private void fillWithRootEntries(String sContent) {
        lKeys.clear();

        if ( sContent != null && joRoot == null ) {
            try {
                joRoot = new JSONObject(sContent);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }

        // if there is a config section, use it
        joMetaData = joRoot.optJSONObject(FeedKeys.FeedMetaData.toString());
        JSONArray types = joMetaData.optJSONArray(FeedKeys.Sequence.toString());
        if ( JsonUtil.isEmpty(types) ) {
            types = joRoot.names();
        } else {
            // allow to show just different set for branded version
            final String brandKey = FeedKeys.Sequence.getBrandSuffixed();
            if ( joMetaData.has(brandKey) ) {
                types = joMetaData.optJSONArray(brandKey);
            }
        }

        // if defined for users locale, add certain types to top
        List<String> lLocaleFeedTypes =  new ArrayList<>();
        {
            List<Locale> lLocales = new ArrayList<>(); // most important FIRST
            {
                Resources res    = context.getResources();
                Configuration resCfg = res.getConfiguration();
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N /* 24 */ ) {
                    LocaleList locales = resCfg.getLocales();
                    if ( locales.isEmpty() == false ) {
                        for(int i=0; i<locales.size();i++) {
                            lLocales.add(locales.get(i));
                        }
                    }
                } else {
                    Locale deviceLocale = RWValues.getDeviceLocale(context);
                    lLocales.add(deviceLocale);
                }
            }
            if ( ListUtil.isNotEmpty(lLocales) ) {
                for ( Locale locale : lLocales ) {
                    String[] saKeys = new String[] { FeedKeys.Sequence.getLocalSuffixed(locale), FeedKeys.Sequence.getLangSuffixed(locale.getLanguage()), FeedKeys.Sequence.getLangSuffixed(locale.getCountry()) };
                    for( String sKey :saKeys ) {
                        JSONArray typesLocale = joMetaData.optJSONArray(sKey);
                        if ( JsonUtil.isNotEmpty(typesLocale) ) {
                            List<String> lTmp = JsonUtil.asListOfStrings(typesLocale);
                            lTmp.removeAll(lLocaleFeedTypes);
                            lLocaleFeedTypes.addAll(lTmp);
                        }
                    }
                }
            }
        }

        try {
            for ( int i=0; i < JsonUtil.size(types); i++ ) {
                String sType = types.getString(i);
                if ( sType.equals(FeedKeys.FeedMetaData.toString())) { continue; }
                lKeys.add(sType);
            }
            if ( ListUtil.isEmpty(lKeys) ) {
                String sUrl = PreferenceValues.getFeedsFeedURL(context);
                //TODO: show some message about the url that does not work
            }
            // if, based on device locale, some feeds are more useful to the user, add them to the top
            if ( ListUtil.isNotEmpty(lLocaleFeedTypes) ) {
                lLocaleFeedTypes.retainAll(lKeys);
                lKeys.removeAll(lLocaleFeedTypes);
                lKeys.addAll(0, lLocaleFeedTypes);
            }
            // for earlier selected 'Types', place them on top of the list
            List<String> lMyFeedTypes =  PreferenceValues.getUsedFeedTypes(context);
            if ( ListUtil.isNotEmpty(lMyFeedTypes) ) {
                lMyFeedTypes.retainAll(lKeys);
                lKeys.removeAll(lMyFeedTypes);
                lKeys.addAll(0, lMyFeedTypes);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
