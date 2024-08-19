package world.nobug.tdd.di;

import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider(implementation));
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), new InjectionProvider(implementation));
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
        return components.get(new Component(ref.getComponentType(), ref.getQualifier()));
    }

    // 深度优先遍历 检查 component 的依赖的访问记录
    // visiting 保存正在被访问的记录，如果发现正在被访问的记录再次被访问，说明存在循环依赖
    private void checkDependencies(Component component, Stack<Class<?>> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            // 如果依赖的类型不存在，就提前停止递归
            Component key = new Component(dependency.getComponentType(), dependency.getQualifier());
            if (!components.containsKey(key))
                throw new DependencyNotFoundException(component.type(), dependency.getComponentType());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType())) throw new CyclicDependenciesException(visiting);
                visiting.push(dependency.getComponentType());
                checkDependencies(key, visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef> getDependencies() {
            return List.of();
        }
    }

}
