package com.munger.stereocamera.fragment;

import android.Manifest;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.munger.stereocamera.MainActivity;import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.service.PhotoProcessorExec;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.ArrayList;

public class TestFragment
{
    private ConnectFragment parent;
    private ViewGroup root;
    private Button btn;
    private ImageView img;

    public TestFragment(ConnectFragment parent, ViewGroup view)
    {
        this.parent = parent;
        root = view;
        btn = view.findViewById(R.id.testButton);
        img = view.findViewById(R.id.testPreview);

        init();
    }

    private void init()
    {
        ArrayList<PhotoFile> files = PhotoFiles.Factory.get().getAllFiles();

        btn.setOnClickListener(btn -> {
            runTest();
        });
    }

    private void runTest()
    {
        Thread t = new Thread(() -> {
            //String out = testCombine();
            //String out = testLibCombine();
            Uri out = testFileSave();
        });
        t.start();

        MainActivity.getInstance().requestPermissionForResult(Manifest.permission.WRITE_EXTERNAL_STORAGE, result -> {

        });


    }

    private Uri testFileSave()
    {
        PhotoFiles files = PhotoFiles.Factory.get();
        String tmpLeft = files.copyAssetToCache("left.jpg");
        Uri newuri = files.saveFile(new File(tmpLeft));

        MainActivity.getInstance().runOnUiThread(() -> {
            img.setImageURI(newuri);
        });

        return newuri;
    }

    private String procOutput;

    private String testLibCombine()
    {
        final Object lock = new Object();
        procOutput = null;

        Thread t = new Thread(new Runnable() { public void run()
        {
            PhotoFiles files = PhotoFiles.Factory.get();
            String tmpLeft = files.copyAssetToCache("left.jpg");
            String tmpRight = files.copyAssetToCache("right.jpg");
            PhotoProcessor proc = new PhotoProcessor(parent.getContext(), PhotoProcessor.CompositeImageType.GREEN_MAGENTA);
            proc.setData(true, tmpRight, PhotoOrientation.DEG_0, 1.0f);
            proc.setData(false, tmpLeft, PhotoOrientation.DEG_0, 1.0f);

            procOutput = proc.processData(false);

            synchronized (lock)
            {
                lock.notify();
            }
        }});
        t.start();

        synchronized (lock)
        {
            if (procOutput == null)
                try {lock.wait();} catch(InterruptedException e) {}
        }

        return procOutput;
    }

    private String testCombine()
    {
        PhotoProcessorExec proc = new PhotoProcessorExec(MainActivity.getInstance(), PhotoProcessorExec.CompositeImageType.GREEN_MAGENTA);
        boolean onRight = true;
        boolean isFacing = false;
        PhotoFiles files = PhotoFiles.Factory.get();
        String tmpLeft = files.copyAssetToCache("left.jpg");
        String tmpRight = files.copyAssetToCache("right.jpg");

        if (tmpLeft == null || tmpRight == null)
        {
            Log.d("stereoCamera", "failed to copy test files");
            return null;
        }

        proc.setData(onRight, tmpRight, PhotoOrientation.DEG_0, 1.0f);
        proc.setData(!onRight, tmpLeft, PhotoOrientation.DEG_0, 1.0f);

        String out = proc.processData(isFacing);

        (new File(tmpLeft)).delete();
        (new File(tmpRight)).delete();

        return out;
    }
}
