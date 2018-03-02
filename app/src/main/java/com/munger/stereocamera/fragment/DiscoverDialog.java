package com.munger.stereocamera.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.munger.stereocamera.R;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 2/21/18.
 */

public class DiscoverDialog extends DialogFragment
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	private View view;
	public TextView titleView;
	public ViewGroup listView;
	public Button cancelButton;

	private boolean cancelled = false;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.dialog_discover, container, false);

		titleView = view.findViewById(R.id.title);
		listView = view.findViewById(R.id.discover_list);
		cancelButton = view.findViewById(R.id.cancel_button);

		titleView.setText(R.string.discover_title);

		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (listener != null)
					listener.cancelled();

				cancelled = true;
				dismiss();
			}
		});

		renderUnrendered();

		return view;
	}

	private class EntryStruct
	{
		public String id;
		public String entry;
		public TextView view;

		@Override
		public int hashCode()
		{
			return id.hashCode();
		}
	}

	private HashMap<String, EntryStruct> entries = new HashMap<>();
	private HashMap<View, EntryStruct> views = new HashMap<>();
	private ActionListener listener;

	private View.OnClickListener entryListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			if (listener == null)
				return;

			EntryStruct str = views.get(view);
			listener.selected(str.id);
		}
	};

	private LinkedList<EntryStruct> unrenderedItems = new LinkedList<>();

	public void addDiscovery(String id, String entry)
	{
		if (cancelled)
			return;

		if (entries.containsKey(id))
		{
			EntryStruct str = entries.get(id);
			listView.removeView(str.view);
			views.remove(str.view);
		}


		EntryStruct newstr = new EntryStruct();
		newstr.id = id;
		newstr.entry = entry;

		if (listView == null)
		{
			unrenderedItems.add(newstr);
		}
		else
		{
			renderItem(newstr);
		}
	}

	private void renderUnrendered()
	{
		while (!unrenderedItems.isEmpty())
		{
			EntryStruct str = unrenderedItems.removeFirst();
			renderItem(str);
		}
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);

		cancelled = true;
	}

	private void renderItem(EntryStruct str)
	{
		if (cancelled)
			return;

		DiscoverDialogItem tv = (DiscoverDialogItem) getLayoutInflater().inflate(R.layout.dialog_discover_item, listView, false);
		tv.setText(str.entry);
		tv.setOnClickListener(entryListener);

		str.view = tv;
		entries.put(str.id, str);
		views.put(tv, str);

		listView.addView(tv);
	}

	public interface ActionListener
	{
		void cancelled();
		void selected(String id);
	}

	public void setListener(ActionListener listener)
	{
		this.listener = listener;
	}
}
