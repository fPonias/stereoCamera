package com.munger.stereocamera.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.munger.stereocamera.bluetooth.command.PhotoOrientation;

public class PhotoProcessorService extends IntentService
{
	public static class PhotoArgument implements Parcelable
	{
		public static Parcelable.Creator<PhotoArgument> CREATOR = new Creator<PhotoArgument>()
		{
			@Override
			public PhotoArgument createFromParcel(Parcel parcel)
			{
				PhotoArgument ret = new PhotoArgument();
				ret.readFromParcel(parcel);
				return ret;
			}

			@Override
			public PhotoArgument[] newArray(int i)
			{
				return new PhotoArgument[i];
			}
		};

		@Override
		public int describeContents()
		{
			return 0;
		}

		public String jpegPath;
		public PhotoOrientation orientation;
		public float zoom;

		@Override
		public void writeToParcel(Parcel parcel, int i)
		{
			parcel.writeString(jpegPath);
			parcel.writeInt(orientation.ordinal());
			parcel.writeFloat(zoom);
		}

		public void readFromParcel(Parcel parcel)
		{
			jpegPath = parcel.readString();
			int idx = parcel.readInt();
			orientation = PhotoOrientation.values()[idx];
			zoom = parcel.readFloat();
		}
	}

	public static String LEFT_PHOTO_ARG = "leftPhoto";
	public static String RIGHT_PHOTO_ARG = "rightPhoto";
	public static String FLIP_ARG = "flip";

	public static String BROADCAST_PROCESSED_ACTION = "com.munger.stereocamera.PROCESSED";
	public static String EXTENDED_DATA_PATH = "com.munger.stereocamera.PATH";

	public PhotoProcessorService()
	{
		super("PhotoProcessorService");
	}

	public PhotoProcessorService(String name)
	{
		super(name);
	}

	private PhotoArgument left;
	private PhotoArgument right;
	private boolean flip;

	@Override
	protected void onHandleIntent(@Nullable Intent intent)
	{
		if (!parseArguments(intent))
			return;


		PhotoProcessor proc = new PhotoProcessor(this);

		proc.setData(false, left.jpegPath, left.orientation, left.zoom);
		proc.setData(true, right.jpegPath, right.orientation, right.zoom);

		String out = proc.processData(flip);

		if (out == null)
			return;


		Intent localIntent = new Intent(BROADCAST_PROCESSED_ACTION);
		localIntent.putExtra(EXTENDED_DATA_PATH, out);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private boolean parseArguments(Intent i)
	{
		Bundle b = i.getExtras();

		if (b == null)
			return false;

		if (!b.containsKey(LEFT_PHOTO_ARG))
			return false;

		if (!b.containsKey(RIGHT_PHOTO_ARG))
			return false;

		if (!b.containsKey(FLIP_ARG))
			flip = false;
		else
			flip = b.getBoolean(FLIP_ARG);

		left = b.getParcelable(LEFT_PHOTO_ARG);
		right = b.getParcelable(RIGHT_PHOTO_ARG);

		return true;
	}
}
