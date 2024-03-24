package world.nobug.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ContextConfig {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(type, injectConstructor));
    }

    public Context getContext() {
        // TODO：check dependencies
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                // Context仅仅是将实现delegate给了ContextConfig中的providers
                // 所以貌似每一次调用get方法都还是会重新创建一个新的Context对象
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get());
            }
        };
    }

    class ConstructorInjectionProvider<T> implements Provider<T> {
        private Class<?> componentType;
        private Constructor<T> injectConstructor;
        private boolean constructing = false;

        public ConstructorInjectionProvider(Class<?> componentType, Constructor<T> injectConstructor) {
            this.componentType = componentType;
            this.injectConstructor = injectConstructor;
        }

        // 预期将context作为参数传入
        @Override
        public T get() {
            if (constructing) throw new CyclicDependenciesException(componentType);
            try {
                constructing = true;
                Object[] dependencies = stream(injectConstructor.getParameters())
                        .map(p -> {
                            Class<?> type = p.getType();
                            return getContext().get(type) // 每次创建Context，貌似也没啥问题，因为providers是单例的。
                                    .orElseThrow(() -> new DependencyNotFoundException(componentType, p.getType()));
                        })
                        .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (CyclicDependenciesException e) {
                throw new CyclicDependenciesException(componentType, e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                // 如果时catch Exception的话，就无法抛出DependencyNotFoundException
                throw new RuntimeException(e);
            } finally {
                constructing = false; // 不需要重置为false也可以通过测试，但是为了保险起见，还是重置为false
            }
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());

        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }
}
