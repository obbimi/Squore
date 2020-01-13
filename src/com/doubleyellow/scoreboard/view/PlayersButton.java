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

package com.doubleyellow.scoreboard.view;

import android.content.Context;
import android.graphics.*;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.doubleyellow.android.view.AutoResizeTextView;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.android.util.ColorUtil;
import com.doubleyellow.demo.DrawTouch;
import com.doubleyellow.scoreboard.feed.FeedMatchSelector;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowCountryAs;
import com.doubleyellow.util.Direction;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.view.SBRelativeLayout;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.DoublesServe;
import com.doubleyellow.scoreboard.model.ServeSide;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Class to display player name (or player names for doubles)
 * For doubles this view includes the serve (side) button.
 * For tabletennis it is also used to indicate who should be receiving.
 *
 * If countries are specified this class can also display flags.
 * If avatars are specified this class can also display an avatar.
 */
public class PlayersButton extends SBRelativeLayout implements DrawTouch
{
    private static final String TAG = "SB." + PlayersButton.class.getSimpleName();

    public PlayersButton(Context context) {
        this(context, null);
    }

    public PlayersButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayersButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if ( isInEditMode() ) {
            setPlayers("Player A", false);
            setTextColor(Color.WHITE);
            setCountry("BEL", true, true);
            setClub("DY");
        }
    }

    private final boolean m_bReuseFlagForAvatar = true;
    private float   m_fAspectRatio_Flag   = 1.6f;
    private float   m_fAspectRatio_Avatar = 1.0f;

    public static final String SUBBUTTON = "SUBBUTTON";
    private List<TextView>       nameButtons  = new ArrayList<TextView>();
    private List<TextView>       serveButtons = new ArrayList<TextView>();
    private List<ImageView>      flagImages   = new ArrayList<ImageView>(); // for now only just the one (even for doubles)
    private List<ImageView>      avatarImages = new ArrayList<ImageView>(); // for now only just the one (even for doubles)
    private boolean              m_bIsDoubles = false;
    public void setPlayers(String players, boolean bIsDoubles) {
        if ( m_bIsDoubles != bIsDoubles ) {
            nameButtons  = new ArrayList<>();
            serveButtons = new ArrayList<>();
            flagImages   = new ArrayList<>();
            avatarImages = new ArrayList<>();
            super.removeAllViews();
            if ( bIsDoubles ) {
                // from singles to doubles
                fReduceFactor = 0.75f;  // TODO: customizable
                this.fTxtSize = this.fTxtSize * fReduceFactor;
            } else {
                // from doubles to singles
                this.fTxtSize = this.fTxtSize / fReduceFactor;
                fReduceFactor = 1.0f;
            }
            m_bIsDoubles = bIsDoubles;
        }

        String[] saPlayers = new String[] {players};
        if ( bIsDoubles ) {
            saPlayers = StringUtil.singleCharacterSplit("/" + players + "/"); // TODO: improve
        }
        //if ( fReduceFactor == 1.0f && bIsDoubles) {
        //    fReduceFactor = 0.75f; // TODO: customizable
             //this.fTxtSize = this.fTxtSize * fReduceFactor;
        //}

        for ( int n=0; n < saPlayers.length; n++ ) {
            // portrait and first in landscape: flag, avatar, name, serve
            int avatarLRofFlag             = PercentRelativeLayout.RIGHT_OF;
            int flagImageAlignParentRL     = PercentRelativeLayout.ALIGN_PARENT_LEFT;
            int playerNameLRofAvatar       = PercentRelativeLayout.RIGHT_OF;
            int playerNameLRofServe        = PercentRelativeLayout.LEFT_OF;
            int sideButtonAlignParentRL    = PercentRelativeLayout.ALIGN_PARENT_RIGHT;
            int playerNameAlignParentRL    = PercentRelativeLayout.ALIGN_PARENT_RIGHT;

            int playerNameAlignLRofPartner = PercentRelativeLayout.ALIGN_LEFT;
            if ( this.getId() == R.id.txt_player2 ) {
                if ( ViewUtil.isLandscapeOrientation(getContext()) ) {
                    avatarLRofFlag             = PercentRelativeLayout.LEFT_OF;
                    flagImageAlignParentRL     = PercentRelativeLayout.ALIGN_PARENT_RIGHT;
                    playerNameLRofAvatar       = PercentRelativeLayout.LEFT_OF;
                    playerNameLRofServe        = PercentRelativeLayout.RIGHT_OF;
                    sideButtonAlignParentRL    = PercentRelativeLayout.ALIGN_PARENT_LEFT;
                    playerNameAlignParentRL    = PercentRelativeLayout.ALIGN_PARENT_LEFT;

                    playerNameAlignLRofPartner = PercentRelativeLayout.ALIGN_RIGHT;
                }
            }

            final int iNameButtonId      = 100 + n/* + this.getId()*/;
            final int iServeSideButtonID = 200 + n/* + this.getId()*/;
            final int iFlagImageID       = 300 + n/* + this.getId()*/;
            final int iAvatarImageID     = 400 + n/* + this.getId()*/;
            if ( bIsDoubles ) {
                if ( serveButtons.size() <= n ) {
                    // serve side button (1 for singles, 2 for doubles)
                    PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null);
                    rlp.addRule(sideButtonAlignParentRL);
                    if ( serveButtons.size() == 1 ) {
                        // for doubles, add the second serve side button below the first
                        rlp.addRule(PercentRelativeLayout.BELOW, iServeSideButtonID - 1);
                    }

                    PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                    info.heightPercent = bIsDoubles?0.50f:1.0f;
                  //info.widthPercent  = -1.0f;
                    info.aspectRatio   =  1.5f;

                    TextView bServeSide = new AutoResizeTextView(new ContextThemeWrapper(getContext(), R.style.SBButton) /*, null, R.style.SBButton*/);
                    bServeSide.setId(iServeSideButtonID);
                    bServeSide.setTag(((this.getId() == R.id.txt_player2) ? R.id.btn_side2 : R.id.btn_side1) + SUBBUTTON + n); // used to calculate parent id from within ScoreBoard listeners
                    if ( m_bIsDoubles == false ) {
                        bServeSide.setTypeface(null, Typeface.BOLD);
                    }
                    bServeSide.setText(" ");
                    super.addView(bServeSide, rlp);
                    serveButtons.add(bServeSide);

                    bServeSide.setTextColor(this.textColorServer);
                    ColorUtil.setBackground(bServeSide, this.bgColorServer);

                    if ( serveSideButtonListener instanceof OnClickListener ) {
                        bServeSide.setOnClickListener((OnClickListener) serveSideButtonListener);
                    }
                    if ( serveSideButtonListener instanceof OnLongClickListener ) {
                        bServeSide.setOnLongClickListener((OnLongClickListener) serveSideButtonListener);
                    }
                    if ( serveSideButtonListener instanceof OnTouchListener ) {
                        bServeSide.setOnTouchListener(serveSideButtonListener);
                    }
                } else {
                    TextView bServeSide = serveButtons.get(n);
                    bServeSide.setVisibility(VISIBLE);
                }
            }

            if ( flagImages.size() <= 0 ) { // for now only add a single flag and avatar imageview
                // flag image
                {
                    PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null);
                    rlp.addRule(flagImageAlignParentRL);

                    PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                    info.heightPercent =  1.0f;
                  //info.widthPercent  = -1.0f; // the default
                    info.aspectRatio   =  m_fAspectRatio_Flag; // TODO: dynamic dependant of where we retrieve images from

                    ImageView ivFlag = new ImageView(getContext());
                  //ivFlag.setVisibility(GONE);
                    ivFlag.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // FIT_XY
                    //ivFlag.setImageResource(R.drawable.logo);
                    ivFlag.setId(iFlagImageID);
                    ivFlag.setBackgroundResource(R.drawable.image_border);
                    super.addView(ivFlag, rlp);
                    flagImages.add(ivFlag);

                    if (onTouchListenerNames != null) {
                        ivFlag.setOnTouchListener(onTouchListenerNames);
                    }
                    //ivFlag.setOnTouchListener(onTouchGONE);

                    if ( m_bReuseFlagForAvatar ) {
                        avatarImages.add(ivFlag);
                    }
                }

                if ( m_bReuseFlagForAvatar == false) {
                    PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null);
                    rlp.addRule(avatarLRofFlag, iFlagImageID);

                    PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                    info.heightPercent =  1.0f;
                  //info.widthPercent  = -1.0f; // the default
                    info.aspectRatio   =  m_fAspectRatio_Avatar;

                    ImageView ivAvatar = new ImageView(getContext());
                  //ivAvatar.setVisibility(GONE);
                    ivAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // FIT_START
                    ivAvatar.setId(iAvatarImageID);
                    //ivAvatar.setBackgroundResource(R.drawable.image_border);
                    super.addView(ivAvatar, rlp);
                    avatarImages.add(ivAvatar);

                    if (onTouchListenerNames != null) {
                        ivAvatar.setOnTouchListener(onTouchListenerNames);
                    }
                    //ivAvatar.setOnTouchListener(onTouchGONE);
                }
            }

            TextView b = null;
            if ( nameButtons.size() <= n ) {
                // player buttons
                PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
              //PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null); // NOK
                if ( m_bIsDoubles ) {
                    rlp.addRule(playerNameLRofServe, iServeSideButtonID);
                } else {
                    rlp.addRule(playerNameAlignParentRL);
                }
                if ( nameButtons.size() == 1 ) {
                    // for double, add the second name below the first
                    rlp.addRule(PercentRelativeLayout.BELOW, iNameButtonId - 1);
                    rlp.addRule(playerNameAlignLRofPartner, iNameButtonId - 1);
                } else {
                    if ( m_bReuseFlagForAvatar ) {
                        rlp.addRule(playerNameLRofAvatar, iFlagImageID);
                    } else {
                        rlp.addRule(playerNameLRofAvatar, iAvatarImageID);
                    }
                }

                PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                info.heightPercent = bIsDoubles?0.50f:1.0f;

                b = new AutoResizeTextView(new ContextThemeWrapper(getContext(), R.style.SBButton) /*, null, R.style.SBButton*/);
                b.setId(iNameButtonId);
                b.setTag(this.getId() + SUBBUTTON + n); // used to calculate parent id from within ScoreBoard listeners
                b.setTypeface(null, Typeface.BOLD); // TODO: not four doubles?
                //b.setSingleLine();
                b.setMaxLines(1);
                super.addView(b, rlp);
                nameButtons.add(b);

                if (onTouchListenerNames != null) {
                    b.setOnTouchListener(onTouchListenerNames);
                }
            } else {
                b = nameButtons.get(n);
                b.setVisibility(VISIBLE);
            }
            b.setText(saPlayers[n]);
        }

        if ( bIsDoubles == false ) {
            if ( nameButtons.size() == 2 ) {
                // ensure, if previous match was a double, than second player name TextView is now invisible
                nameButtons.get(1).setVisibility(GONE);
            }

            if ( ListUtil.size(serveButtons) > 0 ) {
                // no serve buttons for singles (TODO: via preferences)
                serveButtons.get(0).setVisibility(GONE);
                if ( serveButtons.size() == 2 ) {
                    serveButtons.get(1).setVisibility(GONE);
                }
            }
        }

        if ( DoublesServe.NA.equals(m_doublesServe) == false) {
            setServer(m_doublesServe, m_serveSide, m_isHandout, m_sServerDisplayValueOverwrite);
        }
        if ( DoublesServe.NA.equals(m_doublesReceiver) == false) {
            setReceiver(m_doublesReceiver);
        }
        //this.setOnTouchListener(new TouchBothListener(clickBothListener));
    }

    private Set<ShowCountryAs> lHasCountry = EnumSet.noneOf(ShowCountryAs.class);
    private boolean bHasClub    = false;

    public void setCountry(String sCountryCode, boolean bShowAsText, boolean bShowFlag) {
        for ( TextView tv : nameButtons) {
            String sOld = tv.getText().toString();
                   sOld = sOld.replaceAll(FeedMatchSelector.sCountry, "").trim();
            if ( StringUtil.isEmpty(sCountryCode) ) {
                this.lHasCountry.clear();
                flagImages.get(0).setVisibility(GONE); // TODO: if only avatar is specified (no country) layout is screwed up when using GONE for player2 in landscape
                if ( this.bHasClub == false ) {
                    tv.setText(sOld);
                }
            } else {
                if ( bShowAsText ) {
                    if ( this.bHasClub == false ) {
                        this.lHasCountry.add(ShowCountryAs.AbbreviationAfterName);
                        tv.setText(String.format("%s [%s]", sOld, sCountryCode));
                    }
                } else {
                    if ( this.bHasClub == false ) {
                        tv.setText(sOld);
                    }
                }
            }
        }
        if ( ListUtil.size(flagImages) > 0 ) {
            ImageView imageView = flagImages.get(0);
            if ( bShowFlag ) {
                if ( StringUtil.isNotEmpty(sCountryCode) ) {
                    updateAspectRatio(imageView, m_fAspectRatio_Flag);
                }
                this.lHasCountry.add(ShowCountryAs.FlagNextToNameOnDevice);
                PreferenceValues.downloadImage(getContext(), imageView, sCountryCode);
            } else {
                imageView.setVisibility(GONE);
            }
        }
    }

    public void setAvatar(String sAvatar) {
        if ( ListUtil.size(avatarImages) > 0 ) {
            ImageView imageView = avatarImages.get(0);
            if ( StringUtil.isEmpty(sAvatar) ) {
                imageView.setVisibility(GONE);
            } else {
                updateAspectRatio(imageView, m_fAspectRatio_Avatar);
                imageView.setVisibility(VISIBLE);
                PreferenceValues.downloadAvatar(getContext(), imageView, sAvatar);
            }
        }
    }
    /** assume avatar is always set first */
    private void updateAspectRatio(ImageView imageView, float fNew) {
        if ( m_bReuseFlagForAvatar == false ) { return; }

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        if ( layoutParams instanceof PercentRelativeLayout.LayoutParams == false ) { return; }

        PercentRelativeLayout.LayoutParams plParams = (PercentRelativeLayout.LayoutParams) layoutParams;
        PercentLayoutHelper.PercentLayoutInfo info = plParams.getPercentLayoutInfo();
        if ( info.aspectRatio != fNew ) {
            info.aspectRatio = fNew;

/*
            // attempts to force a redraw... all seem to have no effect... after screen rotation everything is layed-out OK, though

            // attempt 1
            imageView.setVisibility(GONE);// if actual image is successfully downloaded (or taken from cache) visibility will be set back to View.VISIBLE

            // attempt 2
            if ( imageView.isInLayout() == false ) { // API 18 and higher
                imageView.requestLayout();
                imageView.getParent().requestLayout();
                requestLayout();
                imageView.setLayoutParams(plParams);
            }

            // attempt 3.a
            imageView.invalidateOutline(); // API 21 and higher

            // attempt 3.b
            invalidateOutline();

            // attempt 4.a
            imageView.invalidate();

            // attempt 5.b
            imageView.postInvalidate();

*/
        }
    }

    /** Should be invoked after setCountry */
    public void setClub(final String sClubFull) {
        if ( StringUtil.isEmpty(sClubFull) ) {
            bHasClub = false;
            if ( lHasCountry.isEmpty() == false ) {
                // do not clear anything : not the between brackets, not the flag
                return;
            }
        }
        // if the club name is not an abbreviation and does not end in an abbreviation, 'calculate' an abbreviation
        String sClub = sClubFull;
        if ( StringUtil.size(sClub) > 4 ) {
            if ( sClub.toLowerCase().startsWith("squash") && sClub.trim().length() > 6) {
                sClub = sClub.replaceFirst("(quash|QUASH)", "");
            }
            sClub = sClub.replaceAll("\\.", " "); // abbreviations with dot, make them into one letter words. Logic further on will only take the letters
            String sAbbr = sClub;

            String sRegExp = ".*[\\[\\(]([A-Za-z0-9_-]{2,4})[\\]\\)]\\s*$";
            if ( sClub.matches(sRegExp) ) {
                // abbreviation between brackets at the end
                sAbbr = sClub.replaceAll(sRegExp, "$1");
            } else if ( sClub.matches(".*\\-\\s*[A-Za-z0-9_]{2,4}$") ) {
                // abbreviation after a dash at the end
                sAbbr = sClub.substring(sClub.lastIndexOf("-") + 1).trim();
            } else {
                // guess an abbreviation: first attempt see words connect with a dash as one word
                String sTst = sClub.replaceAll("\\b(\\w)[A-Za-z\\-]*\\b", "$1").replaceAll("[\\s\"']", "");
                if ( sTst.length() <= 1 ) {
                    // second attempt: see words separated by a dash as separate words
                    sTst = sClub.replaceAll("-", " ") .replaceAll("\\b(\\w)[A-Za-z]*\\b", "$1").replaceAll("[\\s\"']", "");
                }
                if ( sTst.length() > 1 ) {
                    sAbbr = sTst;
                }
            }
            sClub = sAbbr.substring(0,Math.min(4, sAbbr.length()));
        }

        this.bHasClub = StringUtil.isNotEmpty(sClubFull);
        if ( bHasClub ) {
/*
        // assume club is more important than country: hide flag
        if ( ListUtil.isNotEmpty(flagImages) ) {
            this.lHasCountry.remove(ShowCountryAs.FlagNextToNameOnDevice);
            flagImages.get(0).setVisibility(GONE);
        }
*/
        }
        for ( TextView tv : nameButtons) {
            String sOld = tv.getText().toString();
                   sOld = sOld.replaceAll("[\\[\\(](.*)[\\]\\)]\\s*$", "").trim(); // remove either country or previous club
            lHasCountry.remove(ShowCountryAs.AbbreviationAfterName);
            if ( this.bHasClub ) {
                if ( sOld.equalsIgnoreCase(sClubFull) ) {
                    // player name is the same as the club. No need to add the abbreviation
                } else {
                    tv.setText(String.format("%s [%s]", sOld, sClub));
                }
            } else {
                tv.setText(sOld);
            }
        }
    }

    private OnTouchListener serveSideButtonListener = null;
    public void setServeSideButtonListener(OnTouchListener listener) {
        this.serveSideButtonListener = listener;
        for(TextView b: serveButtons) {
            b.setOnTouchListener(serveSideButtonListener);
        }
    }

    /** Test listener */
