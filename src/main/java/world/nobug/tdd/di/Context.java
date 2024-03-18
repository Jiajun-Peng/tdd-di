package world.nobug.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, () -> {
            try {
                Constructor<Implementation> injectConstructor = implementation.getConstructor();
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(p -> get(p.getType()))
                        .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }
}
