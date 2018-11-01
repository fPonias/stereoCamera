package com.munger.stereocamera.fragment;

import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.ActionProvider;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class Gallery extends Fragment
{
    private RecyclerView grid;
    private LayoutMgr layoutMgr;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private PhotoFiles.DatedFiles imageFiles;
    private PhotoFiles files;

    private ViewGroup bottomMenu;
    private AppCompatImageButton shareButton;
    private AppCompatImageButton trashButton;

    public enum Types
    {
        TITLE,
        IMAGE
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        files = new PhotoFiles(getActivity());
        files.openTargetDir(new PhotoFiles.Listener() {
            @Override
            public void done()
            {
                imageFiles = files.getFilesByDate();
                adapter = new Gallery.Adapter();

                if (grid != null)
                    grid.setAdapter(adapter);
            }

            @Override
            public void fail() {

            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("firstIndex"))
        {
            firstShownIndex = savedInstanceState.getInt("firstIndex");
        }

        setHasOptionsMenu(true);
    }

    private MenuItem editItem;
    private boolean isEditing = false;
    private HashSet<Integer> selected = new HashSet<>();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.gallery_menu, menu);

        editItem = menu.findItem(R.id.editBtn);

        editItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {public boolean onMenuItemClick(MenuItem item)
        {
            toggleEditing();
            return true;
        }});
    }

    private void toggleEditing()
    {
        isEditing = !isEditing;

        if (isEditing)
        {
            bottomMenu.setVisibility(View.VISIBLE);
            selected.clear();
        }
        else
        {
            bottomMenu.setVisibility(View.GONE);
        }
    }

    private int firstShownIndex = 0;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        int idx = layoutMgr.getFirstShownIndex();
        outState.putInt("firstIndex", idx);
    }

    private int getIdx(int position)
    {
        int ret = 0;
        int sz = imageFiles.dates.size();
        for (int i = 0; i < sz; i++)
        {
            position--;

            Long date = imageFiles.dates.get(i);
            ArrayList<String> list = imageFiles.files.get(date);
            position -= list.size();

            if (position > 0)
                ret++;
        }

        return ret;
    }

    private int getSubIdx(int position)
    {
        int sz = imageFiles.dates.size();
        for (int i = 0; i < sz; i++)
        {
            if (position == 0)
                return -1;

            position--;

            Long date = imageFiles.dates.get(i);
            ArrayList<String> list = imageFiles.files.get(date);
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
        grid = ret.findViewById(R.id.daysList);
        layoutMgr = new LayoutMgr();
        grid.setLayoutManager(layoutMgr);

        if (adapter != null)
            grid.setAdapter(adapter);

        if (firstShownIndex > 0)
            layoutMgr.scrollToIndex(firstShownIndex);


        bottomMenu = ret.findViewById(R.id.bottom_menu);
        shareButton = ret.findViewById(R.id.share_btn);
        trashButton = ret.findViewById(R.id.trash_btn);

        trashButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v)
        {
            deleteSelected();
        }});

        return ret;
    }

    private void deleteSelected()
    {
        int[] toDelete = new int[selected.size()];
        int i = 0;
        Iterator<Integer> iter = selected.iterator();
        while(iter.hasNext())
        {
            int position = iter.next();
            toDelete[i] = position;
            int idx = getIdx(position);
            int subidx = getSubIdx(position);

            String path = imageFiles.files.get(imageFiles.dates.get(idx)).get(subidx);
            File file = new File(path);
            file.delete();
            i++;
        }

        Arrays.sort(toDelete);
        int sz = toDelete.length;
        for (i = sz - 1; i >= 0; i--)
        {
            int position = toDelete[i];
            int idx = getIdx(position);
            int subidx = getSubIdx(position);

            long hash = imageFiles.dates.get(idx);
            ArrayList<String> paths = imageFiles.files.get(hash);

            paths.remove(subidx);
            if (paths.size() == 0)
            {
                imageFiles.files.remove(hash);
                imageFiles.dates.remove(idx);
            }
        }

        selected.clear();
        toggleEditing();

        layoutMgr.reset();
        adapter.notifyDataSetChanged();
    }

    private void openThumbnail(int position)
    {
        int idx = getIdx(position);
        int subIdx = getSubIdx(position);
        long dt = imageFiles.dates.get(idx);
        String path = imageFiles.files.get(dt).get(subIdx);

        MainActivity.getInstance().startThumbnailView(path);
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

        private HashMap<Integer, Integer> widgetIndex = new HashMap<>();

        private View.OnClickListener clickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                int tag = (int) view.getTag();
                int position = widgetIndex.get(tag);

                if (!isEditing)
                    openThumbnail(position);
                else
                    toggleSelected(view, position);
            }
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
                int subSz = imageFiles.files.get(date).size();
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
                    TextView tv = new TextView(getActivity());
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
            long year = dt / 10000;
            long month = (dt - year * 10000) / 100;
            long day = (dt - year * 10000 - month * 100);

            Calendar now = Calendar.getInstance();
            int thisyear = now.get(Calendar.YEAR);

            Calendar then = Calendar.getInstance();
            then.set(Calendar.YEAR, (int) year);
            then.set(Calendar.MONTH, (int) month);
            then.set(Calendar.DAY_OF_MONTH, (int) day);

            if (now.equals(then))
            {
                return "Today";
            }

            now.add(Calendar.DAY_OF_YEAR, -1);
            if (now.equals(then))
            {
                return "Yesterday";
            }

            StringBuilder ret = new StringBuilder();
            ret.append(months[(int) month - 1]);
            ret.append(" ");
            ret.append(day);

            if (year != Calendar.getInstance().get(Calendar.YEAR))
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

                String path = imageFiles.files.get(dt).get(subIdx);
                Bitmap bmp = getBitmap(path);
                imageHolder.thumbnail.setImageBitmap(bmp);

                if (selected.contains(position))
                    imageHolder.selectedIcon.setVisibility(View.VISIBLE);
                else
                    imageHolder.selectedIcon.setVisibility(View.INVISIBLE);
            }
        }

        private Bitmap getBitmap(String path)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 16;
            Bitmap bmp = BitmapFactory.decodeFile(path, options);
            return bmp;
        }
    }

    private class LayoutMgr extends RecyclerView.LayoutManager
    {
        private int currentY = 0;
        private int maxY = 0;
        private ArrayList<Integer> itemMinYs;
        private ArrayList<Integer> itemMaxYs;
        private ArrayList<Integer> itemMinXs;
        private ArrayList<Integer> itemMaxXs;
        private ArrayList<Types> itemTypes;

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            RecyclerView.LayoutParams ret = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );

            return ret;
        }

        private void calculatePositions(RecyclerView.State state)
        {
            itemMinYs = new ArrayList<>();
            itemMaxYs = new ArrayList<>();
            itemMinXs = new ArrayList<>();
            itemMaxXs = new ArrayList<>();
            itemTypes = new ArrayList<>();

            Display display = getActivity().getWindowManager().getDefaultDisplay();
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
                dy = 0 - currentY;
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

        private HashMap<Integer, View> placedViews = new HashMap<>();

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
