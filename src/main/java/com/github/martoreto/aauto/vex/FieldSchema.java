package com.github.martoreto.aauto.vex;

import android.os.Parcel;
import android.os.Parcelable;

public class FieldSchema implements Parcelable {
    public static final int TYPE_STRING = 0;
    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_FLOAT = 2;

    private int type;
    private String description;

    public FieldSchema(int type, String description) {
        this.type = type;
        this.description = description;
    }

    public FieldSchema(Parcel in) {
        this.type = in.readInt();
        this.description = in.readString();
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(type);
        out.writeString(description);
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
