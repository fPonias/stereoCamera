package com.munger.stereocamera.fragment;

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;

import com.munger.stereocamera.R;
import com.munger.stereocamera.widget.PreviewWidget;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 3/10/18.
 */

public class DiscoverDialogList
{
	private HashMap<String, DiscoverDialog.EntryStruct> discoveredEntries = new HashMap<>();
	private HashMap<View, DiscoverDialog.EntryStruct> views = new HashMap<>();
	private DiscoverDialog parent;
	private ViewGroup target;

	public DiscoverDialogList(DiscoverDialog parent)
	{
		this.parent = parent;
	}

	public void setTarget(ViewGroup target)
	{
		this.target = target;

		if (target != null)
			renderPending();
	}

	private View.OnClickListener entryListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			if (parent.listener == null)
				return;

			DiscoverDialog.EntryStruct str = views.get(view);
			parent.listener.selected(str.id);
		}
	};

	private LinkedList<DiscoverDialog.EntryStruct> pending = new LinkedList<>();

	private void renderPending()
	{
		for (DiscoverDialog.EntryStruct str : pending)
		{
			renderItem(str);
		}
		pending = new LinkedList<>();
	}

	public void addEntry(String id, String entry)
	{
		if (parent.cancelled)
			return;

		if (target == null)
		{
			pending.add(new DiscoverDialog.EntryStruct(id, entry));
			return;
		}

		if (discoveredEntries.containsKey(id))
		{
			DiscoverDialog.EntryStruct str = discoveredEntries.get(id);
			return;
		}


		DiscoverDialog.EntryStruct newstr = new DiscoverDialog.EntryStruct(id, entry);

		renderItem(newstr);
	}

	private void renderItem(DiscoverDialog.EntryStruct str)
	{
		if (parent.cancelled)
			return;

		DiscoverDialogItem tv = (DiscoverDialogItem) parent.getLayoutInflater().inflate(R.layout.dialog_discover_item, target, false);
		tv.setText(str.entry);
		tv.setOnClickListener(entryListener);

		str.view = tv;
		discoveredEntries.put(str.id, str);
		views.put(tv, str);

		target.addView(tv);
	}
}
