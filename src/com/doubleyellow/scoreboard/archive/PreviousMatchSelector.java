package com.doubleyellow.scoreboard.archive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.ResultSender;
import com.doubleyellow.scoreboard.dialog.*;
import com.doubleyellow.scoreboard.history.MatchHistory;
import com.doubleyellow.scoreboard.main.DialogManager;
import com.doubleyellow.scoreboard.match.ExpandableMatchSelector;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.ExpandableListUtil;
import com.doubleyellow.util.*;

import java.io.File;
import java.util.*;

/**
 * Fragment that allows user to select a match that was scored/reffed earlier to review the details.
 * Use by ArchiveTabbed
 */
public class PreviousMatchSelector extends ExpandableMatchSelector
{
           static String sNoMatchesStored = null;
    public static final String TAG = "SB." + PreviousMatchSelector.class.getSimpleName();

    private SortOrder sortOrder = SortOrder.Descending;

    @Override public void onCreate(Bundle savedInstanceState) {
        sNoMatchesStored = getResources().getString(R.string.sb_no_matches_stored);

        sortOrder = PreferenceValues.sortOrderOfArchivedMatches(context);

        super.onCreate(savedInstanceState);
    }

    @Override protected void setGuiDefaults(List<String> lExpanded) {
        GroupMatchesBy      sortBy              = PreferenceValues.groupArchivedMatchesBy(context); // Event, Date
        String              sMode               = sortOrder + "." + sortBy;
        GroupStatusRecaller groupStatusRecaller = GroupStatusRecaller.getInstance(sMode);

        int iExpandedAfterRestore = ExpandableListUtil.restoreStatus(expandableListView, groupStatusRecaller);
        if ( iExpandedAfterRestore <= 0 ) {
            if ( sortOrder.equals(SortOrder.Ascending) ) {
                ExpandableListUtil.expandAllOrLast(expandableListView, 1);
            } else {
                ExpandableListUtil.expandAllOrFirst(expandableListView, 1);
            }
        }
        expandableListView.setOnGroupCollapseListener(groupStatusRecaller);
        expandableListView.setOnGroupExpandListener  (groupStatusRecaller);
    }

