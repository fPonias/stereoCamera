package com.munger.stereocamera.utility.data;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class FileSystemViewModel extends ViewModel
{
    private TreeMap<Long, PhotoFile> photoList;
    private MutableLiveData<PhotoFile> mostRecentPhoto;
    private final PhotoFiles photoFiles;

    public FileSystemViewModel()
    {
        photoFiles = PhotoFiles.Factory.get();
        lastUpdate = new MutableLiveData<>();
        mostRecentPhoto = new MutableLiveData<>();
        loadPhotoList();
        loadMostRecent();
    }

    private void loadPhotoList()
    {
        photoList = new TreeMap<>((o1, o2) -> (int) (o2 - o1));
        List<PhotoFile> files = photoFiles.getAllFiles();
        for (PhotoFile file : files)
        {
            photoList.put(file.date, file);
        }

        lastUpdate.postValue(System.currentTimeMillis());
    }

    private void loadMostRecent()
    {
        PhotoFile file = photoFiles.getNewest();
        mostRecentPhoto.postValue(file);
    }

    public PhotoFiles getPhotoFiles()
    {
        return photoFiles;
    }

    public MutableLiveData<PhotoFile> getMostRecentPhoto()
    {
        return mostRecentPhoto;
    }

    private MutableLiveData<Long> lastUpdate;

    public MutableLiveData<Long> getLastUpdate()
    {
        return lastUpdate;
    }

    public TreeMap<Long, PhotoFile> getPhotoList()
    {
        return photoList;
    }

    public PhotoFile saveFile(File file)
    {
        PhotoFiles.SaveResult result = photoFiles.saveFile(file);
        PhotoFile recent = photoFiles.getFile(result.id);
        photoList.put(recent.date, recent);
        loadMostRecent();
        lastUpdate.postValue(System.currentTimeMillis());

        return recent;
    }

    public void deletePhotos(long[] ids)
    {
        PhotoFile mostRecent = mostRecentPhoto.getValue();
        boolean mostRecentMatch = false;

        for (long id : ids)
        {
            if (mostRecent != null && id == mostRecent.id)
                mostRecentMatch = true;

            deletePhoto(id, photoList);
        }

        if (mostRecentMatch)
        {
            mostRecent = photoFiles.getNewest();
            mostRecentPhoto.postValue(mostRecent);
        }

        lastUpdate.postValue(System.currentTimeMillis());
    }

    private void deletePhoto(long id, SortedMap<Long, PhotoFile> files)
    {
        PhotoFile file = photoFiles.getFile(id);
        if (file == null)
            return;

        photoFiles.delete(id);

        if (files != null)
            files.remove(file.date);
    }
}
