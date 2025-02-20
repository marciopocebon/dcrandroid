/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AddAccountActivity extends BaseActivity {

    private ProgressDialog pd;
    private PreferenceUtil util;
    private final int PASSCODE_REQUEST_CODE = 2;
    private EditText accountName;
    private EditText passphrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.add_account));
        setContentView(R.layout.add_account_activity);

        accountName = findViewById(R.id.add_acc_name);
        passphrase = findViewById(R.id.add_acc_passphrase);

        util = new PreferenceUtil(this);
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
            passphrase.setVisibility(View.GONE);
        }

        pd = Utils.getProgressDialog(this, false, false, getString(R.string.creating_account));
        findViewById(R.id.add_acc_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String privatePassphrase = passphrase.getText().toString();
                final String name = accountName.getText().toString().trim();
                if (name.equals("")) {
                    accountName.setError(getString(R.string.input_account_name));
                } else {
                    if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
                        if (privatePassphrase.equals("")) {
                            passphrase.setError(getString(R.string.input_private_phrase));
                            return;
                        }
                        createAccount(name, privatePassphrase.getBytes());
                    } else {
                        Intent enterPinIntent = new Intent(AddAccountActivity.this, EnterPassCode.class);
                        startActivityForResult(enterPinIntent, PASSCODE_REQUEST_CODE);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PASSCODE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final String passcode = data.getStringExtra(Constants.PASSPHRASE);
                createAccount(accountName.getText().toString().trim(), passcode.getBytes());
            }
        }
    }

    private void createAccount(final String name, final byte[] privatePassphrase) {
        pd.show();
        new Thread() {
            public void run() {
                try {
                    WalletData.getInstance().wallet.nextAccount(name, privatePassphrase);
                    setResult(RESULT_OK);
                    finish();
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showMessage(AddAccountActivity.this, Utils.translateError(AddAccountActivity.this, e), Toast.LENGTH_LONG);
                        }
                    });
                    setResult(RESULT_CANCELED);
                }
                if (pd.isShowing()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                        }
                    });
                }
            }
        }.start();
    }
}
