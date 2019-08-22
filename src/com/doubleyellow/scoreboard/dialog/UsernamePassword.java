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

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.doubleyellow.android.util.MyPasswordTransformationMethod;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.feed.Authentication;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;
import com.doubleyellow.util.StringUtil;

/**
 * Request username and password for e.g. basic authentication before posting a result to a site.
 */
public class UsernamePassword extends BaseAlertDialog {

    private static final String NAME_PREFKEY = "Name";

    public UsernamePassword(Context context, ScoreBoard scoreBoard) {
        super(context, null, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putString(Authentication.class.getName(), m_authentication.toString());
        outState.putString(NAME_PREFKEY, m_sName);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        Authentication authentication = Authentication.valueOf(outState.getString(Authentication.class.getName()));
        String sName = outState.getString(NAME_PREFKEY);
        init(sName, authentication);
        show();
        return true;
    }

    private String         m_sName          = null;
    private Authentication m_authentication = Authentication.None;

    @Override public void show() {

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        // add user name and password elements
        etUsername = new EditText(context);
        etUsername.setInputType(InputType.TYPE_CLASS_TEXT);
        etUsername.setHint(R.string.username);
        String sUserName = PreferenceValues.getString(getUsernamePrefKey(m_sName), "", context);
        if (StringUtil.isNotEmpty(sUserName) ) {
            etUsername.setText(sUserName);
        }
        ll.addView(etUsername);

        etPassword = new EditText(context);
        etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
        etPassword.setTransformationMethod(new MyPasswordTransformationMethod('•', 1));
        etPassword.setHint(R.string.password);
        if ( PreferenceValues.savePasswords(context) ) {
            String sPassword = PreferenceValues.getString(getPasswordPrefKey(m_sName), "", context);
            if (StringUtil.isNotEmpty(sPassword) ) {
                etPassword.setText(sPassword);
            }
        }
        ll.addView(etPassword);

        CheckBox cbStorePasswords = new PreferenceCheckBox(scoreBoard, PreferenceKeys.savePasswords, R.bool.savePasswords_default, false);
        cbStorePasswords.setText(R.string.save_password);
        cbStorePasswords.setTextColor(Color.WHITE);
        ll.addView(cbStorePasswords);

        String sTitle = String.format("%s - %s %s", m_sName, m_authentication, Authentication.class.getSimpleName());

        dialog = adb.setIcon(android.R.drawable.ic_lock_lock)
                .setTitle(sTitle)
                .setPositiveButton(R.string.cmd_ok    , null)
                .setNegativeButton(R.string.cmd_cancel, null)
                .setView(ll)
                .create();
        dialog.setOnShowListener(onShowListener);

        // in a try catch to prevent crashing if somehow scoreBoard is not showing any longer
        try {
            dialog.show();

            if (StringUtil.isEmpty(sUserName) ) {
                etUsername.requestFocus();
                ViewUtil.showKeyboard(dialog);
            } else {
                if (StringUtil.isEmpty(etPassword.getText())) {
                    etPassword.requestFocus();
                    ViewUtil.showKeyboard(dialog);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String sName, Authentication authentication) {
        m_sName          = sName;
        m_authentication = authentication;
    }

    private String getUsernamePrefKey(String sFeedName) {
        return sFeedName + "__" + NAME_PREFKEY + "__" + getString(R.string.username);
    }
    private String getPasswordPrefKey(String sFeedName) {
        return sFeedName + "__" + NAME_PREFKEY + "__" + getString(R.string.password);
    }

    private EditText etUsername = null;
    private EditText etPassword = null;

    private DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        //We register an onClickListener for the 'positive' button. There is already one, but that one closes the dialog no matter what.
        @Override public void onShow(DialogInterface dialogInterface) {
            final Button btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (btnOk == null) return;
            btnOk.setOnClickListener(onClickOkListener);
        }
    };

    @Override public void handleButtonClick(int which) {
        onClickOkListener.onClick(null);
    }

    private View.OnClickListener onClickOkListener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            switch (m_authentication) {
                case BodyParameters:
                case Basic:
                    if ( areAuthenticationFieldsSpecified() ) {
                        PreferenceValues.setString(getUsernamePrefKey(m_sName), etUsername.getText().toString(), context);
                        String passwordPrefKey = getPasswordPrefKey(m_sName);
                        if ( PreferenceValues.savePasswords(context) ) {
                            PreferenceValues.setString(passwordPrefKey, etPassword.getText().toString(), context);
                        } else {
                            // remove password, so that if later 'save password' is enable again user still needs to provide it at least once
                            PreferenceValues.remove(passwordPrefKey, context);
                        }
                        // TODO: flexibility: this dialog is not reuasable with this
                        scoreBoard.postMatchResult(m_authentication, etUsername.getText().toString(), etPassword.getText().toString());
                        dialog.cancel();
                    } else {
                        // prevent dialog from closing
                    }
                    break;
                default:
                    break;
            }
        }
        private boolean areAuthenticationFieldsSpecified() {
            TextView[] textViews = {etUsername, etPassword};
            for (TextView txt : textViews) {
                if (txt.getText().toString().trim().length() == 0) {
                    //txt.setError(getString(msg_enter_authentication_details)); // To aggressive
                    txt.setHint(R.string.msg_enter_authentication_details); // TODO: does this work a screen rotation has occured
                    return false;
                } else {
                    txt.setError(null);
                    txt.setHint(null);
                }
            }
            return true;
        }
    };
}