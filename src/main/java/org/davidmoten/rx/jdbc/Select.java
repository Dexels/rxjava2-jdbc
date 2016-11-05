package org.davidmoten.rx.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class Select {

    public static <T> Flowable<T> create(Callable<Connection> connectionFactory, List<Object> parameters, String sql,
            Function<? super ResultSet, T> mapper) {
        Callable<ResultSet> initialState = () -> {
            Connection con = connectionFactory.call();
            PreparedStatement ps = con.prepareStatement(sql);
            // TODO set parameters
            ResultSet rs = ps.executeQuery();
            return rs;
        };
        BiConsumer<ResultSet, Emitter<T>> generator = (rs, emitter) -> {
            if (rs.next()) {
                emitter.onNext(mapper.apply(rs));
            } else {
                emitter.onComplete();
            }
        };
        Consumer<ResultSet> disposeState = rs -> Util.closeSilently(rs);
        return Flowable.generate(initialState, generator, disposeState);
    }
    

}