/*
    private OnTouchListener onTouchGONE = new OnTouchListener() {
        @Override public boolean onTouch(View v, MotionEvent event) {
            v.setVisibility(GONE);
            return true;
        }
    };
*/
    private OnTouchListener onTouchListenerNames = null;
/*
    private OnTouchListener onTouchListenerSides = null;
    public void setOnTouchListeners(OnTouchListener lNames, OnTouchListener lServeSides) {
        this.onTouchListenerNames = lNames;
        this.onTouchListenerSides = lServeSides;
        for(TextView b: nameButtons) {
            b.setOnTouchListener(this.onTouchListenerNames);
        }
        for(TextView b: serveButtons) {
            b.setOnTouchListener(this.onTouchListenerSides);
        }
    }
*/
    @Override public void setOnTouchListener(OnTouchListener listener) {
        onTouchListenerNames = listener;
        for(TextView b: nameButtons) {
            b.setOnTouchListener(onTouchListenerNames);
        }
/*
        //super.setOnTouchListener(onTouchListener);
        for(RelativeLayout rl: rlButtons) {
            rl.setOnTouchListener(l);
        }
        for(TextView b: serveButtons) {
            b.setOnTouchListener(l);
        }
*/
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean bEventDispatched = super.dispatchTouchEvent(ev);
        if ( bEventDispatched == false ) {
            // somehow in showcase viewmode event does not get dispatched
            for(TextView b: nameButtons) {
                bEventDispatched = bEventDispatched || b.dispatchTouchEvent(ev);
            }
            Log.d(TAG, String.format("Dispatched to name buttons for showcase: %s", bEventDispatched));
        }
        if ( bEventDispatched == false ) {
            for(TextView b: serveButtons) {
                bEventDispatched = bEventDispatched || b.dispatchTouchEvent(ev);
            }
            Log.d(TAG, String.format("Dispatched to serve buttons for showcase: %s", bEventDispatched));
        }
        return bEventDispatched;
    }

    private float fTxtSize       = 1.0f;
    private float fReduceFactor  = 1.0f;
    public void setTextSize(int unit, float size) {
        if ( fReduceFactor == 1.0f && ListUtil.size(nameButtons) == 2 ) {
            fReduceFactor = 0.75f; // TODO: customizable
        }
        this.fTxtSize       = size * fReduceFactor;
        for(TextView button: nameButtons) {
            if ( button instanceof AutoResizeTextView ) {
                AutoResizeTextView resizeTextView = (AutoResizeTextView) button;
                resizeTextView.setAutoResize(true);
                resizeTextView.invalidate();
            } else {
                button.setTextSize(unit, this.fTxtSize);
            }
        }
        float fTxtSizeServer = size * fReduceFactor * 0.75f;
        for(TextView button: serveButtons) {
            button.setTextSize(unit, fTxtSizeServer);
        }
    }

    public void addListener(AutoResizeTextView.OnTextResizeListener listener) {
        for(TextView button: nameButtons) {
            if ( button instanceof AutoResizeTextView ) {
                AutoResizeTextView artv = (AutoResizeTextView) button;
                artv.addOnResizeListener(listener);
            }
        }
    }

    private int textColor = Color.WHITE;
    public void setTextColor(int color) {
        this.textColor = color;
        for(TextView b: nameButtons) {
            b.setTextColor(this.textColor);
        }
    }

    private int bgColor = Color.BLACK;
    @Override public void setBackgroundColor(int color) {
        this.bgColor = color;
        for(TextView b: nameButtons) {
            ColorUtil.setBackground(b, this.bgColor);
        }
    }

    private int textColorServer = Color.YELLOW;
    public void setTextColorServer(int color) {
        this.textColorServer = color;
        for(TextView b: serveButtons) {
            b.setTextColor(this.textColorServer);
        }
        setServer(m_doublesServe, m_serveSide, m_isHandout, m_sServerDisplayValueOverwrite);
    }

    private int bgColorServer = Color.BLACK;
    public void setBackgroundColorServer(int color) {
        this.bgColorServer = color;
        for(TextView b: serveButtons) {
            ColorUtil.setBackground(b, this.bgColorServer);
        }
        setServer(m_doublesServe, m_serveSide, m_isHandout, m_sServerDisplayValueOverwrite);
    }

    private DoublesServe m_doublesServe = DoublesServe.NA;
    private ServeSide    m_serveSide    = ServeSide.R;
    private boolean      m_isHandout    = true;
    private String       m_sServerDisplayValueOverwrite = null;
    public void setServer(DoublesServe dsServer, ServeSide serveSide, boolean bIsHandout, String sDisplayValueOverwrite) {
        m_doublesServe = dsServer;
        if ( DoublesServe.NA.equals(m_doublesServe) == false ) {
            m_doublesReceiver = DoublesServe.NA; // can not be receiver and server at the same time
        }
        m_serveSide    = serveSide;
        m_isHandout    = bIsHandout;

        updateNameButtonColorsForServeReceive(dsServer, true);

        for(int i=0; i < serveButtons.size(); i++) {
            TextView b = serveButtons.get(i);
            if ( b == null ) { continue; } // should not be happening
            String sText = " ";
            if ( (i == dsServer.ordinal()) && (serveSide != null) ) {
                if ( StringUtil.isNotEmpty(sDisplayValueOverwrite) ) {
                    m_sServerDisplayValueOverwrite = sDisplayValueOverwrite;
                    sText = sDisplayValueOverwrite;
                } else {
                    sText = serveSide.toString() + (bIsHandout ? "?" : "");
                }
            }
            b.setText(sText);
        }
    }

    private final String sReceiverSymbol = "\u25CB";
    private DoublesServe m_doublesReceiver = DoublesServe.NA;
    public void setReceiver(DoublesServe dsReceiver) {
        m_doublesReceiver = dsReceiver;
        if ( DoublesServe.NA.equals(m_doublesReceiver) == false ) {
            m_doublesServe = DoublesServe.NA; // can not be receiver and server at the same time
            //m_serveSide    = null;
        }
        updateNameButtonColorsForServeReceive(dsReceiver, false);
        for(int i=0; i < serveButtons.size(); i++) {
            TextView b = serveButtons.get(i);
            if ( b == null ) { continue; } // should not be happening
            String sText = " ";
            if ( i == dsReceiver.ordinal() ) {
                sText = sReceiverSymbol;
            }
            b.setText(sText);
        }
    }

    private void updateNameButtonColorsForServeReceive(DoublesServe serverOrReceiver, boolean bShowDifferentColor) {
        bShowDifferentColor = bShowDifferentColor && (nameButtons.size() == 2); /*&& (serveButtons.size() == 0)*/;
        for(int i=0; i < nameButtons.size(); i++) {
            TextView b = nameButtons.get(i);
            if ( ( i == serverOrReceiver.ordinal() ) && bShowDifferentColor ) {
                b.setTextColor(this.textColorServer);
                ColorUtil.setBackground(b, this.bgColorServer);
            } else {
                b.setTextColor(this.textColor);
                ColorUtil.setBackground(b, this.bgColor);
            }
        }
    }

    @Override public String toString() {
        return nameButtons.get(0).getText() + "/" + nameButtons.get(1).getText()
           + " : " + (m_doublesServe   ) + "/" + m_serveSide
           + " : " + (m_doublesReceiver);
    }

