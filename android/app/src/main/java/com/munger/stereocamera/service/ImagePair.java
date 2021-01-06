package com.munger.stereocamera.service;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.work.Data;

import com.munger.stereocamera.ip.command.PhotoOrientation;

public class ImagePair
{
    public static class ImageArg implements Parcelable
    {
        public String path;
        public PhotoOrientation orientation;
        public float zoom;

        public ImageArg()
        {

        }

        void toData(Data.Builder builder, String prefix)
        {
            builder.putString(prefix + "PATH", path);
            builder.putInt(prefix + "ORIENTATION", orientation.ordinal());
            builder.putFloat(prefix + "ZOOM", zoom);
        }

        ImageArg(Data data, String prefix)
        {
            path = data.getString(prefix + "PATH");
            int oidx = data.getInt(prefix + "ORIENTATION", -1);
            orientation = (oidx > -1) ? PhotoOrientation.values()[oidx] : PhotoOrientation.DEG_0;
            zoom = data.getFloat(prefix + "ZOOM", 1.0f);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static Parcelable.Creator<ImageArg> CREATOR = new Creator<ImageArg>()
        {
            @Override
            public ImageArg createFromParcel(Parcel parcel)
            {
                ImageArg ret = new ImageArg();
                ret.readFromParcel(parcel);
                return ret;
            }

            @Override
            public ImageArg[] newArray(int i)
            {
                return new ImageArg[i];
            }
        };

        @Override
        public void writeToParcel(Parcel parcel, int i)
        {
            parcel.writeString(path);

            if (orientation != null)
                parcel.writeInt(orientation.ordinal());
            else
                parcel.writeInt(-1);

            parcel.writeFloat(zoom);
        }

        public void readFromParcel(Parcel parcel)
        {
            path = parcel.readString();
            int idx = parcel.readInt();

            if (idx > -1)
                orientation = PhotoOrientation.values()[idx];
            else
                orientation = PhotoOrientation.DEG_0;

            zoom = parcel.readFloat();
        }
    }

    public ImageArg left;
    public ImageArg right;
    public boolean flip;
    public PhotoProcessor.CompositeImageType type;

    public Data toData()
    {
        Data.Builder builder = new Data.Builder();
        left.toData(builder, "LEFT_");
        right.toData(builder, "RIGHT_");
        builder.putBoolean("FLIP", flip);
        builder.putInt("TYPE", type.ordinal());

        return builder.build();
    }

    public ImagePair()
    {}

    ImagePair(Data data)
    {
        left = new ImageArg(data, "LEFT_");
        right = new ImageArg(data, "RIGHT_");
        flip = data.getBoolean("FLIP", false);
        int tidx = data.getInt("TYPE", -1);
        type = (tidx > -1) ? PhotoProcessor.CompositeImageType.values()[tidx] : PhotoProcessor.CompositeImageType.SPLIT;
    }
}