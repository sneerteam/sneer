package sneerteam.snapi;

import rx.*;
import rx.functions.*;
import android.util.*;

public class ContactUtils {

    public static Observable<String> nickname(Cloud cloud, String publicKey) {
        return Observable.merge(
                cloud.path(":me", "contacts", publicKey, "nickname").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return Pair.create(3, nickname);
                }}),
                cloud.path(publicKey, "profile", "nickname").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return Pair.create(2, nickname);
                }}),
                cloud.path(publicKey, "profile", "name").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return Pair.create(1, nickname);
                }})).filter(new Func1<Pair<Integer, Object>, Boolean>() {
                    int best = 0;
                    @Override public Boolean call(Pair<Integer, Object> pair) {
                        boolean ret = pair.first <= pair.first;
                        best = Math.max(pair.first, best);
                        return ret;
                    }
                })
                .map(new Func1<Pair<Integer, Object>, Object>() {@Override public Object call(Pair<Integer, Object> pair) {
                    return pair.second;
                }})
                .cast(String.class);
    }
}
