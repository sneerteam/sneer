package sneerteam.snapi;

import static android.util.Pair.*;
import rx.*;
import rx.functions.*;
import android.util.*;

public class ContactUtils {

    public static Observable<String> nickname(Cloud cloud, String publicKey) {
        return Observable.merge(
                cloud.path(":me", "contacts", publicKey, "nickname").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return create(3, nickname);
                }}),
                cloud.path(publicKey, "profile", "nickname").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return create(2, nickname);
                }}),
                cloud.path(publicKey, "profile", "name").value().map(new Func1<Object, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Object nickname) {
                    return create(1, nickname);
                }})).scan(new Func2<Pair<Integer, Object>, Pair<Integer, Object>, Pair<Integer, Object>>() {@Override public Pair<Integer, Object> call(Pair<Integer, Object> a, Pair<Integer, Object> b) {
                    return a.first > b.first ? a : b;
                }})
                .map(new Func1<Pair<Integer, Object>, Object>() {@Override public Object call(Pair<Integer, Object> pair) {
                    return pair.second;
                }})
                .distinctUntilChanged()
                .cast(String.class);
    }
    
}
