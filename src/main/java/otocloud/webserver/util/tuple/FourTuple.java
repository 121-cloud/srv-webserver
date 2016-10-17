package otocloud.webserver.util.tuple;

/**
 * Created by better/zhangye on 15/9/23.
 */
public class FourTuple<T1, T2, T3, T4> extends ThreeTuple<T1,T2,T3> {
    private T4 four;

    public T4 getFour() {
        return four;
    }

    public void setFour(T4 four) {
        this.four = four;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FourTuple<?, ?, ?, ?> fourTuple = (FourTuple<?, ?, ?, ?>) o;

        return !(four != null ? !four.equals(fourTuple.four) : fourTuple.four != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (four != null ? four.hashCode() : 0);
        return result;
    }
}
