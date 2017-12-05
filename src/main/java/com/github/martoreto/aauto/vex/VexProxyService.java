package com.github.martoreto.aauto.vex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.car.Car;
import android.support.car.CarConnectionCallback;
import android.support.car.CarNotConnectedException;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.apps.auto.sdk.service.CarVendorExtensionManagerLoader;
import com.google.android.apps.auto.sdk.service.vec.CarVendorExtensionManager;

import java.io.IOException;

public abstract class VexProxyService extends Service {
    private static final String TAG = "VexProxy";

    public static final String PERMISSION_VEX = "com.google.android.gms.permission.CAR_VENDOR_EXTENSION";

    private static final long RETRY_DELAY_MS = 16000;

    private Car mCar;
    private CarVendorExtensionManager mVexManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private RemoteCallbackList<IVexProxyListener> mListeners;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service starting.");

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mListeners = new RemoteCallbackList<>();

        mCar = Car.createCar(this, mCarConnectionCallback, mHandler);
        mHandler.post(mConnectToCar);
    }

    private final Runnable mConnectToCar = new Runnable() {
        @Override
        public void run() {
            mCar.connect();
        }
    };

    private final CarConnectionCallback mCarConnectionCallback = new CarConnectionCallback() {
        @Override
        public void onConnected(Car car) {
            if (car != mCar) {
                Log.d(TAG, "onConnected: wrong car");
                return;
            }

            Log.i(TAG, "Car connected.");
            try {
                CarVendorExtensionManagerLoader vexLoader =
                        (CarVendorExtensionManagerLoader)mCar.getCarManager(
                                CarVendorExtensionManagerLoader.VENDOR_EXTENSION_LOADER_SERVICE);
                mVexManager = vexLoader.getManager(getVendorChannelName());
                if (mVexManager == null) {
                    throw new RuntimeException("Exlap channel not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing VEX channel", e);
                return;
            }

            mVexManager.registerListener(mVexListener);
            dispatchOnConnected();
        }

        @Override
        public void onDisconnected(Car car) {
            if (car != mCar) {
                Log.d(TAG, "onDisconnected: wrong car");
                return;
            }

            Log.i(TAG, "Car disconnected.");
            dispatchOnDisconnected();
            carDisconnected();
        }
    };

    private void carDisconnected() {
        mHandler.postDelayed(mConnectToCar, RETRY_DELAY_MS);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service stopping.");
        mHandlerThread.quitSafely();
        if (mVexManager != null) {
            mVexManager.release();
            mVexManager = null;
        }
        if (mCar.isConnected()) {
            mCar.disconnect();
        }
        super.onDestroy();
    }

    private final IVexProxy.Stub mBinder = new IVexProxy.Stub() {
        @Override
        public void registerListener(final IVexProxyListener listener) throws RemoteException {
            mListeners.register(listener);

            // If we are already connected, we send the onConnected() event to the newly
            // registered listener.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVexManager != null) {
                        try {
                            listener.onConnected();
                        } catch (Exception e) {
                            Log.d(TAG, "Exception sending initial onConnected()", e);
                        }
                    }
                }
            });
        }

        @Override
        public void unregisterListener(final IVexProxyListener listener) throws RemoteException {
            mListeners.unregister(listener);
        }

        @Override
        public void sendData(byte[] data) throws RemoteException {
            try {
                mVexManager.sendData(data);
            } catch (CarNotConnectedException e) {
                throw new RemoteException("Car not connected");
            } catch (IOException e) {
                Log.w(TAG, "IOException in sendData", e);
                throw new RemoteException("I/O Error sending data");
            }
        }
    };

    private CarVendorExtensionManager.CarVendorExtensionListener mVexListener = new CarVendorExtensionManager.CarVendorExtensionListener() {
        @Override
        public void onData(CarVendorExtensionManager carVendorExtensionManager, byte[] bytes) {
            dispatchOnData(bytes);
        }
    };

    private void dispatchOnData(byte[] data) {
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onData(data);
            } catch (RemoteException e) {
                Log.d(TAG, "Exception from callback", e);
            }
        }
        mListeners.finishBroadcast();
    }

    private void dispatchOnConnected() {
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onConnected();
            } catch (RemoteException e) {
                Log.d(TAG, "Exception from callback", e);
            }
        }
        mListeners.finishBroadcast();
    }

    private void dispatchOnDisconnected() {
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onDisconnected();
            } catch (RemoteException e) {
                Log.d(TAG, "Exception from callback", e);
            }
        }
        mListeners.finishBroadcast();
    }

    protected abstract String getVendorChannelName();

    public static boolean needsPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, PERMISSION_VEX)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Context context) {
        Intent i = new Intent(context, PermissionsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
