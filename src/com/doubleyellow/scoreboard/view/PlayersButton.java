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
 * For doubles this view includes the serve side button
 *
 * If countries are specified this class can also display flags
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

    public static final String SUBBUTTON = "SUBBUTTON";
    private List<TextView>       nameButtons  = new ArrayList<TextView>();
    private List<TextView>       serveButtons = new ArrayList<TextView>();
    private List<ImageView>      flagImages   = new ArrayList<ImageView>(); // for now only just the one (even for doubles)
    private boolean              m_bIsDoubles = false;
    public void setPlayers(String players, boolean bIsDoubles) {
        if ( m_bIsDoubles != bIsDoubles ) {
            nameButtons  = new ArrayList<>();
            serveButtons = new ArrayList<>();
            flagImages   = new ArrayList<>();
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
            int iXmlIdOfSideButton      = R.id.btn_side1;
            int sideButtonAlignParentRL = PercentRelativeLayout.ALIGN_PARENT_RIGHT;
            int flagImageAlignParentRL  = PercentRelativeLayout.ALIGN_PARENT_LEFT;
            //int playerNameAlignParentRL = PercentRelativeLayout.ALIGN_PARENT_LEFT;
            int playerNameLRofServe     = PercentRelativeLayout.LEFT_OF;
            int playerNameLRofFlag      = PercentRelativeLayout.RIGHT_OF;
            int playerNameAlignLRofPartner = PercentRelativeLayout.ALIGN_LEFT;
            if ( (this.getId() == R.id.txt_player2) ) {
                iXmlIdOfSideButton      = R.id.btn_side2;
                if ( ViewUtil.isLandscapeOrientation(getContext()) ) {
                    sideButtonAlignParentRL = PercentRelativeLayout.ALIGN_PARENT_LEFT;
                    flagImageAlignParentRL  = PercentRelativeLayout.ALIGN_PARENT_RIGHT;
                    //playerNameAlignParentRL = PercentRelativeLayout.ALIGN_PARENT_RIGHT;
                    playerNameLRofServe     = PercentRelativeLayout.RIGHT_OF;
                    playerNameLRofFlag      = PercentRelativeLayout.LEFT_OF;
                    playerNameAlignLRofPartner = PercentRelativeLayout.ALIGN_RIGHT;
                }
            }

            int iNameButtonId      = 100 + n;
            int iServeSideButtonID = 200 + n;
            int iFlagImageID       = 300 + n;
            if ( serveButtons.size() <= n ) {
                // serve side button
                PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null);
                rlp.addRule(sideButtonAlignParentRL);
                if ( serveButtons.size() == 1 ) {
                    rlp.addRule(PercentRelativeLayout.BELOW, iServeSideButtonID - 1);
                }

                PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                info.heightPercent = bIsDoubles?0.50f:1.0f;
                info.widthPercent  = -1.0f;
                info.aspectRatio   =  1.5f;

                TextView bServeSide = new AutoResizeTextView(new ContextThemeWrapper(getContext(), R.style.SBButton) /*, null, R.style.SBButton*/);
                bServeSide.setId(iServeSideButtonID);
                bServeSide.setTag(iXmlIdOfSideButton + SUBBUTTON + n); // used to calculate parent id from within ScoreBoard listeners
                if ( bIsDoubles == false ) {
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

            if ( flagImages.size() <= 0 ) {
                // flag image
                PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(getContext(), null);
                rlp.addRule(flagImageAlignParentRL);

                PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                info.heightPercent =  1.0f;
                info.widthPercent  = -1.0f;
                info.aspectRatio   =  1.6f; // TODO: dynamic dependant of where we retrieve images from

                ImageView ivFlag = new ImageView(getContext());
                ivFlag.setVisibility(GONE);
                ivFlag.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // FIT_XY
                //ivFlag.setImageResource(R.drawable.logo);
                ivFlag.setId(iFlagImageID);
                ivFlag.setBackgroundResource(R.drawable.image_border);
                //ivFlag.setTag(iXmlIdOfSideButton + SUBBUTTON + n); // used to calculate parent id from within ScoreBoard listeners
                super.addView(ivFlag, rlp);
                flagImages.add(ivFlag);

                if (onTouchListenerNames != null) {
                    ivFlag.setOnTouchListener(onTouchListenerNames);
                }
            }

            TextView b = null;
            if ( nameButtons.size() <= n ) {
                // player buttons
                PercentRelativeLayout.LayoutParams rlp = new PercentRelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                //rlp.addRule(playerNameAlignParentRL);
                rlp.addRule(playerNameLRofServe, iServeSideButtonID);
                if ( nameButtons.size() == 1 ) {
                    rlp.addRule(PercentRelativeLayout.BELOW, iNameButtonId - 1);
                    rlp.addRule(playerNameAlignLRofPartner, iNameButtonId - 1);
                } else {
                    rlp.addRule(playerNameLRofFlag , iFlagImageID);
                }

                PercentLayoutHelper.PercentLayoutInfo info = rlp.getPercentLayoutInfo();
                info.heightPercent = bIsDoubles?0.50f:1.0f;

                b = new AutoResizeTextView(new ContextThemeWrapper(getContext(), R.style.SBButton) /*, null, R.style.SBButton*/);
                b.setId(iNameButtonId);
                b.setTag(this.getId() + SUBBUTTON + n); // used to calculate parent id from witin ScoreBoard listeners
                b.setTypeface(null, Typeface.BOLD); // TODO: not four doubles?
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
                nameButtons.get(1).setVisibility(GONE);
            }

            // no serve buttons for singles (TODO: via preferences)
            serveButtons.get(0).setVisibility(GONE);
            if ( serveButtons.size() == 2 ) {
                serveButtons.get(1).setVisibility(GONE);
            }
        }

        setServer(doublesServe, serveSide, isHandout, null);
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
                flagImages.get(0).setVisibility(GONE);
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
                if ( bShowFlag ) {
                    this.lHasCountry.add(ShowCountryAs.FlagNextToNameOnDevice);
                    PreferenceValues.downloadImage(getContext(), flagImages.get(0), sCountryCode);
                } else {
                    flagImages.get(0).setVisibility(GONE);
                }
            }
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
                // guess an abbreviation
                String sTst = sClub.replaceAll("\\b(\\w)[A-Za-z\\-]*\\b", "$1").replaceAll("\\s", "");
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
    @Override public void setOnTouchListener(OnTouchListener lNames) {
        onTouchListenerNames = lNames;
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
        setServer(doublesServe, serveSide, isHandout, null);
    }

    private int bgColorServer = Color.BLACK;
    public void setBackgroundColorServer(int color) {
        this.bgColorServer = color;
        for(TextView b: serveButtons) {
            ColorUtil.setBackground(b, this.bgColorServer);
        }
        setServer(doublesServe, serveSide, isHandout, null);
    }

    private DoublesServe doublesServe = DoublesServe.NA;
    private ServeSide    serveSide    = ServeSide.R;
    private boolean      isHandout    = true;
    public void setServer(DoublesServe iServer, ServeSide serveSide, boolean bIsHandout, String sDisplayValueOverwrite) {
        this.doublesServe = iServer;
        this.serveSide    = serveSide;
        this.isHandout    = bIsHandout;
        boolean bShowDifferentColor = (nameButtons.size() == 2) /*&& (serveButtons.size() == 0)*/;
        for(int i=0; i < nameButtons.size(); i++) {
            TextView b = nameButtons.get(i);
            if ( ( i == iServer.ordinal() ) && bShowDifferentColor ) {
                b.setTextColor(this.textColorServer);
                ColorUtil.setBackground(b, this.bgColorServer);
            } else {
                b.setTextColor(this.textColor);
                ColorUtil.setBackground(b, this.bgColor);
            }
        }
        for(int i=0; i < serveButtons.size(); i++) {
            TextView b = serveButtons.get(i);
            if ( b == null ) { continue; } // should not be happening
            String sText = " ";
            if ( (i == iServer.ordinal()) && (serveSide != null) ) {
                if ( StringUtil.isNotEmpty(sDisplayValueOverwrite) ) {
                    sText = sDisplayValueOverwrite;
                } else {
                    sText = serveSide.toString() + (bIsHandout ? "?" : "");
                }
            }
            b.setText(sText);
        }
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
