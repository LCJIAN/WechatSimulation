package com.lcjian.wechatsimulation.ui;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.Constants;
import com.lcjian.wechatsimulation.Manager;
import com.lcjian.wechatsimulation.R;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.ClientInfo;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.entity.Tuple;
import com.lcjian.wechatsimulation.service.JobService;
import com.lcjian.wechatsimulation.service.SimulationService;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.PackageUtils2;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.umeng.analytics.MobclickAgent;

import org.jivesoftware.smack.util.MD5;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import github.nisrulz.easydeviceinfo.base.EasyDeviceMod;
import github.nisrulz.easydeviceinfo.base.EasyNetworkMod;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.et_account)
    EditText et_account;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.btn_connect)
    Button btn_connect;

    @BindView(R.id.tv_we_chat_version)
    TextView tv_we_chat_version;
    @BindView(R.id.tv_app_version)
    TextView tv_app_version;
    @BindView(R.id.tv_root_permission)
    TextView tv_root_permission;
    @BindView(R.id.tv_accessibility)
    TextView tv_accessibility;
    @BindView(R.id.tv_go_accessibility_settings)
    TextView tv_go_accessibility_settings;
    @BindView(R.id.tv_ipv4)
    TextView tv_ipv4;

    private Subscription mSubscription;

    private Subscription mSubscriptionAnother;

    private CompositeSubscription mDelaySubscriptions;

    private Subject<String, String> mSubject = PublishSubject.create();

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SmackClient smackClient = ((JobService.LocalBinder) service).getService().getSmackClient();
            if (smackClient != null) {
                initUI(smackClient);
            } else {
                mSubject.onNext(getSharedPreferences("user_info", MODE_PRIVATE).getString("username", ""));
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

        progressBar.setVisibility(View.GONE);
        btn_connect.setOnClickListener(this);
        tv_go_accessibility_settings.setOnClickListener(this);

        bindService(new Intent(MainActivity.this, JobService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        Observable.defer(
                new Func0<Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call() {
                        String packageName = getPackageName();
                        PackageManager pm = getPackageManager();
                        int permissionGranted = pm.checkPermission(Constants.PMS_WRITE_SECURE_SETTINGS, packageName);
                        if (PackageManager.PERMISSION_DENIED == permissionGranted) {
                            return Observable.just(ShellUtils.execCommand("pm grant " + packageName + " " + Constants.PMS_WRITE_SECURE_SETTINGS, true).result == 0);
                        } else {
                            return Observable.just(true);
                        }
                    }
                }).subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            ContentResolver contentResolver = getContentResolver();
                            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, getPackageName() + "/" + SimulationService.class.getName());
                            Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable);
                    }
                });

