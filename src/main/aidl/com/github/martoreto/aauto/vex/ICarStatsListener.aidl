package com.github.martoreto.aauto.vex;

oneway interface ICarStatsListener {
    void onNewMeasurements(long timestamp, in Map values);
    void onSchemaChanged();
}
