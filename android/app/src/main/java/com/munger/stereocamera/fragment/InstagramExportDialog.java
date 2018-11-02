package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.munger.stereocamera.R;
import com.munger.stereocamera.service.InstagramTransform;

public class InstagramExportDialog extends DialogFragment
{
    private RadioGroup choiceGroup;
    private Button cancelBtn;
    private Button okayBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.dialog_instagram, container, false);

        choiceGroup = ret.findViewById(R.id.selection);
        choiceGroup.check(R.id.radio_copy);

        okayBtn = ret.findViewById(R.id.okay);
        cancelBtn = ret.findViewById(R.id.cancel);

        okayBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
        {
            okay();
        }});

        cancelBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
        {
            cancel();
        }});

        return ret;
    }

    private void okay()
    {
        int id = choiceGroup.getCheckedRadioButtonId();
        InstagramTransform.TransformType type = InstagramTransform.TransformType.ROTATE;
        if (id == R.id.radio_copy)
            type = InstagramTransform.TransformType.COPY;
        else if (id == R.id.radio_rotate)
            type = InstagramTransform.TransformType.ROTATE;
        else if (id == R.id.radio_square)
            type = InstagramTransform.TransformType.SQUARE;
        else if (id == R.id.radio_squarerot)
            type = InstagramTransform.TransformType.SQUARE_ROTATE;

        listener.selected(type);

        dismiss();
    }

    private void cancel()
    {
        listener.cancelled();

        dismiss();
    }

    public interface ActionListener
    {
        void cancelled();
        void selected(InstagramTransform.TransformType type);
    }

    private ActionListener listener;
    public void setListener(ActionListener listener)
    {
        this.listener = listener;
    }
}
