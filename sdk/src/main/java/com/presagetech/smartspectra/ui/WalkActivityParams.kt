package com.presagetech.smartspectra.ui

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable

class WalkActivityParams(
    val rootPosition: Rect,
    val checkupPosition: Rect,
    val infoPosition: Rect,
) : Parcelable {

    @Suppress("DEPRECATION")
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Rect::class.java.classLoader)!!,
        parcel.readParcelable(Rect::class.java.classLoader)!!,
        parcel.readParcelable(Rect::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(rootPosition, flags)
        parcel.writeParcelable(checkupPosition, flags)
        parcel.writeParcelable(infoPosition, flags)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<WalkActivityParams> {
        override fun createFromParcel(parcel: Parcel): WalkActivityParams {
            return WalkActivityParams(parcel)
        }

        override fun newArray(size: Int): Array<WalkActivityParams?> {
            return arrayOfNulls(size)
        }
    }
}
