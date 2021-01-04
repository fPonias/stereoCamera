package com.munger.stereocamera.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;import com.munger.stereocamera.R;
import com.munger.stereocamera.fragment.InstagramExportDialog;
import com.munger.stereocamera.service.InstagramTransform;
import com.munger.stereocamera.utility.MyActivityChooserModel;
import com.munger.stereocamera.utility.MyActivityChooserView;
import com.munger.stereocamera.utility.MyShareActionProvider;
import com.munger.stereocamera.utility.PhotoFile;

import java.io.File;
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
			if (files == null || files.length == 0)
				return;

			this.host = host;
			this.componentName = componentName;
			this.responder = responder;

			if (componentName.getClassName().contains("instagram") && files.length == 1)
			{
				onInstagramChosen();
			}
			else
			{
				sendIntent();
			}
		}

		private void onInstagramChosen()
		{
			InstagramExportDialog dialog = new InstagramExportDialog();
			dialog.setListener(new InstagramExportDialog.ActionListener() {
				@Override
				public void cancelled() {}

				@Override
				public void selected(InstagramTransform.TransformType type)
				{
					onInstagramChosen2(type);
				}
			});
			//dialog.show(MainActivity.getInstance().getFragmentManager(), "instagramDialog");
		}

		private void onInstagramChosen2(InstagramTransform.TransformType type)
		{
			InstagramTransform transform = new InstagramTransform(context);
			transform.transform(files[0], type, new InstagramTransform.Listener()
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
			if (files.length == 1)
			{
				sendIntent(files[0].uri);
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

	private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
	private ViewTreeObserver viewTreeObserver;

	private void doInit(final int itemId)
	{
		this.context = context;

		actionView = new MyActivityChooserView(MainActivity.getInstance());

		MainActivity mainActivity = MainActivity.getInstance();
		viewTreeObserver = mainActivity.getWindow().getDecorView().getViewTreeObserver();
		layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { public void onGlobalLayout()
		{
			if (!viewTreeObserver.isAlive())
			{
				return;
			}

			menuButton = MainActivity.getInstance().findViewById(itemId);

			if (menuButton != null)
			{
				updateMenuPosition();

				// Now you can get rid of this listener
				viewTreeObserver.removeOnGlobalLayoutListener(this);
			}
		}};
		viewTreeObserver.addOnGlobalLayoutListener(layoutListener);
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
			boolean isMultiple = (files.length > 1);

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

	private PhotoFile[] files;

	private ArrayList<Uri> getShareUris()
	{
		ArrayList<Uri> ret = new ArrayList<>();
		for (PhotoFile item : files)
			ret.add(item.uri);

		return ret;
	}

	public void setData(PhotoFile data)
	{
		files = new PhotoFile[] {data};
	}

	public void setData(PhotoFile[] data)
	{
		files = data;
	}
}
