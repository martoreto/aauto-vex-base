package com.github.martoreto.aauto.vex;

import com.github.martoreto.aauto.vex.ICarStatsListener;

interface ICarStats {
    void registerListener(ICarStatsListener listener);
    void unregisterListener(ICarStatsListener listener);
    Map getMergedMeasurements();

    boolean needsPermissions();
    void requestPermissions();

    Map getSchema();
}
