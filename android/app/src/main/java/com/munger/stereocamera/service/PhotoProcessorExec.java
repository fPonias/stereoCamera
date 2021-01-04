package com.munger.stereocamera.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class PhotoProcessorExec
{
    private PhotoFiles photoFiles;
    private Context context;
    private CompositeImageType type;
    private String workingDirectory = "/data/local/tmp";

    private static String targetExecutable = null;

    public PhotoProcessorExec(Context context, CompositeImageType type)
    {
        this.context = context;
        workingDirectory =  context.getFilesDir().getPath();
        this.type = type;

        //if (targetExecutable == null)
        //{
        //    targetExecutable = copyExecutables();
        //}

        photoFiles = PhotoFiles.Factory.get();
    }

    public static String[] abis = {
            "arm64-v8a", "armeabi-v7a", "x86", "x86_64"
    };

    @TargetApi(21)
    private String getSupportedArch21()
    {
        for (String abi : Build.SUPPORTED_ABIS)
        {
            for (String target : abis)
            {
                if (target.equals(abi))
                    return target;
            }
        }

        return null;
    }

    private String getSupportedArch()
    {
        String arch = System.getProperty("os.arch");

        if (arch.equals("x86_64"))
            return abis[3];
        else if (arch.contains("arch64"))
            return abis[0];
        else if (arch.startsWith("i"))
            return abis[2];
        else if (arch.contains("arm"))
            return abis[1];
        else
            return abis[1];
    }

    private String copyExecutables()
    {
        String abi = null;
        //if (Build.VERSION.SDK_INT >= 21)
        //{
        //   abi = getSupportedArch21();
        //}

        if (abi == null || Build.VERSION.SDK_INT < 21)
        {
            abi = getSupportedArch();
        }

        if (abi == null)
            return null;

        InputStream ins = null;
        OutputStream outs = null;
        File outFile = null;
        String ret = workingDirectory + "/stereoCameraImageProcessor";
        try
        {
            ins = context.getAssets().open("exec/" + abi + "/stereoCameraImageProcessor");
            outFile = new File(ret);
            outs = new FileOutputStream(outFile);
            byte[] buffer = new byte[4096];
            int read = 1;
            while (read > 0)
            {
                read = ins.read(buffer);
                if (read > 0)
                    outs.write(buffer, 0, read);
            }

            outs.flush();
        }
        catch(IOException e){
            return null;
        }
        finally
        {
            if (ins != null)
                try { ins.close(); } catch(IOException e){}
            if (outs != null)
                try { outs.close(); } catch(IOException e){}
        }

        try
        {
            runCommand(Arrays.asList("/system/bin/chmod", "u+x", "./stereoCameraImageProcessor"));
            long sz = outFile.length();
            boolean exec = outFile.canExecute();

            if (!exec)
                return null;
        }
        catch(Exception e){
            return null;
        }

        Log.d("stereoCamera", "processor copied and executable at " + outFile.getPath());
        return ret;
    }

    private Process exec(List<String> args) throws IOException {
            ProcessBuilder pb = new ProcessBuilder();
            Process proc = pb.directory(new File(workingDirectory + "/../lib"))
                    .command(args)
                    .redirectErrorStream(true)
                    .start();

            return proc;
    }

    private String runCommand(List<String> args) throws IOException, InterruptedException
    {
        //Process process = Runtime.getRuntime().exec(args, null, new File(workingDirectory));
        Process process = exec(args);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            output.append(buffer, 0, read);
        }
        reader.close();

        process.waitFor();

        String outs = output.toString();
        Log.d("stereoCamera", args.get(0) + " command output: " + outs);
        return outs;
    }

    public void testOldData()
    {
        Thread t = new Thread(new Runnable() { public void run()
        {
            PhotoProcessorService.PhotoArgument localData = new PhotoProcessorService.PhotoArgument();
            localData.jpegPath = "/storage/emulated/0/Download/left.jpg";
            localData.orientation = PhotoOrientation.DEG_90;
            localData.zoom = 1.5f;

            PhotoProcessorService.PhotoArgument remoteData = new PhotoProcessorService.PhotoArgument();
            remoteData.jpegPath = "/storage/emulated/0/Download/left.jpg";
            remoteData.orientation = PhotoOrientation.DEG_180;
            remoteData.zoom = 1.0f;

            Intent i = PhotoProcessorService.getIntent(localData, remoteData, false, PhotoProcessor.CompositeImageType.SPLIT);
            MainActivity.getInstance().startService(i);
        }});
        t.start();
    }

    public static void copy(File src, File dst) throws IOException
    {
        if  (src.getPath().equals(dst.getPath()))
            return;

        InputStream in = new FileInputStream(src);
        try
        {
            OutputStream out = new FileOutputStream(dst);
            try
            {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
            }
            catch(Exception e){

            }
            finally {
                out.close();
            }
        }
        catch(Exception e){

        }
        finally {
            in.close();
        }
    }

    private String leftPath;
    private PhotoOrientation leftOrientation;
    private float leftZoom;
    private String rightPath;
    private PhotoOrientation rightOrientaiton;
    private float rightZoom;

    public void setData(boolean isRight, String path, PhotoOrientation orientation, float zoom)
    {
        String zoomStr = "" + zoom;
        zoomStr = zoomStr.toLowerCase();
        int idx = zoomStr.indexOf('e');
        if (idx >= 0)
        {
            char sign = zoomStr.charAt(idx + 1);
            if (sign == '-')
                zoom = 1.0f;
            else
                zoom = 100.0f;
        }

        if (orientation == null)
            orientation = PhotoOrientation.DEG_0;


        if (isRight)
        {
            rightPath = path;
            rightOrientaiton = orientation;
            rightZoom = zoom;
        }
        else
        {
            leftPath = path;
            leftOrientation = orientation;
            leftZoom = zoom;
        }

        try
        {
            //File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (isRight)
            {
                File fl = new File(workingDirectory + "/right.jpg");
                copy(new File(path), fl);
                rightPath = fl.getPath();
            }
            else
            {
                File fl = new File(workingDirectory + "/left.jpg");
                copy(new File(path), fl);
                leftPath = fl.getPath();
            }
        }
        catch(IOException e){}
    }

    public String processData(boolean flip)
    {
        //if (targetExecutable == null)
        //    return null;

        try {
            File out = new File(workingDirectory + "/out.jpg");
            String typeArg = type.toString();
            runCommand(Arrays.asList("./stereoCameraImageProcessor", workingDirectory, type.toString(), leftPath, "" + leftOrientation.ordinal(), "" + leftZoom, rightPath, "" + rightOrientaiton.ordinal(), "" + rightZoom, out.getPath()));

            if (out.length() == 0)
                return null;

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File newOut = new File(dir.getPath() + "/out.jpg");

            copy(out, newOut);
            return newOut.getPath();
        }
        catch(Exception e){
            return null;
        }
    }

    //mirror enum of data type in CompositeImage.h
    public enum CompositeImageType
    {
        SPLIT,
        GREEN_MAGENTA,
        RED_CYAN
    };
}
