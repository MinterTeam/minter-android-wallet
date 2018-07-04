/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.data;

import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.Queue;

import network.minter.bipwallet.internal.TestWallet;
import network.minter.bipwallet.internal.storage.KVStorage;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RunWith(AndroidJUnit4.class)
public class KVStorageQueueSaveTest extends InstrumentationTestCase {
    @Test
    public void testOrderPreserves() {
        Queue<QueuedObj> data = new LinkedList<>();
        data.add(new QueuedObj(1, "A"));
        data.add(new QueuedObj(2, "B"));
        data.add(new QueuedObj(1, "C"));
        data.add(new QueuedObj(3, "D"));
        data.add(new QueuedObj(1, "E"));
        data.add(new QueuedObj(4, "F"));

        final KVStorage storage = TestWallet.app().storage();
        storage.putQueue("test_queue", data);

        assertTrue(storage.contains("test_queue"));

        Queue<QueuedObj> restored = storage.getQueue("test_queue");
        assertNotNull(restored);
        assertEquals(data.size(), restored.size());

        while (!restored.isEmpty()) {
            QueuedObj rest = restored.poll();
            QueuedObj src = data.poll();
            assertNotNull(rest);
            assertNotNull(src);

            assertEquals(src.id, rest.id);
            assertEquals(src.name, rest.name);
        }

    }

    public static class QueuedObj {
        public int id;
        public String name;

        QueuedObj(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
