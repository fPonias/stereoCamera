package com.munger.stereocamera.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.service.InstagramTransform;
import com.munger.stereocamera.utility.MyActivityChooserModel;
import com.munger.stereocamera.utility.MyActivityChooserView;
import com.munger.stereocamera.utility.MyShareActionProvider;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class MyShareMenuItemCtrl
{
	private MyShareActionProvider actionProvider;
	private Intent shareIntent;
	private MyActivityChooserView actionView;
	private Context context;
	private Object menuItem;
	private View menuButton;

	private MyActivityChooserModel.OnChooseActivityListener shareListener = new MyActivityChooserModel.OnChooseActivityListener()
	{
		MyActivityChooserModel host;
		ComponentName componentName;
		MyActivityChooserModel.OnChooseActivityResponder responder;

		@Override
		public void onActivityChosen(MyActivityChooserModel host, ComponentName componentName, MyActivityChooserModel.OnChooseActivityResponder responder)
		{
			if (paths == null || paths.length == 0)
				return;

			this.host = host;
			this.componentName = componentName;
			this.responder = responder;

			if (componentName.getClassName().contains("instagram") && paths.length == 1)
			{
				InstagramTransform transform = new InstagramTransform(context);
				File file = new File(paths[0]);
				transform.transform(file, new InstagramTransform.Listener()
				{
					@Override
					public void onProcessed(File dest)
					{
						Uri uri = getShareUri(dest);
						sendIntent(uri);
					}

					@Override
					public void onFailed()
					{
						Toast.makeText(context, R.string.instagram_failed_error, Toast.LENGTH_LONG).show();
					}
				});
			}
			else
			{
				sendIntent();
			}
		}

		private void sendIntent(Uri file)
		{
			shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType(host.getShareType());
			shareIntent.setComponent(componentName);
			shareIntent.putExtra(Intent.EXTRA_STREAM, file);
			MyShareActionProvider.updateIntent(shareIntent);

			responder.sendIntent(shareIntent);
		}

		private void sendIntent()
		{
			if (paths.length == 1)
			{
				sendIntent(getShareUri(new File(paths[0])));
				return;
			}

			ArrayList<Uri> uris = getShareUris();

			shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			shareIntent.setType(host.getShareType());
			shareIntent.setComponent(componentName);
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			MyShareActionProvider.updateIntent(shareIntent);

			responder.sendIntent(shareIntent);
		}

		@Override
		public void onActivityStarted(MyActivityChooserModel host, Intent intent)
		{
			super.onActivityStarted(host, intent);
		}
	};

	public MyShareMenuItemCtrl(final Context context, View shareItem, int itemId)
	{
		this.context = context;
		this.menuItem = shareItem;
		shareItem.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
		{
			MyShareMenuItemCtrl.this.onClick();
		}});

		doInit(itemId);
	}

	public MyShareMenuItemCtrl(final Context context, MenuItem shareMenuItem, int itemId)
	{
		this.context = context;
		this.menuItem = shareMenuItem;
		shareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem item)
		{
			onClick();
			return true;
		}});

		doInit(itemId);
	}

	private void doInit(final int itemId)
	{
		this.context = context;

		actionView = new MyActivityChooserView(MainActivity.getInstance());

		MainActivity mainActivity = MainActivity.getInstance();
		final ViewTreeObserver viewTreeObserver = mainActivity.getWindow().getDecorView().getViewTreeObserver();
		viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { public void onGlobalLayout()
		{
			menuButton = MainActivity.getInstance().findViewById(itemId);

			if (menuButton != null)
			{
				updateMenuPosition();

				// Now you can get rid of this listener
				viewTreeObserver.removeOnGlobalLayoutListener(this);
			}
		}});
	}

	public void updateMenuPosition()
	{
		if (menuButton == null)
			return;

		// Found it! Do what you need with the button
		int[] location = new int[2];
		menuButton.getLocationInWindow(location);
		int height = menuButton.getMeasuredHeight();

		actionView.setVerticalOffset(location[1] + height);
		actionView.setHorizontalOffset(location[0]);
	}

	private void onClick()
	{
		if (!actionView.isInEditMode())
		{
			boolean isMultiple = (paths.length > 1) ? true : false;

			MyActivityChooserModel dataModel = MyActivityChooserModel.get(context, "share_history.xml");
			dataModel.setShareType("image/jpeg", isMultiple);
			shareListener.setModel(dataModel);
			actionView.setActivityChooserModel(dataModel);
		}

		actionView.showPopup();
	}

	private Uri getShareUri(File shareFile)
	{
		Uri uri = FileProvider.getUriForFile(context, "com.munger.stereocamera.export.provider", shareFile);
		return uri;
	}

	private String[] paths;

	private ArrayList<Uri> getShareUris()
	{
		int sz = paths.length;
		ArrayList<Uri> ret = new ArrayList<>();
		for (int i = 0; i < sz; i++)
		{
			File shareFile = new File(paths[i]);
			Uri item = getShareUri(shareFile);
			ret.add(item);
		}

		return ret;
	}

	public void setCurrentPath(String path)
	{
		this.paths = new String[] {path};
	}

	public void setCurrentPaths(String[] paths)
	{
		this.paths = paths;
	}
}
