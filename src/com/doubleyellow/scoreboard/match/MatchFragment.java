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

package com.doubleyellow.scoreboard.match;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * To present {@link MatchView} in a fragment for {@link MatchTabbed}
 */
public class MatchFragment extends Fragment {

//  private MatchView rootView = null;
//  private Context   context  = null;

    /** Invoked just before it MAY come into view on the next slide and it was no longer a sibling of currently showing view  */
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //if (rootView == null) { // removed in attempt to fix 'after rotate' problem: does not work
            Context context = getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                context = getContext();
            }
            boolean bIsDoubles = this instanceof MatchFragmentDoubles;
            MatchView rootView = new MatchView(context, bIsDoubles, null, PreferenceValues.getNewMatchLayout(context));
            ColorPrefs.setColors(getActivity(), rootView);
        //}

        return rootView;
    }


/*
    public MatchFragment() {
        super();
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        //this.context = context;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = (MatchView) view;
    }

    @Override public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }
*/

    /** For fragments in tabs only invoked when it is actually created for the first time. Triggered before onCreateView */
/*
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
*/

    Intent getIntent(boolean bBackPressed) {
        View view = getView();
        Context context = getActivity();
        if ( context == null ) {
            return null;
        }
        if ( view == null ) {
            Toast.makeText(context, "Sorry... no root view in " + this.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            return null;
        }
        if ( (view instanceof MatchView) == false ) {
            Toast.makeText(context, "Sorry... root view in " + this.getClass().getSimpleName() + " is not a MatchView but a " + view.getClass().getName(), Toast.LENGTH_LONG).show();
            return null;
        }
        MatchView matchView = (MatchView) view;
        String sFeedKey = (this instanceof MatchFragmentDoubles) ? "ManualDoubles" : "Manual";
        return matchView.getIntent(sFeedKey, null, bBackPressed);
    }
}
