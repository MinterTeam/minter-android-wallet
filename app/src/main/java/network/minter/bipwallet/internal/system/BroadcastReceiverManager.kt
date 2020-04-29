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

package network.minter.bipwallet.internal.system

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.lang.ref.WeakReference
import java.util.*

/**
 * Minter. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */

class BroadcastReceiverManager<T>(context: T) : LifecycleObserver
        where T : Context, T : LifecycleOwner {

    private val _receivers: MutableList<BaseBroadcastReceiver> = ArrayList(1)
    private val _context: WeakReference<Context> = WeakReference(context)
    private var _listener: OnReceiveListener? = null

    fun add(receiver: BaseBroadcastReceiver): BroadcastReceiverManager<T> {
        _receivers.add(receiver)
        receiver.register(_context.get())
        return this
    }

    fun setOnReceiveListener(listener: OnReceiveListener?) {
        _listener = listener
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun register() {
        _receivers.forEach { item ->
            val receiverClass: Class<out BaseBroadcastReceiver> = item.javaClass
            item.setOnReceiveListener { context: Context?, intent: Intent? ->
                if (_listener != null) {
                    _listener!!.onReceive(receiverClass, context, intent)
                }
            }
            item.unregister(_context.get())
            item.register(_context.get())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unregister() {
        _receivers.forEach { rec ->
            _context.get()?.let {
                rec.unregister(it)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun release() {
        _context.clear()
    }

    interface OnReceiveListener {
        fun onReceive(receiverClass: Class<out BaseBroadcastReceiver?>?, context: Context?, intent: Intent?)
    }

    init {
        context.lifecycle.addObserver(this)
    }
}