//        Observable.defer(new Func0<Observable<Rect>>() {
//            @Override
//            public Observable<Rect> call() {
//                Tess tess = new Tess();
//                Rect rect = tess.find(new File(Environment.getExternalStorageDirectory(), "Screenshot.png"), "投诉");
//                tess.end();
//                return Observable.just(rect);
//            }
//        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Rect>() {
//            @Override
//            public void call(Rect rect) {
//                if (rect != null) {
//                    Timber.d(rect.toString());
//                }
//            }
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDelaySubscriptions = new CompositeSubscription();

        Observable<ClientInfo> delayObservable = Observable.just(true)
                .delay(5, TimeUnit.SECONDS)
                .observeOn(Schedulers.newThread())
                .map(new Func1<Boolean, ClientInfo>() {
                    @Override
                    public ClientInfo call(Boolean aBoolean) {
                        ClientInfo clientInfo = new ClientInfo();
                        clientInfo.weChatVersionName = PackageUtils2.getVersionName(MainActivity.this, "com.tencent.mm");
                        clientInfo.appVersionName = PackageUtils2.getVersionName(MainActivity.this);
                        clientInfo.haveRootPermission = ShellUtils.checkRootPermission();
                        clientInfo.accessibilityOpened = SimulationService.isRunning();
                        clientInfo.ipv4 = new EasyNetworkMod(MainActivity.this).getIPv4Address();
                        return clientInfo;
                    }
                }).cache();
        mDelaySubscriptions.add(delayObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ClientInfo>() {
                    @Override
                    public void call(ClientInfo clientInfo) {
                        tv_we_chat_version.setText(TextUtils.isEmpty(clientInfo.weChatVersionName) ? getString(R.string.not_installed) : clientInfo.weChatVersionName);
                        tv_app_version.setText(clientInfo.appVersionName);
                        tv_root_permission.setText(clientInfo.haveRootPermission ? R.string.have_root_permission : R.string.no_root_permission);
                        tv_accessibility.setText(clientInfo.accessibilityOpened ? R.string.accessibility_opened : R.string.accessibility_closed);
                        tv_ipv4.setText(clientInfo.ipv4);
                    }
                }));
        mDelaySubscriptions.add(Observable.zip(delayObservable,
                mSubject.observeOn(Schedulers.newThread()).map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        if (TextUtils.isEmpty(s)) {
                            HashMap<String, String> params = new HashMap<>();
                            EasyDeviceMod easyDeviceMod = new EasyDeviceMod(MainActivity.this);
                            String deviceName = easyDeviceMod.getDevice();
                            String imei = easyDeviceMod.getIMEI();
                            String phoneNo = easyDeviceMod.getPhoneNo();
                            String mac = new EasyNetworkMod(MainActivity.this).getWifiMAC();
                            params.put("devicename", deviceName);
                            params.put("imei", imei);
                            params.put("sign", MD5.hex(deviceName + imei + "WEIju2016888xx3"));
                            params.put("phone", phoneNo);
                            params.put("mac", mac);
                            return new Gson().fromJson(DownloadUtils.post("http://114.215.140.3:8089/LineStoken/api/getnumber", params), Response.class).data.userName;
                        } else {
                            return s;
                        }
                    }
                }), new Func2<ClientInfo, String, Tuple<ClientInfo, String>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Tuple<ClientInfo, String> call(ClientInfo clientInfo, String s) {
                        return new Tuple(clientInfo, s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Tuple<ClientInfo, String>>() {
                    @Override
                    public void call(Tuple<ClientInfo, String> clientInfoStringTuple) {
                        String username = clientInfoStringTuple.y;
                        if (!TextUtils.isEmpty(username) && clientInfoStringTuple.x.haveRootPermission && clientInfoStringTuple.x.accessibilityOpened) {
                            et_account.setText(username);
                            btn_connect.performClick();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable);
                    }
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onStop() {
        if (mDelaySubscriptions != null) {
            mDelaySubscriptions.unsubscribe();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        if (mSubscriptionAnother != null) {
            mSubscriptionAnother.unsubscribe();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect: {
                if (btn_connect.getText().toString().equals(getString(R.string.connect))) {
                    if (mSubscriptionAnother != null) {
                        mSubscriptionAnother.unsubscribe();
                    }
                    mSubscriptionAnother = Observable.just(true)
                            .map(new Func1<Boolean, ClientInfo>() {
                                @Override
                                public ClientInfo call(Boolean aBoolean) {
                                    ClientInfo clientInfo = new ClientInfo();
                                    clientInfo.haveRootPermission = ShellUtils.checkRootPermission();
                                    clientInfo.accessibilityOpened = SimulationService.isRunning();
                                    return clientInfo;
                                }
                            }).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<ClientInfo>() {
                                @Override
                                public void call(ClientInfo clientInfo) {
                                    if (TextUtils.isEmpty(et_account.getEditableText().toString())) {
                                        Toast.makeText(MainActivity.this, R.string.empty_account_msg, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!clientInfo.accessibilityOpened) {
                                        Toast.makeText(MainActivity.this, R.string.accessibility_closed_msg, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (!clientInfo.haveRootPermission) {
                                        Toast.makeText(MainActivity.this, R.string.no_root_permission_msg, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    startService(new Intent(MainActivity.this, JobService.class)
                                            .putExtra("start", true)
                                            .putExtra("username", et_account.getEditableText().toString())
                                            .putExtra("password", "123456"));
                                    getSharedPreferences("user_info", MODE_PRIVATE).edit().putString("username", et_account.getEditableText().toString()).apply();
                                }
                            });
                } else if ((btn_connect.getText().toString().equals(getString(R.string.disconnect)))) {
                    startService(new Intent(this, JobService.class).putExtra("stop", true));
                }
            }
            break;
            case R.id.tv_go_accessibility_settings: {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
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
            case AUTHENTICATING:
                stateText = "AUTHENTICATING";
                progressBar.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(false);
                break;
            case AUTHENTICATED:
                stateText = "AUTHENTICATED";
                delayEnabled(R.string.disconnect);
                break;
            case AUTHENTICATE_FAILED:
                stateText = "AUTHENTICATE_FAILED";
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
        if (smackClient.getState() == SmackClient.State.AUTHENTICATED) {
            if (Manager.getUpdateVersionCode() != 0) {
                PackageManager manager = getPackageManager();
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
