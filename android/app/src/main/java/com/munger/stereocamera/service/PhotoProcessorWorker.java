package com.munger.stereocamera.service;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

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
            proc.preProcess(args.flip);
            String path = proc.processData();

            boolean clean = data.getBoolean("CLEAN", true);
            if (clean)
                proc.clean();

            if (path == null)
                return Result.success(); //don't ever return a failure.

            PhotoFile result = MainActivity.getInstance().getFileSystemViewModel().saveFile(new File(path));
            Data resData = new Data.Builder()
                    .putString("URI", result.uri.toString())
                    .putInt("ID", result.id)
                    .build();


            Result ret = Result.success(resData);
            return ret;
        }


    }

    public PhotoProcessorWorker(Context context)
    {
        workManager = WorkManager.getInstance(context);
        workManager.pruneWork();  //prune residual processes that might mess things up.
    }

    public static abstract class RunListener
    {
        public LifecycleOwner lcOwner;
        public abstract void onResult(Uri uri, int id);

        public RunListener(LifecycleOwner lcOwner)
        {
            this.lcOwner = lcOwner;
        }
    }

    public UUID run(ImagePair args)
    {
        return run(args, true);
    }

    public UUID run(ImagePair args, boolean cleanPreProcessor)
    {
        Data.Builder builder = new Data.Builder();
        args.toData(builder);
        builder.putBoolean("CLEAN", cleanPreProcessor);

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(PhotoWorker.class)
                .setInputData(builder.build())
                .addTag("photoProc")
                .build();

        WorkContinuation queue = workManager.beginUniqueWork("photoProc", ExistingWorkPolicy.APPEND, req);
        queue.enqueue();

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
            int fid = workInfo.getOutputData().getInt("ID", -1);
            listener.onResult(uri, fid);
        });
    }
}
