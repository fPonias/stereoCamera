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
import com.munger.stereocamera.service.InstagramTransform;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by hallmarklabs on 2/21/18.
 */

public class DiscoverDialog extends DialogFragment
{
	public DiscoverDialog()
	{
		knownListCtrl = new DiscoverDialogList(this);
		discoveredListCtrl = new DiscoverDialogList(this);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	private View view;
	public ViewGroup knownView;
	public ViewGroup discoverView;
	public Button cancelButton;
	ActionListener listener;

	private DiscoverDialogList knownListCtrl;
	private DiscoverDialogList discoveredListCtrl;

	boolean cancelled = false;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.dialog_discover, container, false);

		knownView = view.findViewById(R.id.known_list);
		knownListCtrl.setTarget(knownView);

		discoverView = view.findViewById(R.id.discover_list);
		discoveredListCtrl.setTarget(discoverView);

		cancelButton = view.findViewById(R.id.cancel_button);

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

		return view;
	}

	static class EntryStruct
	{
		public String id;
		public String entry;
		public TextView view;

		public EntryStruct(String id, String entry)
		{
			this.id = id;
			this.entry = entry;
		}

		@Override
		public int hashCode()
		{
			return id.hashCode();
		}
	}

	public void addDiscovery(String id, String entry)
	{
		discoveredListCtrl.addEntry(id, entry);
	}

	private LinkedList<EntryStruct> pendingKnown = new LinkedList<>();

	public void addKnown(String id, String entry)
	{
		knownListCtrl.addEntry(id, entry);
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);

		cancelled = true;
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
