/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.internal.common;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class Conditional<T> {

    private Condition mCond;
    private Value<T> mValue;

    /**
     * Пример: <p> private Conditional<AppValue> someCond = new Conditional<>(()->App.isRunning(),
     * ()-> App.getValue()); <p> someCond.call(appVal -> appVal.doSomeAction()) <p> Кастомный
     * коллбэк с кастомным значением
     *
     * @param condition
     * @param value
     */
    public Conditional(Condition condition, Value<T> value) {
        mCond = condition;
        mValue = value;
    }

    /**
     * Коллбэк проверяет, если возвращаемое значение != null то условие равно true
     * <p>
     * private Conditional<AppValue> someCond = new Conditional<>(()->App.getValue());
     * s
     * someCond.call(appVal -> appVal.doSomeAction()) выполнится если App.getValue() != null
     *
     * @param value
     */
    public Conditional(Value<T> value) {
        mCond = () -> value.get() != null;
        mValue = value;
    }

    public void call(Callable<T> callable) {
        if (mCond.isInValidState()) {
            callable.doAction(mValue.get());
        }
    }

    public interface Callable<T> {
        void doAction(T value);
    }

    public interface Condition {
        boolean isInValidState();
    }

    public interface Value<T> {
        T get();
    }
}
