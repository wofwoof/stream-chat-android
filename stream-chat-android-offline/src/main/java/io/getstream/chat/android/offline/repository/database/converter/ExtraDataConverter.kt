package io.getstream.chat.android.offline.repository.database.converter

import androidx.room.TypeConverter
import com.squareup.moshi.adapter

public class ExtraDataConverter {
    @OptIn(ExperimentalStdlibApi::class)
    private val adapter = moshi.adapter<MutableMap<String, Any>>()

    @TypeConverter
    public fun stringToMap(data: String?): MutableMap<String, Any>? {
        if (data.isNullOrEmpty() || data == "null") {
            return mutableMapOf()
        }
        return adapter.fromJson(data)
    }

    @TypeConverter
    public fun mapToString(someObjects: MutableMap<String, Any>?): String {
        if (someObjects == null) {
            return "{}"
        }
        return adapter.toJson(someObjects)
    }
}
