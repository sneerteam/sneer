package sneer.rx;

import rx.Observable;

public class Timeline<T> {

    public final Observable<T> past;
    public final Observable<T> future;

    public Timeline(Observable<T> past, Observable<T> future) {
        this.past = past;
        this.future = future;
    }
}
