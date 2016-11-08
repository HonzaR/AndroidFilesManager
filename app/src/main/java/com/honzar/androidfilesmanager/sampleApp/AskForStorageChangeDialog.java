package com.honzar.androidfilesmanager.sampleApp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.honzar.androidfilesmanager.library.FilesManager;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

public class AskForStorageChangeDialog extends BaseDialogFragment {

    public static final String FRAGMENT_TAG = AskForStorageChangeDialog.class.getName();
    public static final String BUNDLE_STORAGE = AskForStorageChangeDialog.class.getName() + "STORAGE_ID";

    private LayoutInflater inflater;
    private int storageId;

    public static AskForStorageChangeDialog newInstance(int storageId) {
        Bundle b = new Bundle();
        b.putInt(BUNDLE_STORAGE, storageId);
        AskForStorageChangeDialog ret = new AskForStorageChangeDialog();
        ret.setArguments(b);
        return ret;
    }

    @Override
    protected BaseDialogFragment.Builder build(BaseDialogFragment.Builder b, Bundle savedInstanceState) {
        inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.ask_for_storage_change_dialog, null);

        Bundle bundle = getArguments();
        this.storageId = bundle.getInt(BUNDLE_STORAGE);

        b.setTitle(getString(R.string.dialog_ask_storage_change_title));
        ((TextView) root.findViewById(R.id.tv_msg)).setText(getString(R.string.dialog_ask_storage_change_msg).replace("ˆsˆ", storageId == FilesManager.EXTERNAL_STORAGE ? "external" : "internal"));

        b.setView(root);

        b.setPositiveButton(R.string.dialog_ask_storage_change_move, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IOnAskStorageMoveDialogListener)getActivity()).onAskStorageMove(storageId);
                dismiss();
            }
        });

        b.setNegativeButton(R.string.dialog_ask_storage_change_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IOnAskStorageMoveDialogListener)getActivity()).onAskStorageCancel(storageId);
                dismiss();
            }
        });

        return b;
    }

    public interface IOnAskStorageMoveDialogListener {
        void onAskStorageMove(int storageId);
        void onAskStorageCancel(int storageId);
    }
}
