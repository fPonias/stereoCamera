package com.munger.stereocamera.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.munger.stereocamera.MainActivity;import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.widget.MyShareMenuItemCtrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;


public class ImageViewerFragment extends Fragment
{
	private ViewPager pager;
	private ImagePagerAdapter adapter;
	private ImageButton playButton;
	//private AdView adView1;
	//private AdView adView2;
	private RelativeLayout rootView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View ret = inflater.inflate(R.layout.fragment_thumbnail, container, false);

		rootView = ret.findViewById(R.id.root_view);
		pager = ret.findViewById(R.id.pager);

		pager.setOnClickListener(view -> openTopBar());

		pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() { public void onPageSelected(int position)
		{
			super.onPageSelected(position);
			pageIndex = position;

			PhotoFile data = getCurrentItem();

			if (data != null)
			{
				shareMenuItemCtrl.setData(data);
				updateLabel(position);
			}

			if (!pageChangedByPlayback)
			{
				stopPlayback();
			}

			pageChangedByPlayback = false;
		}});

		playButton = ret.findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v)
		{
			togglePlayback();
		}});

		setHasOptionsMenu(true);

		//MainActivity activity = MainActivity.getInstance();
		//if (activity.getAdsEnabled())
		//	addBannerAds();

		return ret;
	}

	/*private void addBannerAds()
	{
		if (MainActivity.getInstance().getCurrentOrientation().isPortait())
		{
			adView1 = createBannerAd();
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			adView1.setLayoutParams(lp);

			rootView.addView(adView1);

			adView2 = null;
		}
		else
		{
			TableLayout tl = new TableLayout(getContext());
			RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			rllp.addRule(RelativeLayout.ALIGN_PARENT_START);
			rllp.addRule(RelativeLayout.ALIGN_PARENT_END);
			rllp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			tl.setLayoutParams(rllp);
			rootView.addView(tl);

			TableRow row = new TableRow(getContext());
			TableLayout.LayoutParams tllp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
			row.setGravity(Gravity.CENTER_HORIZONTAL);
			row.setLayoutParams(tllp);
			tl.addView(row);

			adView1 = createBannerAd();
			TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
			adView1.setId(View.generateViewId());
			adView1.setLayoutParams(lp);

			adView2 = createBannerAd();
			lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
			adView2.setLayoutParams(lp);

			row.addView(adView1);
			row.addView(adView2);
		}
	}

	private AdView createBannerAd()
	{
		MainActivity activity = MainActivity.getInstance();
		AdView adView = new AdView(activity);
		adView.setAdSize(AdSize.BANNER);

		if (activity.getIsDebug())
			adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
		else
			adView.setAdUnitId("ca-app-pub-9089181112526283/9787362203");

		return adView;
	}*/

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
			FragmentActivity activity = getActivity();

			if (activity != null)
			{
				ActionBar bar = MainActivity.getInstance().getSupportActionBar();

				if (bar != null)
					bar.setTitle(label);
			}
		}
	}

	private PhotoFile startingData = null;

	public void setStartingData(PhotoFile data)
	{
		startingData = data;
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
				try {lock.wait(PLAYBACK_DELAY);} catch(InterruptedException ignored){}

			playButton.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow));
		}
	}

	private boolean pageChangedByPlayback = false;

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
			catch(NullPointerException ignored){}

			pager.setCurrentItem(index, smoothScroll);
		}

		public void doScroll(int index, boolean smoothScroll)
		{
			pageChangedByPlayback = true;
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

		shareMenuItem = menu.findItem(R.id.export);
		shareMenuItemCtrl = new MyShareMenuItemCtrl(getContext(), shareMenuItem, R.id.export);
		updateShareMenuItemCtrl();


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

	private void updateShareMenuItemCtrl()
	{
		if (shareMenuItemCtrl != null && adapter != null)
		{
			int idx = pager.getCurrentItem();
			ImagePage frag = adapter.getItem(idx);

			if (frag != null)
			{
				PhotoFile data = frag.getData();
				shareMenuItemCtrl.setData(data);
			}
		}
	}

	public PhotoFile getCurrentItem()
	{
		if (adapter == null)
			return null;

		ImageViewerFragment.ImagePage pg = adapter.getItem(pager.getCurrentItem());

		if (pg == null)
			return null;

		return pg.getData();
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

		MainActivity context = MainActivity.getInstance();
		MutableLiveData<Long> lastFileUpdate = context.getFileSystemViewModel().getLastUpdate();
		lastFileUpdate.observe(context, longPhotoFileTreeMap ->
		{
			if (adapter == null)
				return;

			TreeMap<Long, PhotoFile> files = context.getFileSystemViewModel().getPhotoList();
			adapter.update(files);
			updateShareMenuItemCtrl();
		});
	}

	MainActivity.Listener appListener = new MainActivity.Listener()
	{
		@Override
		public void onNewPhoto(Uri newPath)
		{
			updateAdapter();

			if (handler == null)
				handler = new Handler(Looper.getMainLooper());

			playbackUpdateRunnable.doScroll(0, true);
		}
	};

	private PhotoFile getData()
	{
		return getCurrentItem();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		MainActivity activity = MainActivity.getInstance();
		activity.addListener(appListener);
		updateAdapter();
		pager.setCurrentItem(pageIndex);
		updateShareMenuItemCtrl();
/*
		if (activity.getAdsEnabled())
		{
			AdRequest adRequest = new AdRequest.Builder().build();
			adView1.loadAd(adRequest);

			if (adView2 != null)
			{
				adView2.loadAd(adRequest);
			}
		}

 */
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

		if (adapter != null)
		{
			adapter.cleanUp();
			adapter = null;
		}
	}

	public MenuItem deleteMenuItem;
	public MenuItem shareMenuItem;
	private MyShareMenuItemCtrl shareMenuItemCtrl;
	private Dialog deleteDialog;

	private void deleteCurrent()
	{
		deleteDialog = new AlertDialog.Builder(getContext())
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
		PhotoFile data = frag.getData();
		frag.cleanUp();

		MainActivity.getInstance().getFileSystemViewModel().deletePhotos(new long[]{data.id});

		if (adapter.files.size() == 1)
		{
			MainActivity.getInstance().popView();
		}
	}

	private void openTopBar()
	{

	}

	private void updateAdapter()
	{
		if (adapter == null)
		{
			adapter = new ImagePagerAdapter();

			updateLabel(pageIndex);
			pager.setAdapter(adapter);
			updateShareMenuItemCtrl();
		}

		TreeMap<Long, PhotoFile> files = MainActivity.getInstance().getFileSystemViewModel().getPhotoList();
		adapter.update(files);
	}

	public class ImagePagerAdapter extends PagerAdapter
	{
		private ArrayList<PhotoFile> files;
		private HashMap<Integer, ImagePage> pages = new HashMap<>();
		private final Object lock = new Object();

		public ImagePagerAdapter()
		{
			super();
		}

		public void update(TreeMap<Long, PhotoFile> f)
		{
			updateData(f);

			if (startingData != null)
			{
				int sz = files.size();
				for (int i = 0; i < sz; i++)
				{
					if (files.get(i).id == startingData.id)
					{
						pageIndex = i;
						break;
					}
				}

				startingData = null;
			}

			synchronized (lock)
			{
				Set<Integer> keys = pages.keySet();
				int sz = files.size();

				for (int key : keys)
				{
					ImagePage page = pages.get(key);

					if (key < sz)
					{
						PhotoFile data = files.get(key);
						page.updateData(data);
					}
				}
			}

			notifyDataSetChanged();
		}

		private void updateData(TreeMap<Long, PhotoFile> filesMap)
		{
			files = new ArrayList<>();
			for (Long index : filesMap.navigableKeySet())
			{
				files.add(filesMap.get(index));
			}
		}

		public void cleanUp()
		{
			Set<Integer> keys = pages.keySet();
			for (Integer key : keys)
			{
				ImagePage pg = pages.get(key);
				pg.cleanUp();
			}

			pages.clear();
		}

		@Override
		public int getItemPosition(Object object)
		{
			ImagePage page = (ImagePage) object;
			for (PhotoFile item : files)
			{
				if (item.id == page.data.id)
					return POSITION_UNCHANGED;
			}

			return POSITION_NONE;
		}

		public ImagePage getItem(int idx)
		{
			if (!pages.containsKey(idx))
				return null;

			return pages.get(idx);
		}

		@Override
		public int getCount()
		{
			if (files == null)
				return 0;

			return files.size();
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object)
		{
			super.setPrimaryItem(container, position, object);
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
		{
			return view == object;
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position)
		{
			ImagePage pg = new ImagePage(MainActivity.getInstance());

			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			pg.setLayoutParams(lp);

			PhotoFile item = files.get(position);
			pg.updateData(item);
			container.addView(pg);

			pages.put(position, pg);

			return pg;
		}

		@Override
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object)
		{
			pages.remove(position);
			ImagePage pg = (ImagePage) object;
			pg.cleanUp();
			container.removeView(pg);
		}
	}

	public class ImagePage extends FrameLayout
	{
		private ImageView thumbnailView;
		private Bitmap bmp;
		private PhotoFile data;

		public ImagePage(@NonNull Context context)
		{
			super(context);
			init();
		}

		public ImagePage(@NonNull Context context, @Nullable AttributeSet attrs)
		{
			super(context, attrs);
			init();
		}

		public ImagePage(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
		{
			super(context, attrs, defStyleAttr);
			init();
		}

		private void init()
		{
			thumbnailView = new ImageView(getContext());
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			thumbnailView.setLayoutParams(params);

			addView(thumbnailView);
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

		public void updateData(PhotoFile data)
		{
			if (this.data != null && data.id == this.data.id)
				return;

			this.data = data;
			update();
		}

		private void update()
		{
			/*
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bmp = BitmapFactory.decodeFile(path, options);
			long sz = options.outWidth * options.outHeight * 4;
			double ratio = Math.ceil((double) sz / (double) 0x2000000);

			options = new BitmapFactory.Options();

			if (ratio > 1)
				options.inSampleSize = (int) ratio;

			bmp = BitmapFactory.decodeFile(path, options);*/

			if (thumbnailView != null)
				thumbnailView.setImageURI(data.uri);

			ImageViewerFragment.this.updateShareMenuItemCtrl();
		}

		public PhotoFile getData()
		{
			return data;
		}
	}
}
