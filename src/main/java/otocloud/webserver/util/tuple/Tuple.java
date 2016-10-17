package otocloud.webserver.util.tuple;

/**
 * Created by better/zhangye on 15/9/23.
 */
public class Tuple<T> {
    private T one;

    public T getOne() {
        return one;
    }

    public void setOne(T one) {
        this.one = one;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple<?> tuple = (Tuple<?>) o;

        return !(one != null ? !one.equals(tuple.one) : tuple.one != null);

    }



    @Override
    public int hashCode() {
        return one != null ? one.hashCode() : 0;
    }
}
