package org.davidmoten.rx.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Preconditions;

import io.reactivex.Flowable;

public class SelectBuilder {

    final String sql;
    private final SqlInfo sqlInfo;
    final Flowable<Connection> connections;

    // mutable
    private List<Object> list = null;
    Flowable<List<Object>> parameters = null;
    boolean valuesOnly = false;

    public SelectBuilder(String sql, Flowable<Connection> connections) {
        this.sql = sql;
        this.connections = connections;
        this.sqlInfo = SqlInfo.parse(sql);
    }

    public SelectBuilder parameters(Flowable<List<Object>> parameters) {
        Preconditions.checkArgument(list == null);
        if (this.parameters == null)
            this.parameters = parameters;
        else
            this.parameters = this.parameters.concatWith(parameters);
        return this;
    }

    public SelectBuilder parameterList(List<Object> values) {
        Preconditions.checkArgument(list == null);
        if (this.parameters == null)
            this.parameters = Flowable.just(values);
        else
            this.parameters = this.parameters.concatWith(Flowable.just(values));
        return this;
    }

    public SelectBuilder parameterList(Object... values) {
        Preconditions.checkArgument(list == null);
        if (this.parameters == null)
            this.parameters = Flowable.just(Lists.newArrayList(values));
        else
            this.parameters = this.parameters.concatWith(Flowable.just(Lists.newArrayList(values)));
        return this;
    }

    public SelectBuilder parameter(String name, Object value) {
        Preconditions.checkArgument(parameters == null);
        if (list == null) {
            list = new ArrayList<>();
        }
        this.list.add(new Parameter(name, value));
        return this;
    }

    public SelectBuilder parameters(Object... values) {
        if (values.length == 0) {
            // no effect
            return this;
        }
        Preconditions.checkArgument(list == null);
        Preconditions.checkArgument(sqlInfo.numParameters() > 0, "no parameters present in sql!");
        Preconditions.checkArgument(values.length % sqlInfo.numParameters() == 0,
                "number of values should be a multiple of number of parameters in sql: " + sql);
        Preconditions.checkArgument(Arrays.stream(values).allMatch(o -> sqlInfo.names().isEmpty()
                || (o instanceof Parameter && ((Parameter) o).hasName())));
        return parameters(Flowable.fromArray(values).buffer(sqlInfo.numParameters()));
    }

    public <T> Flowable<T> getAs(Class<T> cls) {
        resolveParameters();
        return Select.create(connections.firstOrError(), parameters, sql,
                rs -> Util.mapObject(rs, cls, 1));
    }

    void resolveParameters() {
        if (list != null) {
            parameters = Flowable.fromIterable(list).buffer(sqlInfo.numParameters());
        }
    }

    public TransactedSelectBuilder transacted() {
        return new TransactedSelectBuilder(this);
    }

}