package com.github.martoreto.aauto.vex;

import com.github.martoreto.aauto.vex.IVexProxyListener;

interface IVexProxy {
    void registerListener(IVexProxyListener listener);
    void unregisterListener(IVexProxyListener listener);
    void sendData(in byte[] data);
}
