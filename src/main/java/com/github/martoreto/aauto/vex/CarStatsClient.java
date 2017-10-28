package com.github.martoreto.aauto.vex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarStatsClient {
    private static final String TAG = "CarStatsClient";

    public static final String PERMISSION_CAR_STATS_PROVIDER = "com.github.martoreto.aauto.vex.BIND_CAR_STATS_PROVIDER";

    private static final String ACTION_CAR_STATS_PROVIDER = "com.github.martoreto.aauto.vex.CAR_STATS_PROVIDER";

    private Context mContext;
    private Map<String, ServiceConnection> mServiceConnections = new HashMap<>();
    private Map<String, ICarStats> mProviders = new HashMap<>();
    private Map<String, ICarStatsListener> mRemoteListeners = new HashMap<>();
    private List<Listener> mListeners = new ArrayList<>();

    public CarStatsClient(Context context) {
        this.mContext = context;
    }

    public static interface Listener {
        void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values);
    }

    public void start() {
        PackageManager pm = mContext.getPackageManager();
        Intent implicitIntent = new Intent(ACTION_CAR_STATS_PROVIDER);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(implicitIntent, 0);
        for (ResolveInfo ri: resolveInfos) {
            ComponentName cn = new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
            Intent explicitIntent = new Intent(implicitIntent);
            explicitIntent.setComponent(cn);
            String provider = cn.flattenToShortString();
            ServiceConnection sc = createServiceConnection(provider);
            mServiceConnections.put(provider, sc);
            Log.d(TAG, "Binding to " + provider);
            mContext.bindService(explicitIntent, sc, Context.BIND_AUTO_CREATE);
        }
    }

    private ServiceConnection createServiceConnection(final String provider) {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.v(TAG, "Connected to " + provider);
                ICarStats stats = ICarStats.Stub.asInterface(iBinder);
                mProviders.put(provider, stats);
                ICarStatsListener listener = createListener(provider);
                mRemoteListeners.put(provider, listener);
                try {
                    stats.registerListener(listener);
                } catch (RemoteException e) {
                    Log.w(TAG, provider + ": Error registering listener", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.v(TAG, "Disconnected from " + provider);
                mProviders.remove(provider);
            }
        };
    }

    private ICarStatsListener createListener(final String provider) {
        return new ICarStatsListener.Stub() {
            @Override
            public void onNewMeasurements(long timestamp, Map values) throws RemoteException {
                for (Listener listener: mListeners) {
                    try {
                        listener.onNewMeasurements(provider, new Date(timestamp), values);
                    } catch (Exception e) {
                        Log.e(TAG, "Error calling listener", e);
                    }
                }
            }
        };
    }

    public void stop() {
        for (Map.Entry<String, ICarStats> e: mProviders.entrySet()) {
            try {
                e.getValue().unregisterListener(mRemoteListeners.get(e.getKey()));
            } catch (RemoteException e1) {
                Log.w(TAG, e.getKey() + ": Error unregistering listener", e1);
            }
        }
        for (ServiceConnection sc: mServiceConnections.values()) {
            mContext.unbindService(sc);
        }

        mProviders.clear();
        mRemoteListeners.clear();
        mServiceConnections.clear();
    }

    public Map<String, Object> getMergedMeasurements() {
        Map<String, Object> measurements = new HashMap<>();
        for (Map.Entry<String, ICarStats> e: mProviders.entrySet()) {
            try {
                measurements.putAll(e.getValue().getMergedMeasurements());
            } catch (RemoteException e1) {
                Log.w(TAG, e.getKey() + ": Error getting measurements", e1);
            }
        }
        return measurements;
    }

    public void registerListener(Listener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        mListeners.remove(listener);
    }
}
