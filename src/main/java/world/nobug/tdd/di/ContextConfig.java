package world.nobug.tdd.di;

import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional get(Type type) {
                Ref ref = Ref.of(type);

                if (isContainerType(type)) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> provider.get(this));
            }

        };
    }

    static class Ref {
        private Type container;
        private Class<?> component;

        Ref(ParameterizedType type) {
            this.container = type.getRawType();
            this.component = (Class<?>) type.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
        }

        static Ref of(Type type) {
            if (type instanceof ParameterizedType) return new Ref((ParameterizedType) type);
            return new Ref((Class<?>) type);
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }
    }

    private static Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
    }

    private static boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    // 深度优先遍历 检查 component 的依赖的访问记录
    // visiting 保存正在被访问的记录，如果发现正在被访问的记录再次被访问，说明存在循环依赖
    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            if (isContainerType(dependency))
                checkContainerDependencies(component, dependency);
            else
                checkComponentDependencies(component, visiting, (Class<?>) dependency);
        }
    }

    private void checkContainerDependencies(Class<?> component, Type dependency) {
        Class<?> componentType = getComponentType(dependency);
        if (!providers.containsKey(componentType)) throw new DependencyNotFoundException(component,
                componentType);
    }

    private void checkComponentDependencies(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        // 如果依赖的类型不存在，就提前停止递归
        Class<?> componentType = dependency;

        if (!providers.containsKey(componentType)) throw new DependencyNotFoundException(component, componentType);
        if (visiting.contains(componentType)) throw new CyclicDependenciesException(visiting);
        visiting.push(componentType);
        checkDependencies(componentType, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencyTypes() {
            return List.of();
        }
    }

}
