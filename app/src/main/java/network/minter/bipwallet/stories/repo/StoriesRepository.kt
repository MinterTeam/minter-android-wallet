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

package network.minter.bipwallet.stories.repo

import io.reactivex.Completable
import io.reactivex.Observable
import network.minter.bipwallet.BuildConfig
import network.minter.bipwallet.internal.Wallet
import network.minter.bipwallet.internal.data.CachedEntity
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.storage.KVStorage
import network.minter.bipwallet.stories.models.Story
import network.minter.core.internal.api.ApiService
import network.minter.core.internal.data.DataRepository

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias RepoCachedStories = CachedRepository<@kotlin.jvm.JvmSuppressWildcards List<Story>, StoriesRepository>

class StoriesRepository(
        storiesApiService: ApiService.Builder,
        private val storage: KVStorage,
        private val uid: String,
) : DataRepository<StoriesEndpoint>(storiesApiService), CachedEntity<@kotlin.jvm.JvmSuppressWildcards List<Story>> {

    companion object {
        private const val KEY_STORIES = BuildConfig.MINTER_CACHE_VERS + "stories"
        private const val KEY_WATCHED_STORIES = BuildConfig.MINTER_CACHE_VERS + "stories_watched"
    }

    fun getStories(): Observable<List<Story>> {
        return instantService.list()
                .map { it.data }
                .map { stories ->
                    val watched = getWatched()
                    stories.map { story ->
                        story.watchedLocal = false
                        if (watched.containsKey(story.id)) {
                            story.watchedLocal = watched.getOrElse(story.id) { false }
                        }
                        story
                    }
                }
                .map { list ->
                    list.filter {
                        it.slides != null && it.isActive
                    }
                }
                .map { list ->
                    list.map {
                        it.slides!!.forEach { slide ->
                            Wallet.app().image().picasso.load(slide.file).fetch()
                        }
                    }
                    list
                }
    }

    fun getWatched(): HashMap<Long, Boolean> {
        return storage[KEY_WATCHED_STORIES, HashMap()]
    }

    fun markWatched(story: Story): Completable {
        return markWatched(story.id)
    }

    fun markWatched(storyId: Long): Completable {
        return instantService.markWatched(uid, storyId)
                .doOnComplete {
                    val watched = getWatched()
                    watched[storyId] = true
                    storage.put(KEY_WATCHED_STORIES, watched)
                }
    }

    override fun getServiceClass(): Class<StoriesEndpoint> {
        return StoriesEndpoint::class.java
    }

    override fun getData(): List<Story> {
        return storage[KEY_STORIES, emptyList()]
    }

    override fun getUpdatableData(): Observable<List<Story>> {
        return getStories()
    }

    override fun onAfterUpdate(result: List<Story>) {
        storage.put(KEY_STORIES, result)
        result.forEach { story ->
            story.slides?.map { !it.file.isNullOrEmpty() }
        }
    }

    override fun onClear() {
        storage.delete(KEY_STORIES)
    }

    override fun isDataReady(): Boolean {
        return storage.contains(KEY_STORIES)
    }
}