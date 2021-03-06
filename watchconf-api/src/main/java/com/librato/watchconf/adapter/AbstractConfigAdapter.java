package com.librato.watchconf.adapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractConfigAdapter<T, V> implements DynamicConfig<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractConfigAdapter.class);
    protected final Class<T> clazz;
    protected final List<ChangeListener> changeListenerList = new ArrayList();
    protected final AtomicReference<Optional<T>> config = new AtomicReference(Optional.absent());
    protected final Converter<T, V> converter;

    protected AbstractConfigAdapter(Converter<T, V> converter, Optional<ChangeListener<T>> changeListener) {
        Preconditions.checkArgument(converter != null, "converter cannot be null");
        this.converter = converter;
        this.clazz = getClassForType();

        if (changeListener.isPresent()) {
            registerListener(changeListener.get());
        }
    }

    public Optional<T> get() throws Exception {
        return config.get();
    }

    protected void getAndSet(V v) {
        try {
            config.set(Optional.of(converter.toDomain(v, clazz)));
        } catch (Exception ex) {
            log.error("unable to parse config", ex);
            notifyListenersOnError(ex);
        }
    }

    public void registerListener(ChangeListener changeListener) {
        changeListenerList.add(changeListener);
    }

    public void removeListener(ChangeListener changeListener) {
        changeListenerList.remove(changeListener);
    }

    protected void notifyListeners() {
        for (ChangeListener changeListener : changeListenerList) {
            changeListener.onChange(config.get());
        }
    }

    protected void notifyListenersOnError(Exception ex) {
        for (ChangeListener changeListener : changeListenerList) {
            changeListener.onError(ex);
        }
    }


    private Class<T> getClassForType() {
        return (Class<T>) (((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments())[0];
    }

    @Override
    public void shutdown() throws Exception {
        // NO-OP
    }
}
