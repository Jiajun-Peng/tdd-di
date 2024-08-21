package world.nobug.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;

public class ContextConfig {

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public ContextConfig() {
        scopes.put(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
            throw new IllegalComponentException();
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        bind(type, implementation, type.getAnnotations());
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        if (Arrays.stream(annotations).map(Annotation::annotationType)
                .anyMatch(q -> !q.isAnnotationPresent(Qualifier.class) && !q.isAnnotationPresent(Scope.class)))
            throw new IllegalComponentException();

        List<Annotation> qualifiers =
                Arrays.stream(annotations).filter(q -> q.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scopeFromType =
                Arrays.stream(implementation.getAnnotations()).filter(q -> q.annotationType().isAnnotationPresent(Scope.class)).findFirst();
        Optional<Annotation> scope =
                Arrays.stream(annotations).filter(q -> q.annotationType().isAnnotationPresent(Scope.class))
                        .findFirst().or(() -> scopeFromType);

        ComponentProvider provider = new InjectionProvider(implementation);
        if (scope.isPresent()) provider = scopes.get(scope.get().annotationType()).apply(provider);

        if (qualifiers.isEmpty())
            components.put(new Component(type, null), provider);
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scopeType, Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
        scopes.put(scopeType, provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
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

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getComponentProvider(ref))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return (Optional<ComponentType>) Optional.ofNullable(getComponentProvider(ref)).map(provider -> provider.get(this));
            }

        };
    }

    private <ComponentType> ComponentProvider<?> getComponentProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    // 深度优先遍历 检查 component 的依赖的访问记录
    // visiting 保存正在被访问的记录，如果发现正在被访问的记录再次被访问，说明存在循环依赖
    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            // 如果依赖的类型不存在，就提前停止递归
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(component , dependency.component());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) throw new CyclicDependenciesException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return List.of();
        }
    }

}
