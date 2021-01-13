package com.munger.stereocamera.utility.data;

import androidx.room.TypeConverter;

import com.munger.stereocamera.ip.utility.FireShutter;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;


public class TypeConv
{
    @TypeConverter
    public static int roleToInt(Client.Role role)
    {
        if (role == null)
            return Client.Role.NONE.ordinal();

        return role.ordinal();
    }
    @TypeConverter
    public static Client.Role intToRole(int idx)
    {
        Client.Role[] roles = Client.Role.values();
        return (idx >= 0 && idx < roles.length) ? roles[idx] : Client.Role.NONE;
    }

    @TypeConverter
    public static int resolutionToInt(PreviewWidget.ShutterType resolution)
    {
        if (resolution == null)
            return PreviewWidget.ShutterType.PREVIEW.ordinal();

        return resolution.ordinal();
    }

    @TypeConverter
    public static PreviewWidget.ShutterType intToResolution(int idx)
    {
        PreviewWidget.ShutterType[] types = PreviewWidget.ShutterType.values();
        return (idx >= 0 && idx < types.length) ? types[idx] : PreviewWidget.ShutterType.PREVIEW;
    }

    @TypeConverter
    public static int overlayToInt(PreviewOverlayWidget.Type overlay)
    {
        if (overlay == null)
            return PreviewOverlayWidget.Type.None.ordinal();

        return overlay.ordinal();
    }

    @TypeConverter
    public static PreviewOverlayWidget.Type intToOverlay(int idx)
    {
        PreviewOverlayWidget.Type[] types = PreviewOverlayWidget.Type.values();
        return (idx >= 0 && idx < types.length) ? types[idx] : PreviewOverlayWidget.Type.None;
    }
}
