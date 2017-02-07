
package com.ng.asynctask;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.bridge.Arguments;

import android.content.Intent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsyncJobModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private String LOG_TAG = "asyncjob";
    private static final String NETWORK_TYPE_UNMETERED = "UNMETERED";
    private static final String NETWORK_TYPE_NONE = "NONE";
    private static final String NETWORK_TYPE_ANY = "ANY";

    private final ReactApplicationContext reactContext;

    private List<JobInfo> mJobs;

    private JobScheduler jobScheduler;

    private boolean mInitialized = false;

    private Object lock1 = new Object();

    @Override
    public void initialize() {
        Log.d(LOG_TAG, "Initializing AsyncJob");
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) getReactApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
            mJobs = jobScheduler.getAllPendingJobs();
            mInitialized = true;
        }
        super.initialize();
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    public AsyncJobModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    private JobInfo createJobObject(int taskId,
                String taskName,
                int timeout,
                int period,
                boolean persist,
                boolean appActive,
                boolean allowInForeground,
                int networkType,
                boolean requiresCharging,
                boolean requiresDeviceIdle,
                boolean isOneTimeJob,
                String payLoad) {
        ComponentName componentName = new ComponentName(getReactApplicationContext(), AsyncJob.class.getName());
        PersistableBundle jobExtras = new PersistableBundle();
        JobInfo.Builder jobInfo = new JobInfo.Builder(taskId, componentName);

        if(!isOneTimeJob) {
            jobInfo.setPeriodic(period);
        }

        jobInfo.setRequiresDeviceIdle(requiresDeviceIdle);
        jobInfo.setRequiresCharging(requiresCharging);
        jobInfo.setPersisted(persist);
        jobInfo.setRequiredNetworkType(networkType);
        jobInfo.setOverrideDeadline(120000);

        jobExtras.putString("taskName", taskName);
        jobExtras.putInt("taskId", taskId);
        jobExtras.putInt("timeout", timeout);
        jobExtras.putInt("period", period);
        jobExtras.putInt("persist", persist ? 1 : 0);
        jobExtras.putInt("networkPrefrence", networkType);
        jobExtras.putInt("requiresCharging", requiresCharging ? 1 : 0);
        jobExtras.putInt("requiresDeviceIdle", requiresDeviceIdle ? 1 : 0);
        jobExtras.putBoolean("allowInForeground", allowInForeground);
        jobExtras.putBoolean("isOneTimeJob", isOneTimeJob);
        jobExtras.putString("payLoad", payLoad);


        jobInfo.setExtras(jobExtras);


        return jobInfo.build();
    }

    private synchronized void scheduleJob(int taskId,
                String taskName,
                int timeout,
                int period,
                boolean persist,
                boolean appActive,
                boolean allowInForeground,
                int networkType,
                boolean requiresCharging,
                boolean requiresDeviceIdle,
                boolean isOneTimeJob,
                String payLoad,
                Callback callback) {
        JobInfo newJob = this.createJobObject( taskId,
                 taskName,
                 timeout,
                 period,
                 persist,
                 appActive,
                 allowInForeground,
                 networkType,
                 requiresCharging,
                 requiresDeviceIdle,
                 isOneTimeJob,
                 payLoad);

        Log.v(LOG_TAG, String.format("Create Job instance for JobId: %s", newJob.getId()));


        for (JobInfo iJobInfo : mJobs) {
            if (iJobInfo.getId() == taskId) {
                mJobs.remove(iJobInfo);
            }
        }
        Log.v(LOG_TAG, "will Add in array");
        mJobs.add(newJob);

        Log.v(LOG_TAG, "Successfully Added in array");


        if (appActive && allowInForeground) {
            scheduleAJob(newJob, callback);
        }
    }

    @ReactMethod
    public void scheduleAsyncJob(
                int taskId,
                String taskName,
                int timeout,
                int period,
                boolean persist,
                boolean appActive,
                boolean allowInForeground,
                int networkType,
                boolean requiresCharging,
                boolean requiresDeviceIdle,
                boolean isOneTimeJob,
                String payLoad,
                Callback callback
              ) {

        Log.v(LOG_TAG, String.format("Scheduling Async Job: %s, JobId: %s, oneTimeJob: %s, allowInForeground: %s, timeout: %s, period: %s, network type: %s, requiresCharging: %s, requiresDeviceIdle: %s, payLoad: %s", taskName, taskId, isOneTimeJob, allowInForeground, timeout, period, networkType, requiresCharging, requiresDeviceIdle, String.valueOf(payLoad)));

        scheduleJob(taskId,
                    taskName,
                    timeout,
                    period,
                    persist,
                    appActive,
                    allowInForeground,
                    networkType,
                    requiresCharging,
                    requiresDeviceIdle,
                    isOneTimeJob,
                    payLoad,
                    callback);

    }

    @ReactMethod
    public void startNow(int taskId, String jobKey, int timeout, String payLoad) {
        Log.v(LOG_TAG, String.format("Starting One Time Job: %s, JobId: %s, timeout: %s, payLoad: %s", jobKey, taskId, timeout, String.valueOf(payLoad)));
        Intent service = new Intent(reactContext, HeadlessService.class);

        Bundle bundle = new Bundle();

        bundle.putString("taskName", jobKey);
        bundle.putInt("timeout", timeout);
        bundle.putInt("taskId", taskId);
        bundle.putString("payLoad", payLoad);

        service.putExtras(bundle);
        reactContext.startService(service);
    }

    @ReactMethod
    public synchronized void hardCancel(int taskId, Callback callback ) {
        mJobs = jobScheduler.getAllPendingJobs();
        //-1(TaskId not Found) , 0(TaskId found, jobKey doesnot match), 1(TaskId found, jobKey matched)
        boolean result = false;
        for (JobInfo iJobInfo : mJobs) {
            if (iJobInfo.getId() == taskId) {
                String taskName = iJobInfo.getExtras().containsKey("taskName") ? iJobInfo.getExtras().getString("taskName") : null;

                Log.d(LOG_TAG, "Hard Cancelling job: " + String.valueOf(taskName) + " (" + taskId + ")");
                jobScheduler.cancel(taskId);
                mJobs = jobScheduler.getAllPendingJobs();
                result = true;
            }
        }
        Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " does not exist");

        callback.invoke(result);
    }

    @ReactMethod
    public synchronized void cancel(String taskName, int taskId, Callback callback ) {
        mJobs = jobScheduler.getAllPendingJobs();
        //-1(TaskId not Found) , 0(TaskId found, jobKey doesnot match), 1(TaskId found, jobKey matched)
        int isValid = -1;
        JobInfo iJobInfo = null;
        boolean result = false;
        for (JobInfo jobInfo : mJobs) {
            if (jobInfo.getId() == taskId) {
                PersistableBundle extras = jobInfo.getExtras();
                if(extras.containsKey("taskName") && taskName.equals(extras.getString("taskName")) ) {
                    isValid = 1;
                }else {
                    isValid = 0;
                }
                iJobInfo = jobInfo;
                break;
            }
        }

        if(isValid == 1) {
            Log.d(LOG_TAG, "Cancelling job: " + taskName + " (" + taskId + ")");
            jobScheduler.cancel(taskId);
            mJobs = jobScheduler.getAllPendingJobs();
            result = true;
        }else if(isValid == 0){
            String storedTaskName = iJobInfo != null && iJobInfo.getExtras().containsKey("taskName") ? iJobInfo.getExtras().getString("taskName") : null;
            Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " existes but taskName(" + String.valueOf(storedTaskName) + ") doesnot match to provided taskName(" + taskName + ")");
        }else {
            Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " does not exist");
        }
        callback.invoke(result);
    }

    @ReactMethod
    public synchronized void cancelAll() {
        Log.d(LOG_TAG, "Cancelling all jobs");
        jobScheduler.cancelAll();
        mJobs = jobScheduler.getAllPendingJobs();
    }

    private WritableArray _getAll() {
        Log.d(LOG_TAG, "Getting all jobs");
        WritableArray jobs = Arguments.createArray();
        if (mJobs != null) {
            for (JobInfo job : mJobs) {
                Log.d(LOG_TAG, "Fetching job " + job.getId());
                Bundle extras = new Bundle(job.getExtras());
                WritableMap jobMap = Arguments.fromBundle(extras);
                jobMap.putString("taskName", extras.getString("taskName"));
                jobMap.putBoolean("persist", extras.getInt("persist") == 1);
                jobMap.putBoolean("requiresCharging", extras.getInt("requiresCharging") == 1);
                jobMap.putBoolean("requiresDeviceIdle", extras.getInt("requiresDeviceIdle") == 1);
                jobs.pushMap(jobMap);
            }
        }

        return jobs;
    }

    @ReactMethod
    public void getAll(Callback callback) {
        WritableArray jobs = _getAll();
        callback.invoke(jobs);
    }

    @Override
    public String getName() {
        return "AsyncJob";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        Log.d(LOG_TAG, "Getting constants");
        jobScheduler = (JobScheduler) getReactApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            // mJobs = jobScheduler.getAllPendingJobs();
            mInitialized = true;
        }
        HashMap<String, Object> constants = new HashMap<>();
        constants.put("jobs", _getAll());
        constants.put(NETWORK_TYPE_UNMETERED, JobInfo.NETWORK_TYPE_UNMETERED);
        constants.put(NETWORK_TYPE_ANY, JobInfo.NETWORK_TYPE_ANY);
        constants.put(NETWORK_TYPE_NONE, JobInfo.NETWORK_TYPE_NONE);
        return constants;
    }

    @Override
    public void onHostResume() {
        Log.d(LOG_TAG, "Woke up");
        mJobs = jobScheduler.getAllPendingJobs();
        jobScheduler.cancelAll();
    }

    private void scheduleAJob(JobInfo job, Callback callback) {
        jobScheduler.cancel(job.getId());
        int result = jobScheduler.schedule(job);
        if (result == JobScheduler.RESULT_SUCCESS) {
           callback.invoke(true);
           return;
        }
        callback.invoke(false);
    }

    private void scheduleJobs() {
        for (JobInfo job : mJobs) {
            Log.d(LOG_TAG, "Sceduling job " + job.getId());
            jobScheduler.cancel(job.getId());
            int result = jobScheduler.schedule(job);
            if (result == JobScheduler.RESULT_SUCCESS)
                Log.d(LOG_TAG, "Job (" + job.getId() + ") scheduled successfully!");
        }
    }

    @Override
    public void onHostPause() {
        Log.d(LOG_TAG, "Pausing");
        scheduleJobs();
    }

    @Override
    public void onHostDestroy() {
        Log.d(LOG_TAG, "Destroyed");
        mJobs = jobScheduler.getAllPendingJobs();
        jobScheduler.cancelAll();
    }
}
