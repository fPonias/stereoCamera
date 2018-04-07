package com.munger.stereocamera.fragment;

import android.app.Dialog;
import android.content.Context;
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
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


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
		int i = pager.getCurrentItem();

		if (i == sz - 1)
		{
			i = 0;
			playbackUpdateRunnable.doScroll(i, true);
		}

		while (isPlaying && i < sz)
		{
			synchronized (lock)
			{
				try { lock.wait(PLAYBACK_DELAY); } catch (InterruptedException e) {break;}

				if (!isPlaying)
					break;
			}

			i++;
			if (i < sz)
				playbackUpdateRunnable.doScroll(i, true);
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

	MainActivity.Listener appListener = new MainActivity.Listener()
	{
		@Override
		public void onNewPhoto(String newPath)
		{
			updateAdapter();

			if (handler == null)
				handler = new Handler(Looper.getMainLooper());

			playbackUpdateRunnable.doScroll(0, true);
		}
	};

	@Override
	public void onResume()
	{
		super.onResume();

		MainActivity.getInstance().addListener(appListener);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		if (isPlaying)
		{
			stopPlayback();
		}

		MainActivity.getInstance().removeListener(appListener);
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
		ImagePage frag = (ImagePage) adapter.getItem(idx);
		frag.cleanUp();

		String[] list = adapter.getData();
		int sz = list.length;
		String selectedPath = list[idx];

		File f = new File(selectedPath);

		if (f.exists())
			f.delete();

		adapter.update();

		updateLabel(idx);

		if (sz == 1)
		{
			MainActivity.getInstance().popView();
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
				if (adapter == null)
				{
					adapter = new ImagePagerAdapter(getFragmentManager(), photoFiles);

					updateLabel(0);
					pager.setAdapter(adapter);
				}
				else
					adapter.update();
			}

			@Override
			public void fail()
			{
			}
		});
	}

	public static class ImagePagerAdapter extends FragmentStatePagerAdapter
	{
		private PhotoFiles photoFiles;
		private String[] files;
		private HashMap<Integer, ImagePage> pages = new HashMap<>();
		private final Object lock = new Object();

		public String[] getData()
		{
			return files;
		}

		public ImagePagerAdapter(FragmentManager fm, PhotoFiles pf)
		{
			super(fm);

			this.photoFiles = pf;
			update();
		}

		public void update()
		{
			files = photoFiles.getFiles();

			synchronized (lock)
			{
				Set<Integer> keys = pages.keySet();

				for (int key : keys)
				{
					ImagePage page = pages.get(key);

					if (key < files.length)
					{
						String path = files[key];
						page.updatePath(path);
					}
				}
			}

			notifyDataSetChanged();
		}

		private ImagePage.Listener pageListener = new ImagePage.Listener()
		{
			@Override
			public void detached(ImagePage page)
			{
				synchronized (lock)
				{
					Set<Integer> keys = pages.keySet();
					for (int key : keys)
					{
						ImagePage pg = pages.get(key);

						if (pg == page)
						{
							pages.remove(key);
							break;
						}
					}
				}
			}
		};

		@Override
		public Fragment getItem(final int position)
		{
			final ImagePage ret = new ImagePage();
			ret.setListener(pageListener);

			synchronized (lock)
			{
				pages.put(position, ret);
			}

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
		private Bitmap bmp;
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

		public void cleanUp()
		{
			if (bmp != null && !bmp.isRecycled())
			{
				thumbnailView.setImageBitmap(null);
				bmp.recycle();
				bmp = null;
			}
		}

		@Override
		public void onDetach()
		{
			super.onDetach();

			if (listener != null)
				listener.detached(this);
		}

		public interface Listener
		{
			void detached(ImagePage page);
		}

		private Listener listener;

		public void setListener(Listener listener)
		{
			this.listener = listener;
		}

		public void updatePath(String path)
		{
			if (this.path != null && path.equals(this.path))
				return;

			this.path = path;
			update();
		}

		private void update()
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bmp = BitmapFactory.decodeFile(path, options);
			long sz = options.outWidth * options.outHeight * 4;
			double ratio = Math.ceil((double) sz / (double) 0x2000000);

			options = new BitmapFactory.Options();

			if (ratio > 1)
				options.inSampleSize = (int) ratio;

			bmp = BitmapFactory.decodeFile(path, options);

			if (thumbnailView != null)
				thumbnailView.setImageBitmap(bmp);
		}

		@Override
		public void onStart()
		{
			super.onStart();

			update();
		}
	}
}
