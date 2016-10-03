package com.lcjian.wechatsimulation.tinker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.lcjian.wechatsimulation.APP;
import com.tencent.tinker.lib.service.DefaultTinkerResultService;
import com.tencent.tinker.lib.service.PatchResult;

public class SampleResultService extends DefaultTinkerResultService {

    @Override
    public void onPatchResult(PatchResult result) {
        if (result != null && result.isSuccess && result.isUpgradePatch) {
            restartProcess();
        }
        super.onPatchResult(result);
    }

    /**
     * you can restart your process through service or broadcast
     */
    private void restartProcess() {
        PendingIntent restartIntent = PendingIntent.getActivity(
                APP.getInstance(),
                0,
                getBaseContext().getPackageManager().getLaunchIntentForPackage(
                        getBaseContext().getPackageName()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager mgr = (AlarmManager) APP.getInstance().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, restartIntent);
    }
}
