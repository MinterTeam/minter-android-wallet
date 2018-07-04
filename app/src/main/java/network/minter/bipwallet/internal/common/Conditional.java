package network.minter.bipwallet.internal.common;

/**
 * Dogsy. 2017
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
