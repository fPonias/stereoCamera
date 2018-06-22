package com.munger.stereocamera.fragment;

import android.app.Dialog;
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
	public TextView messageView;
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
		messageView = view.findViewById(R.id.dialog_message);
		cancelButton = view.findViewById(R.id.cancel_button);

		getDialog().setTitle(statusString);
		messageView.setText(messageString);

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
	private String messageString = "";

	public void setStatus(String status)
	{
		statusString = status;

		Dialog d = getDialog();

		if (d != null)
			d.setTitle(status);
	}

	public void setMessage(String message)
	{
		messageString = message;

		if (messageView != null)
			messageView.setText(messageString);
	}
}
