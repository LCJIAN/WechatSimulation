package com.lcjian.wechatsimulation.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.Constants;
import com.lcjian.wechatsimulation.R;
import com.lcjian.wechatsimulation.RxBus;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.job.Job;
import com.lcjian.wechatsimulation.job.foreground.AddFriendBySearchMobileJob;
import com.lcjian.wechatsimulation.job.foreground.CreateGroupChatJob;
import com.lcjian.wechatsimulation.job.foreground.GetRoomQrCodeJob;
import com.lcjian.wechatsimulation.utils.FileUtils;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

public class JobService extends Service {

    private PowerManager.WakeLock mWakeLock;

    private BlockingQueue<Job> mJobsQueue;

    private AtomicBoolean mStopFlag;

    private Job mCurrentJob;

    private Thread mStarterThread;

    private Thread mStopperThread;

    private Thread mJobConsumerThread;

    private SmackClient mSmackClient;

    private boolean mStarted;

    private SmackClientCreatedListener mSmackClientCreatedListener;

    private LocalBinder mLocalBinder = new LocalBinder();

    public class LocalBinder extends Binder {

        public JobService getService() {
            return JobService.this;
        }

    }

    public void setSmackClientCreatedListener(SmackClientCreatedListener smackClientCreatedListener) {
        this.mSmackClientCreatedListener = smackClientCreatedListener;
    }

    public interface SmackClientCreatedListener {
        void onSmackClientCreated(SmackClient smackClient);
    }

    public SmackClient getSmackClient() {
        return mSmackClient;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    @Override
    public void onCreate() {
        Timber.d("onCreate");
        mJobsQueue = new LinkedBlockingQueue<>();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "JobServiceLock");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Timber.d("onStartCommand");
        if (intent != null && intent.getBooleanExtra("start", false) && !mStarted) {
            mStopFlag = new AtomicBoolean(false);
            mSmackClient = new SmackClient(intent.getStringExtra("username"), intent.getStringExtra("password"), mJobsQueue);
            mSmackClient.addStateChangeListener(new SmackClient.StateChangeListener() {
                @Override
                public void onStateChange(SmackClient.State state) {
                    if (state == SmackClient.State.CONNECT_FAILED
                            || state == SmackClient.State.ACCOUNT_CREATE_FAILED
                            || state == SmackClient.State.AUTHENTICATE_FAILED) {
                        startService(new Intent(JobService.this, JobService.class).putExtra("stop", true));
                    }
                }
            });
            if (mSmackClientCreatedListener != null) {
                mSmackClientCreatedListener.onSmackClientCreated(mSmackClient);
            }
            mJobConsumerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            Timber.d("%s is running", Thread.currentThread().getName());
                            if (mStopFlag.get()) {
                                break;
                            }
                            if (mCurrentJob != null && !mCurrentJob.isFinished() && !mCurrentJob.isCancelled() && !mCurrentJob.isError()) {
                                Thread.sleep(1000 * 10);
                                continue;
                            }
                            mCurrentJob = mJobsQueue.take();
                            final Job currentJob = mCurrentJob;
                            int minutes;
                            if (currentJob instanceof AddFriendBySearchMobileJob) {
                                minutes = 1;
                            } else if (currentJob instanceof CreateGroupChatJob) {
                                minutes = 4;
                            } else {
                                minutes = 3;
                            }
                            Observable.just(true).delay(minutes, TimeUnit.MINUTES).subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    currentJob.cancel();
                                }
                            });
                            currentJob.addJobListener(new Job.JobListener() {
                                @Override
                                public void onCancelled() {
                                    mSmackClient.sendMessage(new Gson().toJson(new Response(1, "Job was cancelled", currentJob.getJobData())));
                                    Timber.d("onCancelled");
                                }

                                @Override
                                public void onFinished() {
                                    if (currentJob instanceof GetRoomQrCodeJob) {
                                        currentJob.getJobData().roomNameQrImgStr = FileUtils.firstFileToBase64(currentJob.getJobData().roomNameQrLocalDirectory);
                                        FileUtils.deleteDir(currentJob.getJobData().roomNameQrLocalDirectory);
                                    }
                                    mSmackClient.sendMessage(new Gson().toJson(new Response(0, "Job was finished", currentJob.getJobData())));
                                    Timber.d("onFinished");
                                }

                                @Override
                                public void onError(Throwable t) {
                                    mSmackClient.sendMessage(new Gson().toJson(new Response(2, t.getMessage(), currentJob.getJobData())));
                                    Timber.e(t, t.getMessage());
                                    Timber.d("onError:%s", t.getMessage());
                                }
                            });
                            RxBus.getInstance().send(currentJob);
                            NotificationManagerCompat.from(JobService.this).notify((int) System.currentTimeMillis(),
                                    new NotificationCompat.Builder(JobService.this)
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                                            .setContentTitle(Constants.JOB.toUpperCase(Locale.getDefault()))
                                            .setContentText(currentJob.getClass().getSimpleName())
                                            .setContentInfo(currentJob.getJobData().toString())
                                            .setContentIntent(PendingIntent.getActivity(JobService.this, 0,
                                                    new Intent().setAction(Intent.ACTION_MAIN)
                                                            .addCategory(Intent.CATEGORY_HOME)
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                            .setClassName(currentJob.getComponentPackageName(), currentJob.getComponentClassName()), PendingIntent.FLAG_ONE_SHOT))
                                            .setTicker(String.format(Locale.getDefault(), "%s:%s", Constants.JOB, currentJob.getClass().getSimpleName()))
                                            .setAutoCancel(true)
                                            .build());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Timber.d("%s will stop", Thread.currentThread().getName());
                }
            }, "JobConsumerThread");
            mStarterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mSmackClient.start();
                    mJobConsumerThread.start();
                    Timber.d("%s will stop", Thread.currentThread().getName());
                }
            }, "StarterThread");
            mStopperThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mSmackClient.stop();
                    mStopFlag.getAndSet(true);
                    mStarterThread.interrupt();
                    mJobConsumerThread.interrupt();
                    stopSelf();
                    Timber.d("%s will stop", Thread.currentThread().getName());
                }
            }, "StopperThread");
            mStarterThread.start();
            mWakeLock.acquire();
            mStarted = true;
        }
        if (intent != null && intent.getBooleanExtra("stop", false) && mStarted) {
            mStopperThread.start();
            mWakeLock.release();
            mStarted = false;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy");
        super.onDestroy();
    }
}
