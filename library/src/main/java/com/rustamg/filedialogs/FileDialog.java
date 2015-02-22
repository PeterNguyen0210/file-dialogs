package com.rustamg.filedialogs;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.rustamg.filedialogs.utils.KeyboardUtils;

import java.io.File;
import java.io.FileFilter;


/**
 * Created at 31/01/15 12:27
 *
 * @author rustamg
 */
public abstract class FileDialog extends DialogFragment implements FileListAdapter.OnFileSelectedListener {

    public static final String ROOT_DIRECTORY = "root_directory";
    public static final String START_DIRECTORY = "start_directory";
    public static final String EXTENSION = "extension";

    private static final String OUT_STATE_CURRENT_DIRECTORY = "out_state_current_dir";
    private static final String EXTERNAL_ROOT_PATH = Environment.getExternalStorageDirectory().getPath();

    protected File mCurrentDir;
    protected File mRootDir;
    protected FileFilter mFilesFilter;

    protected Toolbar mToolbar;
    protected ProgressBar mProgress;
    protected RecyclerView mRecyclerView;

    private UpdateFilesTask mUpdateFilesTask;
    protected String mExtension;

    private String mRootPathDisplayName; // todo: create an argument for this

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        //        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return inflater.inflate(getLayoutResourceId(), container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv_files);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mCurrentDir.getPath().equalsIgnoreCase(EXTERNAL_ROOT_PATH)) {
                    dismiss();
                }
                else {
                    mCurrentDir = mCurrentDir.getParentFile();
                    refresh();
                }
            }
        });

        extractArguments(savedInstanceState);

        initList();
    }

    protected void extractArguments(Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        mRootDir = arguments != null ? (File) arguments.getSerializable(ROOT_DIRECTORY) : null;

        if (savedInstanceState != null) {
            mCurrentDir = (File) savedInstanceState.getSerializable(OUT_STATE_CURRENT_DIRECTORY);
        }
        else {
            mCurrentDir = (File) (arguments != null ? arguments.getSerializable(START_DIRECTORY) : null);
        }

        if (mCurrentDir == null) {
            mCurrentDir = new File(EXTERNAL_ROOT_PATH);
        }

        if (mRootDir == null) {
            mRootDir = mCurrentDir;
        }

        if (arguments != null) {
            mExtension = arguments.getString(EXTENSION);
            mFilesFilter = new ExtensionFilter(mExtension);
        }
    }

    private void initList() {

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onStart() {

        super.onStart();

        refresh();
    }

    public void refresh() {

        if (mUpdateFilesTask == null || mUpdateFilesTask.getStatus() == AsyncTask.Status.FINISHED) {

            mUpdateFilesTask = new UpdateFilesTask();
            mUpdateFilesTask.execute(mCurrentDir);
        }
    }

    @Override
    public void onStop() {

        super.onStop();
        mUpdateFilesTask.cancel(false);
    }

    @Override
    public void onFileSelected(File file) {

        if (file.isDirectory()) {
            mCurrentDir = file;
            refresh();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putSerializable(OUT_STATE_CURRENT_DIRECTORY, mCurrentDir);
    }

    protected void sendResult(File file) {

        Fragment targetFragment = getParentFragment();

        if (targetFragment != null && targetFragment instanceof OnFileSelectedListener) {
            ((OnFileSelectedListener) targetFragment).onFileSelected(this, file);
        }
        else {
            Activity activity = getActivity();
            if (activity != null && activity instanceof OnFileSelectedListener) {
                ((OnFileSelectedListener) activity).onFileSelected(this, file);
            }
        }

        dismiss();
    }

    protected abstract int getLayoutResourceId();

    private class UpdateFilesTask extends AsyncTask<File, Void, File[]> {

        private File[] mFileArray;
        private File mDirectory;

        private UpdateFilesTask() {

        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected File[] doInBackground(File... files) {

            mDirectory = files[0];
            mFileArray = files[0].listFiles(mFilesFilter);

            return mFileArray;
        }

        @Override
        protected void onPostExecute(File[] localFiles) {

            super.onPostExecute(localFiles);

            if (!isCancelled() && getActivity() != null) {

                if (mDirectory.getPath().equalsIgnoreCase(EXTERNAL_ROOT_PATH)) {

                    mToolbar.setTitle(mRootPathDisplayName);
                    mToolbar.setNavigationIcon(R.drawable.ic_cross);
                }
                else {
                    mToolbar.setTitle(mCurrentDir.getName());
                    mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                }

                mRecyclerView.setAdapter(new FileListAdapter(getActivity(), localFiles, FileDialog.this));

                mProgress.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {

        if (getActivity() != null) {
            KeyboardUtils.hideKeyboard(getActivity());
        }
        super.onDismiss(dialog);
    }

    public interface OnFileSelectedListener {

        void onFileSelected(FileDialog dialog, File file);
    }
}
