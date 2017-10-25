// IVexProxyCallback.aidl
package com.github.martoreto.aauto.vex;

oneway interface IVexProxyListener {
    void onConnected();
    void onData(in byte[] data);
    void onDisconnected();
}
