package com.munger.stereocamera.service;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.UUID;

public class PhotoProcessorWorker
{
    private final WorkManager workManager;

    public static class PhotoWorker extends Worker
    {
        private final PhotoProcessor proc;
        private final PhotoFiles files;

        public PhotoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
        {
            super(context, workerParams);
            files = PhotoFiles.Factory.get();
            proc = new PhotoProcessor(context);
        }

        @NonNull
        @Override
        public Result doWork()
        {
            Data data = getInputData();
            ImagePair args = new ImagePair(data);
            proc.setProcessorType(args.type);
            proc.setData(true, args.right);
            proc.setData(false, args.left);
            String path = proc.processData(args.flip);

            if (path == null)
                return Result.failure();

            Uri outUri = files.saveFile(new File(path));
            Data resData = new Data.Builder().putString("URI", outUri.toString()).build();

            Result ret = Result.success(resData);
            MainActivity.getInstance().onNewPhoto(outUri);
            return ret;
        }
    }

    public PhotoProcessorWorker(Context context)
    {
        workManager = WorkManager.getInstance(context);
    }

    public static abstract class RunListener
    {
        public LifecycleOwner lcOwner;
        public abstract void onResult(Uri uri);

        public RunListener(LifecycleOwner lcOwner)
        {
            this.lcOwner = lcOwner;
        }
    }

    public UUID run(ImagePair args)
    {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(PhotoWorker.class)
                .setInputData(args.toData())
                .build();

        workManager.beginUniqueWork("photoProc", ExistingWorkPolicy.APPEND, req).enqueue();

        return req.getId();
    }

    public void listen(UUID id, RunListener listener)
    {
        workManager.getWorkInfoByIdLiveData(id).observe(listener.lcOwner, workInfo ->
        {
            WorkInfo.State state = workInfo.getState();
            if (state != WorkInfo.State.SUCCEEDED)
                return;

            String uriStr = workInfo.getOutputData().getString("URI");
            Uri uri = Uri.parse(uriStr);
            listener.onResult(uri);
        });
    }
}
