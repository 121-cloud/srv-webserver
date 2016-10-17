package otocloud.webserver.util.tuple;

/**
 * Created by better/zhangye on 15/9/23.
 */
public class ThreeTuple<T1, T2, T3> extends TwoTuple<T1,T2> {
    private T3 three;

    public T3 getThree() {
        return three;
    }

    public void setThree(T3 three) {
        this.three = three;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ThreeTuple<?, ?, ?> that = (ThreeTuple<?, ?, ?>) o;

        return !(three != null ? !three.equals(that.three) : that.three != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (three != null ? three.hashCode() : 0);
        return result;
    }
}
