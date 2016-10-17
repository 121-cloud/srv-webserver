package otocloud.webserver.util.tuple;

/**
 * Created by better/zhangye on 15/9/23.
 */
public class TwoTuple<T1, T2> extends Tuple<T1> {
    private T2 two;

    public T2 getTwo() {
        return two;
    }

    public void setTwo(T2 two) {
        this.two = two;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TwoTuple<?, ?> twoTuple = (TwoTuple<?, ?>) o;

        return !(two != null ? !two.equals(twoTuple.two) : twoTuple.two != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (two != null ? two.hashCode() : 0);
        return result;
    }
}
