package com.github.martoreto.aauto.vex;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

public class FieldSchema implements Parcelable {
    public static final int TYPE_STRING = 0;
    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_BOOLEAN = 3;

    private int type;
    private @Nullable String description;
    private @Nullable String unit;
    private float min;
    private float max;
    private float resolution;

    public FieldSchema(int type, @Nullable String description, @Nullable String unit, float min,
                       float max, float resolution) {
        this.type = type;
        this.description = description;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.resolution = resolution;
    }

    public FieldSchema(Parcel in) {
        this.type = in.readInt();
        this.description = in.readString();
        this.unit = in.readString();
        this.min = in.readFloat();
        this.max = in.readFloat();
        this.resolution = in.readFloat();
    }

    public int getType() {
        return type;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getUnit() {
        return unit;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getResolution() {
        return resolution;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type);
        out.writeString(description);
        out.writeString(unit);
        out.writeFloat(min);
        out.writeFloat(max);
        out.writeFloat(resolution);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public FieldSchema createFromParcel(Parcel in) {
            return new FieldSchema(in);
        }

        public FieldSchema[] newArray(int size) {
            return new FieldSchema[size];
        }
    };
}
