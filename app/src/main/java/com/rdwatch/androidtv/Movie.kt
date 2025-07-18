package com.rdwatch.androidtv

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Movie class represents video entity with title, description, image thumbs and video url.
 */
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey
    var id: Long = 0,
    var title: String? = null,
    var description: String? = null,
    var backgroundImageUrl: String? = null,
    var cardImageUrl: String? = null,
    var videoUrl: String? = null,
    var studio: String? = null,
) : Serializable {
    override fun toString(): String {
        return "Movie{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", videoUrl='" + videoUrl + '\'' +
            ", backgroundImageUrl='" + backgroundImageUrl + '\'' +
            ", cardImageUrl='" + cardImageUrl + '\'' +
            '}'
    }

    companion object {
        internal const val serialVersionUID = 727566175075960653L
    }
}
