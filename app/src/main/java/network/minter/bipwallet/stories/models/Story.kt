/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.stories.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

@Parcelize
data class StorySlide(
        val id: Long,
        @SerializedName("story_id")
        val storyId: Long,
        val weight: Int,
        val title: String?,
        val file: String,
        val link: String?,
        val text: String?,
        @SerializedName("created_at")
        val createdAt: Date,
        @SerializedName("updated_at")
        val updatedAt: Date?,
        @SerializedName("deleted_at")
        val deletedAt: Date?
) : Parcelable

@Parcelize
data class Story(
        val id: Long,
        val title: String,
        val icon: String,
        val weight: Int,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("expire_date")
        val expireAt: Date?,
        @SerializedName("created_at")
        val createdAt: Date,
        @SerializedName("updated_at")
        val updatedAt: Date?,
        @SerializedName("deleted_at")
        val deletedAt: Date?,
        var slides: List<StorySlide>?,

        // local
        var watchedLocal: Boolean = false
) : Parcelable