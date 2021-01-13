package com.munger.stereocamera.utility.data;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.widget.PreviewWidget;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ClientViewModel extends ViewModel
{
    private MutableLiveData<Set<Client>> clients;
    private HashMap<String, Client> clientAddressIdx;
    private HashMap<Long, Client> clientIdx;
    private Client lastUsed;

    public ClientViewModel()
    {
        loadClients();
    }

    private void loadClients()
    {
        if (clients == null)
        {
            clients = new MutableLiveData<>();
            clientAddressIdx = new HashMap<>();
            clientIdx = new HashMap<>();
        }

        List<Client> clientList =  MyApplication.getInstance().getDatabase().clientDao().getAll();
        Set<Client> clientData = new HashSet<>();
        lastUsed = null;
        for (Client cli : clientList)
        {
            clientData.add(cli);
            clientAddressIdx.put(cli.address, cli);
            clientIdx.put(cli.id, cli);

            if (lastUsed == null || lastUsed.lastUsed < cli.lastUsed)
                lastUsed = cli;
        }

        clients.postValue(clientData);
    }

    public MutableLiveData<Set<Client>> getAll()
    {
        return clients;
    }

    public Client getLast()
    {
        return lastUsed;
    }

    public Client get(String address)
    {
        if (clientAddressIdx.containsKey(address))
            return clientAddressIdx.get(address);

        Client ret = new Client();
        ret.address = address;

        clientAddressIdx.put(address, ret);
        clients.getValue().add(ret);

        ret.id = MyApplication.getInstance().getDatabase().clientDao().insert(ret);

        clientIdx.put(ret.id, ret);

        return ret;
    }

    public boolean has(String address)
    {
        return clientAddressIdx.containsKey(address);
    }

    public void update(Client client)
    {
        MyApplication.getInstance().getDatabase().clientDao().update(client);

        if (lastUsed == null || client.lastUsed > lastUsed.lastUsed)
            lastUsed = client;

        clientAddressIdx.put(client.address, client);
    }

    private Client currentClient;

    public Client getCurrentClient()
    {
        return currentClient;
    }

    public void setCurrentClient(Client client)
    {
        currentClient = client;
    }

    public static class CameraDataPair
    {
        public CameraData local;
        public CameraData remote;
    }

    public CameraDataPair getCameras(long clientid, boolean isFacing)
    {
        if (!clientIdx.containsKey(clientid))
            return null;

        Client client = clientIdx.get(clientid);

        CameraDataPair ret = new CameraDataPair();
        CameraDataDao dao =  MyApplication.getInstance().getDatabase().cameraDataDao();
        List<CameraData> cameras = dao.getByClient(clientid, isFacing);
        int sz = cameras.size();
        if (sz == 2)
        {
            CameraData one = cameras.get(0);
            CameraData two = cameras.get(1);
            ret.local = (!one.isRemote) ? one : two;
            ret.remote = (one.isRemote) ? one : two;
            return ret;
        }

        if (sz > 0)
        {
            return null;
        }

        ret.local = new CameraData();
        ret.local.clientid = clientid;
        ret.local.isFacing = isFacing;
        ret.local.isLeft = true;
        ret.local.zoom = 1.0f;
        ret.local.isRemote = false;
        ret.local.id = dao.insert(ret.local);

        ret.remote = new CameraData();
        ret.remote.clientid = clientid;
        ret.remote.isFacing = isFacing;
        ret.remote.isLeft = false;
        ret.remote.zoom = 1.0f;
        ret.remote.isRemote = true;
        ret.remote.id = dao.insert(ret.remote);

        return ret;
    }

    public void update(CameraData cameraData)
    {
        CameraDataDao dao =  MyApplication.getInstance().getDatabase().cameraDataDao();
        dao.update(cameraData);
    }
}
