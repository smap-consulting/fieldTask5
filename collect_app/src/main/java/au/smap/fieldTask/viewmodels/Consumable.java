package au.smap.fieldTask.viewmodels;

/**
 * smap - Wraps a value delivered through LiveData that should be handled only once, even though
 * LiveData re-delivers its latest value to new observers (e.g. after the activity is recreated).
 */
public class Consumable<T> {

    private final T content;
    private boolean consumed = false;

    public Consumable(T content) {
        this.content = content;
    }

    /**
     * Returns the content the first time it is called and null on every subsequent call.
     */
    public T consume() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return content;
    }

    public boolean isConsumed() {
        return consumed;
    }
}
