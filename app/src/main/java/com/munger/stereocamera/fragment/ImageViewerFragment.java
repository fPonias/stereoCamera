package com.munger.stereocamera.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.ArrayList;


public class ImageViewerFragment extends Fragment
{
	private ViewPager pager;
	private ImagePagerAdapter adapter;
	private ImageButton playButton;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View ret = inflater.inflate(R.layout.fragment_thumbnail, container, false);
		pager = ret.findViewById(R.id.pager);

		pager.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			openTopBar();
		}});
		pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() { public void onPageSelected(int position)
		{
			super.onPageSelected(position);
			pageIndex = position;
			updateLabel(position);
		}});

		playButton = ret.findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v)
		{
			togglePlayback();
		}});

		setHasOptionsMenu(true);

		return ret;
	}

	private void resetLabel()
	{
		if (menuTitle != null)
			setLabel(menuTitle);
	}

	private void updateLabel(int index)
	{
		if (menuTitle != null)
		{
			int sz = adapter.getCount();
			int idx = index + 1;
			String newTitle = menuTitle + " (" + idx + "/" + sz + ")";

			setLabel(newTitle);
		}
	}

	private void setLabel(String label)
	{
		if (adapter != null)
		{
			MainActivity activity = (MainActivity) getActivity();

			if (activity != null)
			{
				ActionBar bar = activity.getSupportActionBar();

				if (bar != null)
					bar.setTitle(label);
			}
		}
	}

	private int pageIndex = 0;
	private final Object lock = new Object();
	private Thread playbackThread = null;
	private boolean isPlaying = false;
	private PlaybackUpdateRunnable playbackUpdateRunnable = new PlaybackUpdateRunnable();
	private int PLAYBACK_DELAY = 3000;
	private Handler handler = null;

	private void togglePlayback()
	{
		if (isPlaying)
		{
			stopPlayback();
		}
		else
		{
			startPlayback();
		}
	}

	private void startPlayback()
	{
		synchronized (lock)
		{
			if (isPlaying)
				return;

			if (handler == null)
				handler = new Handler(Looper.getMainLooper());

			playbackThread = new Thread(togglePlaybackLoop);
			isPlaying = true;
			playbackThread.start();

			playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
		}
	}

	private void stopPlayback()
	{
		synchronized (lock)
		{
			if (!isPlaying)
				return;

			isPlaying = false;
			lock.notify();

			if (playbackThread != null)
				try {lock.wait(PLAYBACK_DELAY);} catch(InterruptedException e){}

			playButton.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow));
		}
	}

	private class PlaybackUpdateRunnable implements Runnable
	{
		private int index = 0;
		private boolean smoothScroll = false;

		public void setIndex(int idx)
		{
			index = idx;
		}

		public void setSmoothScroll(boolean smoothScroll)
		{
			this.smoothScroll = smoothScroll;
		}

		public void run()
		{
			try
			{
				updateLabel(index);
			}
			catch(NullPointerException e){}

			pager.setCurrentItem(index, smoothScroll);
		}

		public void doScroll(int index, boolean smoothScroll)
		{
			playbackUpdateRunnable.setIndex(index);
			playbackUpdateRunnable.setSmoothScroll(smoothScroll);
			handler.post(this);
		}
	}

	private Runnable togglePlaybackLoop = new Runnable() { public void run()
	{
		int sz = adapter.getCount();

		int i = 0;
		playbackUpdateRunnable.doScroll(0, false);
		while (isPlaying && i < sz)
		{
			playbackUpdateRunnable.doScroll(i, true);

			synchronized (lock)
			{
				try { lock.wait(PLAYBACK_DELAY); } catch (InterruptedException e) {break;}

				if (!isPlaying)
					break;
			}

			i++;
		}

		synchronized (lock)
		{
			isPlaying = false;
			playbackThread = null;
			lock.notify();
		}
	}};

	private String menuTitle;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.image_viewer_menu, menu);

		deleteMenuItem = menu.findItem(R.id.delete);
		deleteMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			stopPlayback();
			deleteCurrent();
			return true;
		}});

		try
		{
			MainActivity activity = (MainActivity) getActivity();

			if (activity != null)
			{
				ActionBar bar = activity.getSupportActionBar();

				if (bar != null)
				{
					CharSequence seq = bar.getTitle();

					if (seq != null)
						menuTitle = seq.toString();
				}
			}

			updateLabel(pageIndex);
		}
		catch(NullPointerException e){}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt("index", pageIndex);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			pageIndex = savedInstanceState.getInt("index", 0);
		}
		else
		{
			pageIndex = 0;
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if (isPlaying)
		{
			stopPlayback();
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();
		resetLabel();
	}

	public MenuItem deleteMenuItem;
	private Dialog deleteDialog;

	private void deleteCurrent()
	{
		deleteDialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.delete_confirm_title)
				.setMessage(R.string.delete_confirm_message)
				.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialogInterface, int i)
					{
						deleteDialog.dismiss();
					}
				})
				.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialogInterface, int i)
					{
						deleteDialog.dismiss();
						deleteCurrent2();
					}
				})
				.create();
		deleteDialog.show();
	}

	private void deleteCurrent2()
	{
		int idx = pager.getCurrentItem();
		String[] list = adapter.getData();
		int sz = list.length;
		String selectedPath = list[idx];

		File f = new File(selectedPath);

		if (f.exists())
			f.delete();

		adapter.deleteItem(idx);

		updateLabel(idx);

		if (sz == 1)
		{
			((MainActivity)MyApplication.getInstance().getCurrentActivity()).popView();
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();

		updateAdapter();
	}

	private void openTopBar()
	{

	}

	private void updateAdapter()
	{
		final PhotoFiles photoFiles = new PhotoFiles(getContext());
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				adapter = new ImagePagerAdapter(getFragmentManager(), photoFiles);
				updateLabel(0);
				pager.setAdapter(adapter);
			}

			@Override
			public void fail()
			{
			}
		});
	}

	public static class ImagePagerAdapter extends FragmentStatePagerAdapter
	{
		private String[] files;

		public String[] getData()
		{
			return files;
		}

		public ImagePagerAdapter(FragmentManager fm, PhotoFiles pf)
		{
			super(fm);

			files = pf.getFiles();
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment ret = new ImagePage();
			Bundle args = new Bundle();

			String arg = files[position];
			args.putString("file", arg);
			ret.setArguments(args);

			return ret;
		}

		@Override
		public int getItemPosition(Object object)
		{
			ImagePage page = (ImagePage) object;
			int sz = files.length;
			for (int i = 0; i < sz; i++)
			{
				String item = files[i];

				if (item.equals(page.path))
					return POSITION_UNCHANGED;
			}

			return POSITION_NONE;
		}

		public void deleteItem(int position)
		{
			int sz = files.length;
			String[] newFiles = new String[sz - 1];

			for (int i = 0; i < position; i++)
				newFiles[i] = files[i];

			for (int i = position + 1; i < sz; i++)
				newFiles[i - 1] = files[i];

			files = newFiles;
			notifyDataSetChanged();
		}

		@Override
		public int getCount()
		{
			return files.length;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object)
		{
			super.setPrimaryItem(container, position, object);
		}
	}

	public static class ImagePage extends Fragment
	{
		private ImageView thumbnailView;
		private String path;

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
		{
			thumbnailView = new ImageView(getContext());
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			thumbnailView.setLayoutParams(params);

			Bundle args = getArguments();
			path = args.getString("file");

			return thumbnailView;
		}

		@Override
		public void onStart()
		{
			super.onStart();

			File fl = new File(path);
			long sz = fl.length();
			Bitmap bmp;

			if (sz <= 600000) //jpegs bigger than .5 MB are likely to be too large to render
				bmp = BitmapFactory.decodeFile(path);
			else
			{
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 2;
				bmp = BitmapFactory.decodeFile(path, options);
			}

			thumbnailView.setImageBitmap(bmp);
		}
	}
}
