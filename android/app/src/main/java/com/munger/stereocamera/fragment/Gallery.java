package com.munger.stereocamera.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.data.FileSystemViewModel;
import com.munger.stereocamera.widget.MyShareMenuItemCtrl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Gallery extends Fragment
{
    private RecyclerView grid;
    private LayoutMgr layoutMgr;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;

    private ViewGroup bottomMenu;
    private MyShareMenuItemCtrl shareMenuItemCtrl;

    public enum Types
    {
        TITLE,
        IMAGE
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FileSystemViewModel vm = MainActivity.getInstance().getFileSystemViewModel();
        MutableLiveData<Long> photoListUpdate = vm.getLastUpdate();
        photoListUpdate.observe(this, this::update);

        adapter = new Gallery.Adapter();

        if (savedInstanceState != null && savedInstanceState.containsKey("firstIndex"))
        {
            firstShownIndex = savedInstanceState.getInt("firstIndex");
            isEditing = savedInstanceState.getBoolean("isEditing");
            int[] arr = savedInstanceState.getIntArray("selected");

            selected = new HashSet<>();
            for (int value : arr)
                selected.add(value);
        }

        setHasOptionsMenu(true);
    }

    private DatedFiles imageFiles;

    public static class DatedFiles
    {
        public ArrayList<Long> dates = new ArrayList<>();
        public HashMap<Long, ArrayList<PhotoFile>> files = new HashMap<>();

        public ArrayList<PhotoFile> get(long date)
        {
            return files.get(date);
        }
    }

    private void update(long lastUpdate)
    {
        imageFiles = new DatedFiles();

        TreeMap<Long, PhotoFile> longPhotoFileTreeMap = MainActivity.getInstance().getFileSystemViewModel().getPhotoList();
        for (Map.Entry<Long, PhotoFile> item : longPhotoFileTreeMap.entrySet())
        {
            long idx = item.getKey();
            idx = (idx / 86400) * 86400;

            if (!imageFiles.files.containsKey(idx))
            {
                imageFiles.dates.add(idx);
                imageFiles.files.put(idx, new ArrayList<>());
            }

            Objects.requireNonNull(imageFiles.files.get(idx)).add(item.getValue());
        }

        Collections.sort(imageFiles.dates, (l, r) -> (int) (r - l));


        adapter.notifyDataSetChanged();
    }

    private boolean isEditing = false;
    private HashSet<Integer> selected = new HashSet<>();

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.gallery_menu, menu);

        MenuItem editItem = menu.findItem(R.id.editBtn);

        editItem.setOnMenuItemClickListener(item ->
        {
            setIsEditing(!isEditing);
            return true;
        });
    }

    private void setIsEditing(boolean isEditing)
    {
        this.isEditing = isEditing;

        if (isEditing)
        {
            bottomMenu.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> shareMenuItemCtrl.updateMenuPosition(), 250);
        }
        else
        {
            bottomMenu.setVisibility(View.GONE);
            selected.clear();
        }
    }

    private int firstShownIndex = 0;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        int idx = layoutMgr.getFirstShownIndex();
        outState.putInt("firstIndex", idx);
        outState.putBoolean("isEditing", isEditing);

        int[] arr = new int[selected.size()];
        Iterator<Integer> iter = selected.iterator();
        int i = 0;
        while(iter.hasNext())
        {
            arr[i] = iter.next();
            i++;
        }
        outState.putIntArray("selected", arr);
    }

    private int getIdx(int position)
    {
        int ret = 0;
        for (long date : imageFiles.dates)
        {
            position--;

            ArrayList<PhotoFile> list = imageFiles.get(date);
            position -= list.size();

            if (position >= 0)
                ret++;
        }

        return ret;
    }

    private int getSubIdx(int position)
    {
        for (long date : imageFiles.dates)
        {
            if (position == 0)
                return -1;

            position--;

            ArrayList<PhotoFile> list = imageFiles.files.get(date);
            assert list != null;
            int imgSz = list.size();

            if (position < imgSz)
                return position;

            position -= imgSz;
        }

        return position;
    }

    public Types getItemViewType(int position)
    {
        int subIdx = getSubIdx(position);

        if (subIdx == -1)
            return Types.TITLE;
        else
            return Types.IMAGE;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_gallery, container, false);

        bottomMenu = ret.findViewById(R.id.bottom_menu);
        AppCompatImageButton shareButton = ret.findViewById(R.id.share_btn);
        AppCompatImageButton trashButton = ret.findViewById(R.id.trash_btn);

        trashButton.setOnClickListener(v -> deleteSelected());

        shareMenuItemCtrl = new MyShareMenuItemCtrl(getContext(), shareButton, R.id.share_btn);
        updateShareMenuItemCtrl();

        setIsEditing(isEditing);


        grid = ret.findViewById(R.id.daysList);
        grid.setAdapter(adapter);
        layoutMgr = new LayoutMgr();
        grid.setLayoutManager(layoutMgr);

        if (firstShownIndex > 0)
            layoutMgr.scrollToIndex(firstShownIndex);

        return ret;
    }

    private void deleteSelected()
    {
        int[] ids = new int[selected.size()];
        int i = 0;
        for (int position : selected)
        {
            int idx = getIdx(position);
            int subidx = getSubIdx(position);

            long date = imageFiles.dates.get(idx);
            ids[i] = Objects.requireNonNull(imageFiles.files.get(date)).get(subidx).id;
            i++;
        }

        FileSystemViewModel vm = MainActivity.getInstance().getFileSystemViewModel();
        vm.deletePhotos(ids);

        selected.clear();
        setIsEditing(false);

        layoutMgr.reset();
    }

    private void updateShareMenuItemCtrl()
    {
        if (shareMenuItemCtrl != null && adapter != null)
        {
            int sz = selected.size();
            Iterator<Integer> iter = selected.iterator();
            PhotoFile[] list = new PhotoFile[sz];
            int i = 0;

            while(iter.hasNext())
            {
                int pos = iter.next();
                int idx = getIdx(pos);
                int subidx = getSubIdx(pos);

                Long hash = imageFiles.dates.get(idx);
                list[i] = Objects.requireNonNull(imageFiles.files.get(hash)).get(subidx);
                i++;
            }

            shareMenuItemCtrl.setData(list);
        }
    }

    private void openThumbnail(int position)
    {
        int idx = getIdx(position);
        int subIdx = getSubIdx(position);
        long dt = imageFiles.dates.get(idx);
        PhotoFile data = Objects.requireNonNull(imageFiles.files.get(dt)).get(subIdx);

        MainActivity.getInstance().startThumbnailView(data);
    }

    private void toggleSelected(View view, int position)
    {
        ImageView selectedIcon = view.findViewById(R.id.check);
        if (selected.contains(position))
        {
            selectedIcon.setVisibility(View.INVISIBLE);
            selected.remove(position);
        }
        else
        {
            selectedIcon.setVisibility(View.VISIBLE);
            selected.add(position);
            updateShareMenuItemCtrl();
        }
    }

    private static int tag = 0;

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        class TitleHolder extends RecyclerView.ViewHolder
        {
            public TextView title;
            public TitleHolder(TextView itemView)
            {
                super(itemView);
                title = itemView;
            }
        }

        class ImageHolder extends RecyclerView.ViewHolder
        {

            public ImageView thumbnail;
            public ImageView selectedIcon;

            public ImageHolder(View itemView)
            {
                super(itemView);

                tag++;
                itemView.setTag(tag);
                itemView.setOnClickListener(clickListener);

                thumbnail = itemView.findViewById(R.id.image);
                selectedIcon = itemView.findViewById(R.id.check);
            }
        }

        private final HashMap<Integer, Integer> widgetIndex = new HashMap<>();

        private final View.OnClickListener clickListener = view ->
        {
            int tag = (int) view.getTag();
            Integer position = widgetIndex.get(tag);

            if (position == null)
                return;

            if (!isEditing)
                openThumbnail(position);
            else
                toggleSelected(view, position);
        };

        @Override
        public int getItemCount()
        {
            if (imageFiles == null)
                return 0;

            int ret = 0;
            int sz = imageFiles.dates.size();
            for (int i = 0; i < sz; i++)
            {
                ret++;
                long date = imageFiles.dates.get(i);
                int subSz = Objects.requireNonNull(imageFiles.files.get(date)).size();
                ret += subSz;
            }

            return ret;
        }

        @Override
        public int getItemViewType(int position)
        {
            Types type = Gallery.this.getItemViewType(position);
            return type.ordinal();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inf = getLayoutInflater();
            Types type = Types.values()[viewType];
            switch (type)
            {
                case TITLE:
                    TextView tv = new TextView(getContext());
                    return new TitleHolder(tv);
                case IMAGE:
                    View view = inf.inflate(R.layout.fragment_gallery_grid_thumbnail, parent, false);
                    //ImageView view = new ImageView(getActivity());
                    return new ImageHolder(view);
            }

            return null;
        }

        public String[] months = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        private String getDateFromLong(long dt)
        {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            long nowMillis = now.getTimeInMillis();

            Calendar then = Calendar.getInstance();
            then.setTimeInMillis(dt * 1000);
            int year = then.get(Calendar.YEAR);
            int month = then.get(Calendar.MONTH);
            int day = then.get(Calendar.DAY_OF_MONTH);
            long thenMillis = then.getTimeInMillis();

            if (then.after(now))
                return "Today";

            now.add(Calendar.DAY_OF_YEAR, -1);
            if (then.after(now))
                return "Yesterday";

            StringBuilder ret = new StringBuilder();
            ret.append(months[(int) month]);
            ret.append(" ");
            ret.append(day);

            if (year != now.get(Calendar.YEAR))
            {
                ret.append(" ");
                ret.append(year);
            }

            return ret.toString();
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            int idx = getIdx(position);
            int subIdx = getSubIdx(position);
            long dt = imageFiles.dates.get(idx);
            if (subIdx == -1 && holder instanceof TitleHolder)
            {
                TitleHolder titleHolder = (TitleHolder) holder;
                String title = getDateFromLong(dt);
                titleHolder.title.setText(title);
            }
            else if (subIdx >= 0 && holder instanceof ImageHolder)
            {
                ImageHolder imageHolder = (ImageHolder) holder;
                int tag = (int) holder.itemView.getTag();
                widgetIndex.put(tag, position);

                PhotoFile data = Objects.requireNonNull(imageFiles.files.get(dt)).get(subIdx);
                FileSystemViewModel vm = MainActivity.getInstance().getFileSystemViewModel();
                Bitmap bmp = vm.getPhotoFiles().getThumbnail(data.id);
                imageHolder.thumbnail.setImageBitmap(bmp);

                if (selected.contains(position))
                    imageHolder.selectedIcon.setVisibility(View.VISIBLE);
                else
                    imageHolder.selectedIcon.setVisibility(View.INVISIBLE);
            }
        }

        private final Rect emptyRect = new Rect();
    }

    private class LayoutMgr extends RecyclerView.LayoutManager
    {
        private int currentY = 0;
        private int maxY = 0;
        private ArrayList<Integer> itemMinYs;
        private ArrayList<Integer> itemMaxYs;
        private ArrayList<Integer> itemMinXs;
        private ArrayList<Integer> itemMaxXs;

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {

            return new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
        }

        private void calculatePositions(RecyclerView.State state)
        {
            itemMinYs = new ArrayList<>();
            itemMaxYs = new ArrayList<>();
            itemMinXs = new ArrayList<>();
            itemMaxXs = new ArrayList<>();
            ArrayList<Types> itemTypes = new ArrayList<>();

            Display display = getContext().getDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);

            float density  = getResources().getDisplayMetrics().density;
            int width = getWidth();
            int height = getHeight();

            int imgWidth = (int) (160 * density);
            int rowCount = width / imgWidth;
            int diff = width - rowCount * imgWidth;
            int spacing = diff / (rowCount + 1);
            int imgh = (int) (80 * density);
            int imgw = (int) (160 * density);
            int th = (int)(20 * density);

            int curY = 0;
            int curX = 0;

            int sz = state.getItemCount();

            for (int i = 0; i < sz; i++)
            {
                Types type = Gallery.this.getItemViewType(i);
                itemTypes.add(type);

                if (type == Types.TITLE)
                {
                    if (curX > 0)
                    {
                        curX = 0;
                        curY += imgh + spacing;
                    }

                    itemMinXs.add(0);
                    itemMaxXs.add(width);
                    itemMinYs.add(curY);
                    itemMaxYs.add(curY + th);

                    curX = 0;
                    curY += th + spacing;
                }
                else
                {
                    itemMinXs.add(curX + spacing);
                    itemMinYs.add(curY);

                    itemMaxYs.add(curY + imgh);
                    itemMaxXs.add(curX + spacing + imgw);

                    curX += imgw + spacing;

                    if (curX > width - imgh)
                    {
                        curX = 0;
                        curY += imgh + spacing;
                    }
                }
            }

            maxY = curY - height;
        }

        private boolean resetOnLayout = false;

        public void reset()
        {
            resetOnLayout = true;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state)
        {
            if (resetOnLayout)
            {
                resetOnLayout = false;
                removeAndRecycleAllViews(recycler);
                placedViews.clear();
            }

            calculatePositions(state);

            if (delayedScroll > 0)
            {
                currentY = itemMinYs.get(delayedScroll);
                delayedScroll = 0;
            }

            doLayout(recycler, state);
        }

        @Override
        public boolean canScrollHorizontally()
        {
            return false;
        }

        @Override
        public boolean canScrollVertically()
        {
            return true;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state)
        {
            int nextY = currentY + dy;
            if (nextY > maxY)
            {
                dy = maxY - currentY;
                currentY = maxY;
            }
            else if (nextY < 0)
            {
                dy = -currentY;
                currentY = 0;
            }
            else
            {
                currentY = nextY;
            }

            offsetChildrenVertical(-dy);
            doLayout(recycler, state);

            return dy;
        }

        private final HashMap<Integer, View> placedViews = new HashMap<>();

        private void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state)
        {
            int height = getHeight();
            int min = currentY;
            int max = currentY + height;
            int sz = state.getItemCount();

            for (int i = 0; i < sz; i++)
            {
                int yMin = itemMinYs.get(i);
                int yMax = itemMaxYs.get(i);

                if (yMin < max && yMax > min)
                {
                    if (!placedViews.containsKey(i))
                    {
                        View view = recycler.getViewForPosition(i);

                        addView(view);
                        int bottom = yMax - currentY;
                        measureChild(view, 0, 0);
                        int xMin = itemMinXs.get(i);
                        int xMax = itemMaxXs.get(i);
                        layoutDecorated(view, xMin, yMin - currentY, xMax, bottom);

                        placedViews.put(i, view);
                    }
                }
                else
                {
                    if (placedViews.containsKey(i))
                    {
                        View view = placedViews.remove(i);
                        assert view != null;
                        removeAndRecycleView(view, recycler);
                    }
                }
            }
        }

        public int getFirstShownIndex()
        {
            Set<Integer> keys = placedViews.keySet();
            int ret = adapter.getItemCount() - 1;
            for (int key : keys)
            {
                if (key < ret)
                    ret = key;
            }

            return ret;
        }

        private int delayedScroll = 0;

        public void scrollToIndex(int index)
        {
            if (itemMinYs == null)
            {
                delayedScroll = index;
                return;
            }

            int yOffset = itemMinYs.get(index);
            grid.scrollTo(0, yOffset);
        }
    }
}
