package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.munger.stereocamera.R;

/**
 * Created by hallmarklabs on 2/21/18.
 */

public class ListenDialog extends DialogFragment
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	private View view;
	public TextView statusView;
	public Button cancelButton;

	private boolean cancelled = false;

	public boolean getIsCancelled()
	{
		return cancelled;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.dialog_listen, container, false);
		statusView = view.findViewById(R.id.dialog_status);
		cancelButton = view.findViewById(R.id.cancel_button);

		statusView.setText(statusString);

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

	private ActionListener listener = null;

	public interface ActionListener
	{
		void cancelled();
	}

	public void setListener(ActionListener listener)
	{
		this.listener = listener;
	}

	private String statusString = "";

	public void setStatus(String status)
	{
		statusString = status;

		if (statusView != null)
			statusView.setText(status);
	}
}
