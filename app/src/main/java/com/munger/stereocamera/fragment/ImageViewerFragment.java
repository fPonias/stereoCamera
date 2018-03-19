package com.munger.stereocamera.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ImageView;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.ArrayList;


public class ImageViewerFragment extends Fragment
{
	ViewPager pager;
	private ImagePagerAdapter adapter;
	private ActionBar toolbar;

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

		BaseActivity act = MyApplication.getInstance().getCurrentActivity();
		toolbar = act.getSupportActionBar();

		setHasOptionsMenu(true);

		return ret;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.image_viewer_menu, menu);

		deleteMenuItem = menu.findItem(R.id.delete);
		deleteMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			deleteCurrent();
			return true;
		}});
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
		ArrayList<String> list = adapter.getData();
		int sz = list.size();
		String selectedPath = list.get(idx);

		File f = new File(selectedPath);

		if (f.exists())
			f.delete();

		adapter.deleteItem(idx);

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
		private ArrayList<String> files;

		public ArrayList<String> getData()
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

			String arg = files.get(position);
			args.putString("file", arg);
			ret.setArguments(args);

			return ret;
		}

		@Override
		public int getItemPosition(Object object)
		{
			ImagePage page = (ImagePage) object;
			int sz = files.size();
			for (int i = 0; i < sz; i++)
			{
				String item = files.get(i);

				if (item.equals(page.path))
					return POSITION_UNCHANGED;
			}

			return POSITION_NONE;
		}

		public void deleteItem(int position)
		{
			files.remove(position);
			notifyDataSetChanged();
		}

		@Override
		public int getCount()
		{
			return files.size();
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
		private View root;

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
		{
			root = inflater.inflate(R.layout.fragment_thumbnail_collection_object, container, false);
			thumbnailView = root.findViewById(R.id.image);

			Bundle args = getArguments();
			path = args.getString("file");

			return root;
		}

		@Override
		public void onStart()
		{
			super.onStart();

			Bitmap bmp = BitmapFactory.decodeFile(path);
			thumbnailView.setImageBitmap(bmp);
		}
	}
}