/*
    @Override public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
    }

    @Override public void setBackground(Drawable background) {
        super.setBackground(background);
    }
*/
    private Direction bDrawTouch = Direction.Unknown;
    private int       m_touchColor = 0;
    public void drawTouch(Direction direction, int id, int color) {
        if ( direction != null ) {
            this.bDrawTouch = direction;
        }
        this.m_touchColor = color;
        requestLayout();
        invalidate();
    }
    private void _drawTouch(Canvas canvas) {
        int iAlpha = 255;
        Paint mPaint = new Paint();
        mPaint.setAlpha(iAlpha);
        mPaint.setColor(m_touchColor);
        int cx = 60;
        int cy = getHeight() / 2;
        if ( bDrawTouch.toString().contains("E")) {
            cx = getWidth() - 60;
        }
        canvas.drawCircle(cx, cy, 15, mPaint);

/*
        synchronized (this) {
            try {
                wait(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.bDrawTouch = Direction.Unknown;
        invalidate();
*/
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        // draw here if you want to draw behind children
        super.dispatchDraw(canvas);
        // draw here if you want to draw over children
        if ( (bDrawTouch != null) && bDrawTouch.equals(Direction.Unknown) == false ) {
            _drawTouch(canvas);
        }
    }
/*
    @Override protected void dispatchDraw(Canvas canvas)
    {
        Rect clip=new Rect();
        getDrawingRect(clip);
        int saveCount = canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(clip, Region.Op.REPLACE);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }
*/
}
