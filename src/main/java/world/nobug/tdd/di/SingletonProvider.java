package world.nobug.tdd.di;

import java.util.List;

class SingletonProvider<T> implements ComponentProvider<T> {
    T instance;
    ComponentProvider<T> provider;

    SingletonProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (instance == null) {
            instance = provider.get(context);
        }
        return instance;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
