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

public class MyShareMenuItemCtrl
{
	private MyShareActionProvider actionProvider;
	private Intent shareIntent;
	private MyActivityChooserView actionView;
	private Context context;
	private MenuItem menuItem;

	private MyActivityChooserModel.OnChooseActivityListener shareListener = new MyActivityChooserModel.OnChooseActivityListener()
	{
		MyActivityChooserModel host;
		ComponentName componentName;
		MyActivityChooserModel.OnChooseActivityResponder responder;

		@Override
		public void onActivityChosen(MyActivityChooserModel host, ComponentName componentName, MyActivityChooserModel.OnChooseActivityResponder responder)
		{
			if (path == null)
				return;

			this.host = host;
			this.componentName = componentName;
			this.responder = responder;

			if (componentName.getClassName().contains("instagram"))
			{
				InstagramTransform transform = new InstagramTransform(context);
				File file = new File(path);
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
				sendIntent(getShareUri());
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

		@Override
		public void onActivityStarted(MyActivityChooserModel host, Intent intent)
		{
			super.onActivityStarted(host, intent);
		}
	};

	public MyShareMenuItemCtrl(final Context context, MenuItem shareMenuItem, final int itemId)
	{
		this.context = context;
		this.menuItem = shareMenuItem;

		actionView = new MyActivityChooserView(MainActivity.getInstance());

		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if (!actionView.isInEditMode())
				{
					MyActivityChooserModel dataModel = MyActivityChooserModel.get(context, "share_history.xml");
					dataModel.setShareType("image/jpeg");
					shareListener.setModel(dataModel);
					actionView.setActivityChooserModel(dataModel);
				}

				actionView.showPopup();

				return true;
			}
		});


		MainActivity mainActivity = MainActivity.getInstance();
		final ViewTreeObserver viewTreeObserver = mainActivity.getWindow().getDecorView().getViewTreeObserver();
		viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { public void onGlobalLayout()
		{
			View menuButton = MainActivity.getInstance().findViewById(itemId);

			if (menuButton != null)
			{
				// Found it! Do what you need with the button
				int[] location = new int[2];
				menuButton.getLocationInWindow(location);
				int height = menuButton.getMeasuredHeight();

				actionView.setVerticalOffset(location[1] + height);
				actionView.setHorizontalOffset(location[0]);

				// Now you can get rid of this listener
				viewTreeObserver.removeOnGlobalLayoutListener(this);
			}
		}});
	}

	private Uri getShareUri(File shareFile)
	{
		Uri uri = FileProvider.getUriForFile(context, "com.munger.stereocamera.export.provider", shareFile);
		return uri;
	}

	private String path;

	private Uri getShareUri()
	{
		File shareFile = new File(path);
		return getShareUri(shareFile);
	}

	public void setCurrentPath(String path)
	{
		this.path = path;
	}
}
