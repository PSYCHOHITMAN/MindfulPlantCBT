package com.mindfulplant.cbt.util

import android.graphics.Bitmap
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.mindfulplant.cbt.R

/**
 * The avatar is a TextView showing the user's initials on a circle. When the
 * user has a profile photo, that photo (cropped to a circle) becomes the
 * TextView's background and the initials are cleared.
 */
object AvatarUtil {
    fun apply(view: TextView, photo: Bitmap?, initials: String) {
        if (photo != null) {
            view.background = RoundedBitmapDrawableFactory.create(view.resources, photo)
                .apply { isCircular = true }
            view.text = ""
        } else {
            view.setBackgroundResource(R.drawable.bg_avatar_circle)
            view.text = initials
        }
    }
}
