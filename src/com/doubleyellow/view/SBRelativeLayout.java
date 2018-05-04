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

package com.doubleyellow.view;

import android.content.Context;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.doubleyellow.android.view.ArrowView;
import com.doubleyellow.android.view.TouchBothListener;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.util.Direction;

import java.util.*;

/**
 * Extended to
 * - have more flexible touch event handling
 * - allow drawing arrows for educational/demo purposes
 */
public class SBRelativeLayout extends PercentRelativeLayout {

    public SBRelativeLayout(Context context) {
        super(context);
    }

    public SBRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SBRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ( touchBothListener != null) {
            boolean intercept = touchBothListener.intercept(this, ev);
            if (intercept) {
                Log.i("SB.RelativeLayout", "intercept returns " + intercept);
            }
            return intercept;
        }
        return bInterceptTouchEvents;
        //return super.onInterceptTouchEvent(ev);
    }

    private TouchBothListener touchBothListener = null;

    private boolean bInterceptTouchEvents = false;
    @Override public void setOnTouchListener(View.OnTouchListener l) {
        if ( l instanceof TouchBothListener) {
            touchBothListener = (TouchBothListener) l;
        }
        super.setOnTouchListener(l);
    }

    private ArrowView[] vArrow = new ArrowView[2];
    public void hideArrows() {
        for(int a=0; a < vArrow.length; a++) {
            View arrow = vArrow[a];
            if (arrow != null) {
                arrow.setVisibility(View.GONE);
            }
        }
    }

    public void drawArrow(int[] vRelatedGuiElements, int bgColor) {
        drawArrow(vRelatedGuiElements, null, bgColor);
    }

    public void drawArrow(int[] vRelatedGuiElements, Direction[] arrowDirection, int bgColor)
    {
        if ( vRelatedGuiElements == null) { return; } // e.g. a menu id
        if ( vRelatedGuiElements.length == 0) { return; }

        // ensure both arrows are hidden first if only one is 'moved'
        if ( vRelatedGuiElements.length != 2 ) {
            hideArrows();
        }

        if ( vRelatedGuiElements.length == 5 ) {
            // special case
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(vRelatedGuiElements[0]       , vRelatedGuiElements[1]);
            params.addRule(vRelatedGuiElements[2]       , vRelatedGuiElements[3]);
            Direction d = Direction.values()[vRelatedGuiElements[4]];
            showArrow(0, params, d, bgColor);
            return;
        }

        for(int a=0; a < vRelatedGuiElements.length; a++) {
            View vRelatedGuiElement = this.findViewById(vRelatedGuiElements[a]);
            if ( vRelatedGuiElement == null ) { continue; } // TODO: e.g. a menu item
            //Log.w(TAG, "Drawing arrow towards " + vRelatedGuiElement.getClass().getSimpleName());

            View vParent = vRelatedGuiElement.getRootView();
            int iQuarterScreenWidth  = vParent.getWidth()  / 4; // e.g. 120
            int iQuarterScreenHeight = vParent.getHeight() / 4; // e.g. 200
            float x = vRelatedGuiElement.getX();
            float y = vRelatedGuiElement.getY(); // x,y = (6,78) for score button 1 in portrait | (3,3) for player1 in portrait | (6,400) for score2 in portrait
            float w = vRelatedGuiElement.getWidth();
            float h = vRelatedGuiElement.getHeight();

            // determine what x/y quarters of the screen the gui elements are located
            List<String> lXIsInQuarter = new ArrayList<String>(Arrays.asList("1", "2", "3", "4"));
            List<String> lYIsInQuarter = new ArrayList<String>(lXIsInQuarter);
            calculateQuarters(iQuarterScreenWidth, iQuarterScreenHeight, x, y, w, h, lXIsInQuarter, lYIsInQuarter);

            Direction direction = Direction.E;
            Direction bHasXMirror = null;
            Direction bHasYMirror = null;
            if ( arrowDirection == null ) {
                EnumSet<Direction> directions = EnumSet.allOf(Direction.class);

                if (lXIsInQuarter.size() > lYIsInQuarter.size()) {
                    bHasXMirror = eliminateDirectionsBasedOnXQuarters(lXIsInQuarter, directions);
                    bHasYMirror = eliminateDirectionsBasedOnYQuarters(lYIsInQuarter, directions);
                } else {
                    bHasYMirror = eliminateDirectionsBasedOnYQuarters(lYIsInQuarter, directions);
                    bHasXMirror = eliminateDirectionsBasedOnXQuarters(lXIsInQuarter, directions);
                }

                //Log.w(TAG, "Possible arrow directions [" + a + "] " + directions);
                //Log.w(TAG, String.format("Has possible mirror [" + a + "] %s %s", bHasXMirror, bHasYMirror));
                Iterator<Direction> iterator = directions.iterator();
                if (iterator.hasNext()) {
                    direction = iterator.next();
                    while (iterator.hasNext()) {
                        Direction better = iterator.next();
                        if (better.toString().length() < direction.toString().length()) {
                            //Log.w(TAG, String.format("Switching to hopefully better from %s to %s", direction, better));
                            direction = better;
                        }
                    }
                }
            } else {
                direction = arrowDirection[a];
            }
            LayoutParams params = calculateLayoutParams(vRelatedGuiElement, bHasXMirror, bHasYMirror, direction);
            //params.leftMargin = 107;
            showArrow(a, params, direction, bgColor);
        }
    }

    private void showArrow(int a, LayoutParams params, Direction direction, int bgColor) {
        if (vArrow[a] == null) {
            vArrow[a] = new ArrowView(getContext());
        } else {
            this.removeView(vArrow[a]);
        }
        vArrow[a].setColor(bgColor);
        vArrow[a].setVisibility(View.VISIBLE);

        if ( true ) {
            vArrow[a].setDirection(direction);
            this.addView(vArrow[a], params);
        } else {
            //vArrow[a].setDirection(Direction.SE);
            //this.addView(vArrow[a], (int) vRelatedGuiElement.getX(), (int) vRelatedGuiElement.getY());
        }
    }

    private LayoutParams calculateLayoutParams(View vRelatedGuiElement, Direction bHasXMirror, Direction bHasYMirror, Direction direction)
    {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switch(direction) {
            case N:
                params.addRule(RelativeLayout.BELOW       , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.ALIGN_LEFT  , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                break;
            case NE:
                params.addRule(RelativeLayout.BELOW       , vRelatedGuiElement.getId());
                params.addRule(RelativeLayout.LEFT_OF     , vRelatedGuiElement.getId());
                break;
            case E:
                params.addRule(RelativeLayout.LEFT_OF     , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.ALIGN_TOP   , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                break;
            case SE:
                params.addRule(RelativeLayout.LEFT_OF     , vRelatedGuiElement.getId());
                params.addRule(RelativeLayout.ABOVE       , vRelatedGuiElement.getId());
                break;
            case S:
                params.addRule(RelativeLayout.ABOVE       , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.ALIGN_RIGHT , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                break;
            case SW:
                params.addRule(RelativeLayout.RIGHT_OF    , vRelatedGuiElement.getId());
                params.addRule(RelativeLayout.ABOVE       , vRelatedGuiElement.getId());
                break;
            case W:
                params.addRule(RelativeLayout.RIGHT_OF    , vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.ALIGN_BOTTOM, vRelatedGuiElement.getId());
                //params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                break;
            case NW:
                params.addRule(RelativeLayout.RIGHT_OF    , vRelatedGuiElement.getId());
                params.addRule(RelativeLayout.BELOW       , vRelatedGuiElement.getId());
                break;
        }

        // let the center of the arrow be centered with the element it is pointing to
        int iWHHalf = ArrowView.getWH() / 2;
        switch(direction) {
            case N:
            case S:
                params.addRule(RelativeLayout.ALIGN_LEFT, vRelatedGuiElement.getId());
                params.leftMargin = (vRelatedGuiElement.getWidth() / 2) - iWHHalf;
                break;
            case E:
            case W:
                params.addRule(RelativeLayout.ALIGN_TOP, vRelatedGuiElement.getId());
                params.topMargin = (vRelatedGuiElement.getHeight() / 2) - iWHHalf;
                break;
        }
        // let the arrow slight flow over the target element
        switch(direction) {
            case N: params.bottomMargin = - iWHHalf / 4; break;
            case S: params.topMargin    = - iWHHalf / 4; break;
            case E: params.leftMargin   = - iWHHalf / 4; break;
            case W: params.rightMargin  = - iWHHalf / 4; break;
        }

/*
        int iAlign = 0;
        if (direction.toString().length()==1) {
            switch (bHasXMirror) {
                case E:
                    iAlign = direction.getAngle()%180==0?RelativeLayout.ALIGN_RIGHT:RelativeLayout.ALIGN_TOP;
                    break;
                case W:
                    iAlign = direction.getAngle()%180==0?RelativeLayout.ALIGN_LEFT:RelativeLayout.ALIGN_TOP;
                    break;
            }
            switch (bHasYMirror) {
                case N:
                    iAlign = direction.getAngle()%180==0?RelativeLayout.ALIGN_RIGHT:RelativeLayout.ALIGN_TOP;
                    break;
                case S:
                    iAlign = direction.getAngle()%180==0?RelativeLayout.ALIGN_RIGHT:RelativeLayout.ALIGN_BOTTOM;
                    break;
            }
            if ( iAlign != 0 ) {
                params.addRule(iAlign, vRelatedGuiElement.getId());
            }
        }
*/
        return params;
    }

    private void calculateQuarters(int iQuarterScreenWidth, int iQuarterScreenHeight, float x, float y, float w, float h, List<String> lXIsInQuarter, List<String> lYIsInQuarter) {
        for(int q=1;q<=3;q++) {
            String qBefore = String.valueOf(q);
            String qAfter  = String.valueOf(q+1);
            if (x > iQuarterScreenWidth*q) {
                lXIsInQuarter.remove(qBefore);
            }
            if (y > iQuarterScreenHeight*q) {
                lYIsInQuarter.remove(qBefore);
            }
            if (x+w < iQuarterScreenWidth*q) {
                lXIsInQuarter.remove(qAfter);
            }
            if (y+h < iQuarterScreenHeight*q) {
                lYIsInQuarter.remove(qAfter);
            }
        }
        //Log.w(TAG, " x quarters :" + lXIsInQuarter);
        //Log.w(TAG, " y quarters :" + lYIsInQuarter);
    }

    private Direction eliminateDirectionsBasedOnXQuarters(List<String> lXIsInQuarter, EnumSet<Direction> directions) {
        Direction bMostLikelyHasMirrorInX = Direction.Unknown;

        List<Direction> E3 = Arrays.asList(Direction.SE, Direction.E , Direction.NE);
        List<Direction> W3 = Arrays.asList(Direction.W , Direction.NW, Direction.SW);
        List<Direction> NS = Arrays.asList(Direction.N , Direction.S);

        int iFirstQuarterX = Integer.parseInt(lXIsInQuarter.get(0)) ;
        if ( lXIsInQuarter.size() == 4 ) {
            directions.retainAll(NS);
        } else if ( lXIsInQuarter.size() == 3 ) {
            if ( iFirstQuarterX == 1) {
                // no room to point eastwards
                directions.removeAll(E3);
            } else if ( iFirstQuarterX ==2 ) {
                // no room to point westwards
                directions.removeAll(W3);
            }
        } else if ( lXIsInQuarter.size() == 2 ) {
            if (iFirstQuarterX==1 || iFirstQuarterX==3) {
                if ( false && directions.size() > 2 ) {
                    // not in the center so assume there is a mirroring element that will also receive an arrow
                    directions.retainAll(NS);
                } else {
                    switch (iFirstQuarterX) {
                        case 1:
                            // no room to point east
                            directions.removeAll(E3);
                            break;
                        case 3:
                            // no room to point west
                            directions.removeAll(W3);
                            break;
                    }
                }
            }
        } else {
            // just one quarter
            switch (iFirstQuarterX) {
                case 1:
                case 3:
                    directions.retainAll(W3);
                    break;
                case 2:
                case 4:
                    directions.retainAll(E3);
                    break;
            }
        }
        if ( ViewUtil.isLandscapeOrientation(getContext()) && (lXIsInQuarter.size() <= 2)) {
            bMostLikelyHasMirrorInX = (iFirstQuarterX <= 2) ? Direction.E : Direction.W;
        }

        return bMostLikelyHasMirrorInX;
    }

    private Direction eliminateDirectionsBasedOnYQuarters(List<String> lYIsInQuarter, EnumSet<Direction> directions) {
        Direction bMostLikelyHasMirrorInY = Direction.Unknown;

        List<Direction> S3 = Arrays.asList(Direction.S , Direction.SE, Direction.SW);
        List<Direction> N3 = Arrays.asList(Direction.NE, Direction.N , Direction.NW);
        List<Direction> EW = Arrays.asList(Direction.E , Direction.W);

        int iFirstQuarterY = Integer.parseInt(lYIsInQuarter.get(0));
        if ( lYIsInQuarter.size() == 4 ) {
            directions.retainAll(EW);
        } else if ( lYIsInQuarter.size() == 3 ) {
            if ( iFirstQuarterY == 1) {
                // no room to point southwards
                directions.removeAll(S3);
            } else if ( iFirstQuarterY ==2 ) {
                // no room to point norhtwards
                directions.removeAll(N3);
            }
        } else if ( lYIsInQuarter.size() == 2 ) {
            if (iFirstQuarterY==1 || iFirstQuarterY==3) {
                if ( false && directions.size() > 2 ) {
                    // not in the center so assume there is a mirroring element that will also receive an arrow
                    directions.retainAll(EW);
                } else {
                    switch (iFirstQuarterY) {
                        case 1:
                            // no room to point south
                            directions.removeAll(S3);
                            break;
                        case 3:
                            // no room to point north
                            directions.removeAll(N3);
                            break;
                    }
                }
            }
        } else {
            // just one quarter
            switch (iFirstQuarterY) {
                case 1:
                case 3:
                    directions.retainAll(N3);
                    break;
                case 2:
                case 4:
                    directions.retainAll(S3);
                    break;
            }
        }
        if ( ViewUtil.isPortraitOrientation(getContext()) && (lYIsInQuarter.size() <= 2)) {
            bMostLikelyHasMirrorInY = (iFirstQuarterY <= 2) ? Direction.S : Direction.N;
        }

        return bMostLikelyHasMirrorInY;
    }

}