    private void confirmDeleteHeader(final String sHeader) {
        ScoreBoard.getAlertDialogBuilder(context)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setTitle         (getString(R.string.sb_delete_group_of_matches_confirm, sHeader) )
                .setNegativeButton(R.string.cmd_cancel, null)
                .setPositiveButton(R.string.cmd_delete, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        _deleteHeader(sHeader);
                    }
                }).show();
    }

    private void confirmDeleteMatch(final File f, final String sHeader, final String sText) {
        ScoreBoard.getAlertDialogBuilder(context)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setTitle         (getString(R.string.sb_delete_item, sText) )
                .setNegativeButton(R.string.cmd_cancel, null)
                .setPositiveButton(R.string.cmd_delete, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        f.delete();
                        emsAdapter.load(false);
                    }
                }).show();
    }

    private void _deleteHeader(String sHeader) {
        List<File> lFiles = (List<File>) emsAdapter.getObjects(sHeader);
        if ( ListUtil.isEmpty(lFiles) ) { return; }
        for( File f: lFiles ) {
            f.delete();
        }
        emsAdapter.load(false);
    }

    @Override public ExpandableListView.OnChildClickListener getOnChildClickListener() {
        return new ExpandableListView.OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                matchDetails(v, groupPosition, childPosition);
                return false;
            }
        };
    }

    @Override public AdapterView.OnItemLongClickListener getOnItemLongClickListener() {
        return null;
    }

    static void selectSortingOptions(Context context) {
        SortOptions so = new SortOptions(context, null, null);
        DialogManager.getInstance().show(so);
    }
    public static void selectFilenameForExport(Context context) {
        Export export = new Export(context, null, null);
        DialogManager.getInstance().show(export);
    }

    public static void selectFilenameForImport(Context context) {
        Import imp = new Import(context, null, null);
        DialogManager.getInstance().show(imp);
    }

    private void matchDetails(View v, int groupPosition, int childPosition) {
        File f = (File) emsAdapter.getObject(groupPosition, childPosition);
        matchDetails(f);
    }
    private void selectMatch(File f)
    {
        if ( f == null ) { return; }

        Intent intent = new Intent();
        intent.putExtra(PreviousMatchSelector.class.getSimpleName(), f);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }
    private void matchDetails(File f)
    {
        if ( f == null ) { return; }
        if ( f.exists() == false ) {
            Log.w(TAG, "File does not exist: " + f.getAbsolutePath()); // e.g. manually deleted from the device
            Toast.makeText(getActivity(), String.format("File %s no longer present. Refresh might be appropriate...", f.getAbsolutePath()), Toast.LENGTH_LONG).show();
            // refresh cache
            //emsAdapter.load(false);
            return;
        }

        Intent matchHistory = new Intent(context, MatchHistory.class);
        Bundle b = new Bundle();
        b.putSerializable(MatchHistory.class.getSimpleName(), f);
        matchHistory.putExtra(MatchHistory.class.getSimpleName(), b);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            activity.startActivity(matchHistory, b);
        }
    }

    private EMSAdapter emsAdapter;
    @Override public SimpleELAdapter getListAdapter(LayoutInflater inflater) {
        if ( emsAdapter == null ) {
            emsAdapter = new EMSAdapter(inflater);
        }
        return emsAdapter;
    }

    private class EMSAdapter extends SimpleELAdapter
    {
        private EMSAdapter(LayoutInflater inflater)
        {
            super(inflater, R.layout.expandable_match_selector_group_with_menu, R.layout.expandable_match_selector_item_with_menu, null, false);

            load(Brand.isSquash());
        }

        public void load(boolean bUseCacheIfPresent)
        {
            String sNewMsg = getString(R.string.loading);
            boolean bAsync = true; //sNewMsg.equals(m_sLastMessage);

            showProgress(sNewMsg, null);

            sortOrder = PreferenceValues.sortOrderOfArchivedMatches(context);
            sortItems(sortOrder); // TODO: does not have desired effect if player names are first in display name of match
            sortHeaders(sortOrder);

            setGuiDefaults(null);

            // start separated task to ensure loading message is actually shown
            ReadStoredMatches rsmTask = new ReadStoredMatches(PreviousMatchSelector.this, this, bUseCacheIfPresent);
            if ( bAsync ) {
                rsmTask.execute(); // doing this in background works initially with nice progress message, but sorting no longer works
            } else {
                rsmTask.doInBackground();
                rsmTask.onPostExecute(null);
            }
        }

        @Override protected void doPostInflateItem(final View v, final String sHeader, final String sText) {
            ImageButton mImageButtonOverflow = (ImageButton) v.findViewById(R.id.card_item_button_overflow);
            if ( mImageButtonOverflow == null ) { return; }

            final PopupMenu mPopupMenu = new PopupMenu(context, mImageButtonOverflow);
            MenuInflater inflater = mPopupMenu.getMenuInflater();
            inflater.inflate(R.menu.previous_match_item_menu, mPopupMenu.getMenu());

            // listener to show popup
            mImageButtonOverflow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if ( sHeader.equals(sNoMatchesStored) /*|| sText.equals(sNoMatchesStored)*/ ) {
                        return;
                    }
                    mPopupMenu.show(); // v is the imagebutton
                }
            });

            // listener to menu items clicks of the popup
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
                    //TextView txtMatch = ViewUtil.getFirstView(v, TextView.class);
                    //String sText = txtMatch.getText().toString();
                    String msg = "Menu item '" + item.getTitle() + "' clicked for '" + sText + "' in '" + sHeader + "'";
                    Log.d(TAG, msg);
                    //Toast.makeText(PreviousMatchSelector.this, msg, Toast.LENGTH_SHORT).show();
                    File f = (File) getObject(sHeader, sText);
                    switch (item.getItemId()) {
                        case R.id.pmi_item_details:
                            matchDetails(f);
                            break;
                        case R.id.pmi_item_open:
                            selectMatch(f);
                            break;
                        case R.id.pmi_item_delete:
                            confirmDeleteMatch(f, sHeader, sText);
                            break;
                        case R.id.pmi_match_share:
                            shareScoreSheet(f);
                            break;
                        case R.id.pmi_item_email:
                            mailMatch(f);
                            break;
                        case R.id.pmi_item_clipboard:
                            clipboardMatch(f);
                            break;
                        case R.id.pmi_item_message:
                            messageMatch(f, null);
                            break;
                        case R.id.pmi_item_edit_event:
                            // TODO: edit event
                            editEventAndPlayers(f);
                            break;
                        case R.id.pmi_item_edit_date:
                            // TODO: edit
                            break;
                    }
                    return false;
                }
            });
        }

        @Override protected void doPostInflateGroup(final View v, final String sHeader) {
            ImageButton mImageButtonOverflow = (ImageButton) v.findViewById(R.id.card_header_button_overflow);
            if ( mImageButtonOverflow == null ) { return; }

            final PopupMenu mPopupMenu = new PopupMenu(context, mImageButtonOverflow);
            MenuInflater inflater = mPopupMenu.getMenuInflater();
            inflater.inflate(R.menu.previous_match_group_menu, mPopupMenu.getMenu());

            // listener to show popup
            mImageButtonOverflow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    mPopupMenu.show(); // v is the imagebutton
                }
            });

            // listener to menu items clicks of the popup
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    String msg = "Menu item '" + item.getTitle() + "' clicked for '" + sHeader + "'";
                    Log.d(TAG, msg);
                    //Toast.makeText(scoreBoard, msg, Toast.LENGTH_SHORT).show();
                    switch (item.getItemId()) {
                        case R.id.pmi_group_delete:
                            confirmDeleteHeader(sHeader);
                            //_deleteHeader(sHeader);
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private boolean editEventAndPlayers(File fMatchModel) {
        Model match = Brand.getModel();
        try {
            match.fromJsonString(fMatchModel);
            File fCheck = match.getStoreAs(getArchiveDir(context));
            if ( fCheck.getAbsoluteFile().equals(fMatchModel) == false ) {
                fMatchModel.delete();
                ScoreBoard.storeAsPrevious(context, match, true);
            }
        } catch (Exception e) {
            return false;
        }
        EditPlayers editPlayers = new EditPlayers(context, match, null);
        DialogManager.getInstance().show(editPlayers);
        //baseDialog = editPlayers;
        return true;
    }

/*
    public static int moveFileFromCacheDir_Temp(Context context) {
        List<File> lAllMatchFiles = new MultiSelectList<File>(ContentUtil.getFilesRecursive(context.getCacheDir(), ".*\\.sb", null));
        File fNewParent = getArchiveDir(context);
        List<File> lAllFromOldImportFiles = new MultiSelectList<File>(ContentUtil.getFilesRecursive(new File(fNewParent, "cache"), ".*\\.sb", null));
        if ( ListUtil.isNotEmpty(lAllFromOldImportFiles) ) {
            lAllMatchFiles.addAll(lAllFromOldImportFiles);
        }
        int iMoved = 0;
        for(File f: lAllMatchFiles) {
            if (  f.renameTo(new File(fNewParent, f.getName()))  ) {
                iMoved++;
            };
        }
        return iMoved;
    }
*/
    /** Returns map with [PlayerA-PlayerB]=[File] for recent matches */
    private static final String S_FORMAT_NAMES = "%s-%s";
    public static Map<String, File> getLastFewHoursMatchesAsMap(Context context, int iHoursBack) {
        Map<String, File> mReturn = new HashMap<>();

        List<File> lAllMatchFiles = PreviousMatchSelector.getLastFewHoursMatches(context, iHoursBack);
        if ( lAllMatchFiles == null ) { return mReturn; }

        for(File fStored: lAllMatchFiles ) {
            Model mTmp = Brand.getModel();
            try {
                if ( mTmp.fromJsonString(fStored) ) {
                    String sA = mTmp.getName(Player.A);
                    String sB = mTmp.getName(Player.B);
                    mReturn.put(getKeyFromNames(sA, sB), fStored);
                };
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mReturn;
    }

    public static String getKeyFromNames(String sA, String sB) {
        if ( StringUtil.areAllEmpty(sA, sB) ) { return null; }
        return String.format(S_FORMAT_NAMES, sA,sB);
    }

    public static List<File> getLastFewHoursMatches(Context context, int iHoursBack) {
        Date d3HoursAgo = DateUtil.getCurrent_addHours(-1 * iHoursBack, null, false);
        return getPreviousMatchFiles(context, d3HoursAgo);
    }
    public static List<File> getPreviousMatchFiles(Context context) {
        return getPreviousMatchFiles(context, null);
    }
    public static List<File> getPreviousMatchFiles(Context context, Date dNewerThan) {
        List<File> files = ContentUtil.getFilesRecursive(getArchiveDir(context), ".*\\.(sb|json)", null, dNewerThan);
        List<File> lAllMatchFiles = new MultiSelectList<File>(files);
        lAllMatchFiles.remove(ScoreBoard.getLastMatchFile(context)); // TODO: for multisports version remove possible multiple LAST.xxx.sb files
        lAllMatchFiles.remove(ColorPrefs.getFile(context));
        return lAllMatchFiles;
    }

    public static File getArchiveDir(Context context) {
        return context.getFilesDir();
    }

    public static void confirmDeleteAllPrevious(final Context context) {
        AlertDialog.Builder ab = ScoreBoard.getAlertDialogBuilder(context);
        ab.setIcon          (android.R.drawable.ic_menu_delete)
          .setTitle         (R.string.hms_delete_all )
          .setNegativeButton(R.string.cmd_cancel, null)
          .setPositiveButton(R.string.cmd_delete, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        List<File> lAllMatchFiles = PreviousMatchSelector.getPreviousMatchFiles(context);
                        if ( ListUtil.isNotEmpty(lAllMatchFiles) ) {
                            for ( File f : lAllMatchFiles ) {
                                f.delete();
                            }
                            if ( context instanceof MenuHandler) {
                                MenuHandler tabbed = (MenuHandler) context;
                                tabbed.handleMenuItem(R.id.refresh);
                                tabbed.handleMenuItem(R.id.close); // TODO: do not invoke close: for now we do this because refresh does not seem to do the job
                            }
                        }
                    }
                }).show();
    }

    private boolean shareScoreSheet(File fMatchModel) {
        Model match = Brand.getModel();
        try {
            String sJson = FileUtil.readFileAsString(fMatchModel);
            match.fromJsonString(sJson, false);
        } catch (Exception e) {
            return false;
        }
        ScoreBoard.shareScoreSheet(context, match, false);
        return true;
    }
    private boolean mailMatch(File fMatchModel) {
        Model match = Brand.getModel();
        try {
            String sJson = FileUtil.readFileAsString(fMatchModel);
            match.fromJsonString(sJson, false);
        } catch (Exception e) {
            return false;
        }
        ScoreBoard.emailMatchResult(context, match);
        return true;
    }
    private boolean messageMatch(File fMatchModel, String sPackage) {
        Model match = Brand.getModel();
        try {
            String sJson = FileUtil.readFileAsString(fMatchModel);
            match.fromJsonString(sJson, false);
        } catch (Exception e) {
            return false;
        }
        String defaultSMSTo = PreferenceValues.getDefaultSMSTo(context);
        ScoreBoard.shareMatchSummary(context, match, sPackage, defaultSMSTo);
        return true;
    }
    private boolean clipboardMatch(File fMatchModel) {
        Model match = Brand.getModel();
        try {
            String sJson = FileUtil.readFileAsString(fMatchModel);
            match.fromJsonString(sJson, false);
        } catch (Exception e) {
            return false;
        }
        ContentUtil.placeOnClipboard(context, "squore summary", ResultSender.getMatchSummary(activity, match));
        return true;
    }
}
