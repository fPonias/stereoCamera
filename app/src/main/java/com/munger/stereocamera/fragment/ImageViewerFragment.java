package com.munger.stereocamera.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
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

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;


public class ImageViewerFragment extends Fragment
{
	private ViewPager pager;
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
		String selectedPath = list.get(idx);

		File f = new File(selectedPath);

		if (f.exists())
			f.delete();

		adapter.deleteItem(idx);
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance().getCurrentActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MyApplication.getInstance().getCurrentActivity().requestPermissionForResult(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					updateAdapter();
				}
			});
			return;
		}

		File targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		adapter = new ImagePagerAdapter(getFragmentManager(), targetDir);
		pager.setAdapter(adapter);
	}

	public static class ImagePagerAdapter extends FragmentStatePagerAdapter
	{
		private File targetDir;
		private ArrayList<String> files;

		public ArrayList<String> getData()
		{
			return files;
		}

		public ImagePagerAdapter(FragmentManager fm, File targetDir)
		{
			super(fm);

			this.targetDir = targetDir;
			files = getFiles();
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

		private ArrayList<String> getFiles()
		{
			ArrayList<String> ret = new ArrayList<>();
			File[] files = targetDir.listFiles();

			for (File file : files)
			{
				String filename = file.getName();
				String[] parts = filename.split("\\.");

				try
				{
					int count = Integer.parseInt(parts[0]);
					if (parts[1].equals("jpg"))
						ret.add(file.getPath());
				}
				catch(NumberFormatException e) {}
			}

			return ret;
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
