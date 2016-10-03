package com.lcjian.wechatsimulation.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.Manager;
import com.lcjian.wechatsimulation.R;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.service.JobService;
import com.lcjian.wechatsimulation.utils.ShellUtils;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.et_account)
    EditText et_account;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.btn_connect)
    Button btn_connect;

    private Subscription mSubscription;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SmackClient smackClient = ((JobService.LocalBinder) service).getService().getSmackClient();
            if (smackClient != null) {
                initUI(smackClient);
            } else {
                String username = getSharedPreferences("user_info", MODE_PRIVATE).getString("username", "");
                if (!TextUtils.isEmpty(username)) {
                    et_account.setText(username);
                    btn_connect.performClick();
                }
            }
            ((JobService.LocalBinder) service).getService().setSmackClientCreatedListener(new JobService.SmackClientCreatedListener() {
                @Override
                public void onSmackClientCreated(SmackClient smackClient) {
                    initUI(smackClient);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ShellUtils.execCommand("echo get_root", true);
        progressBar.setVisibility(View.GONE);
        btn_connect.setOnClickListener(this);

        bindService(new Intent(MainActivity.this, JobService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect: {
                if (btn_connect.getText().toString().equals(getString(R.string.connect))) {
                    if (!TextUtils.isEmpty(et_account.getEditableText().toString())) {
                        startService(new Intent(this, JobService.class)
                                .putExtra("start", true)
                                .putExtra("username", et_account.getEditableText().toString())
                                .putExtra("password", "123456"));
                        getSharedPreferences("user_info", MODE_PRIVATE).edit().putString("username", et_account.getEditableText().toString()).apply();
                    }
                } else if ((btn_connect.getText().toString().equals(getString(R.string.disconnect)))) {
                    startService(new Intent(this, JobService.class).putExtra("stop", true));
                }
            }
            break;
            default:
                break;
        }
    }

    private void initUI(SmackClient smackClient) {
        et_account.setText(smackClient.getUsername());
        updateState(smackClient.getState());
        checkUpdate(smackClient);
        smackClient.addStateChangeListener(new SmackClient.StateChangeListener() {
            @Override
            public void onStateChange(final SmackClient.State state) {
                btn_connect.post(new Runnable() {
                    @Override
                    public void run() {
                        updateState(state);
                    }
                });
            }
        });
    }

    private void updateState(SmackClient.State state) {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        String stateText;
        switch (state) {
            case CONNECTING:
                stateText = "CONNECTING";
                progressBar.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(false);
                break;
            case CONNECTED:
                stateText = "CONNECTED";
                break;
            case CONNECT_FAILED:
                stateText = "CONNECT_FAILED";
                delayEnabled(R.string.connect);
                break;
            case RECONNECTING:
                stateText = "RECONNECTING";
                progressBar.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(false);
                break;
            case ACCOUNT_CREATING:
                stateText = "ACCOUNT_CREATING";
                break;
            case ACCOUNT_CREATED:
                stateText = "ACCOUNT_CREATED";
                break;
            case ACCOUNT_CREATE_FAILED:
                stateText = "ACCOUNT_CREATE_FAILED";
                delayEnabled(R.string.connect);
                break;
            case LOGIN_ING:
                stateText = "LOGIN_ING";
                progressBar.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(false);
                break;
            case LOGIN_ED:
                stateText = "LOGIN_ED";
                delayEnabled(R.string.disconnect);
                break;
            case LOGIN_FAILED:
                stateText = "LOGIN_FAILED";
                delayEnabled(R.string.connect);
                break;
            case DISCONNECTING:
                stateText = "DISCONNECTING";
                progressBar.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(false);
                break;
            case DISCONNECTED:
                stateText = "DISCONNECTED";
                delayEnabled(R.string.connect);
                break;
            default:
                stateText = "DISCONNECTED";
                delayEnabled(R.string.connect);
                break;
        }
        btn_connect.setText(stateText);
    }

    private void delayEnabled(final int resId) {
        mSubscription = Observable.just(true)
                .delay(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        progressBar.setVisibility(View.GONE);
                        btn_connect.setText(resId);
                        btn_connect.setEnabled(aBoolean);
                    }
                });
    }

    private void checkUpdate(SmackClient smackClient) {
        if (smackClient.getState() == SmackClient.State.LOGIN_ED) {
            if (Manager.getUpdateVersionCode() != 0) {
                PackageManager manager = this.getPackageManager();
                try {
                    PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
                    if (Manager.getUpdateVersionCode() == info.versionCode) {
                        smackClient.sendMessage(new Gson().toJson(new Response(0, "Job was finished", new Gson().fromJson(Manager.getJobData(), JobData.class))));
                    } else {
                        smackClient.sendMessage(new Gson().toJson(new Response(2, "Update failed", new Gson().fromJson(Manager.getJobData(), JobData.class))));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                Manager.removeUpdateVersionCode();
            }
        }
    }
}
