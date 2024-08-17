package world.nobug.tdd.di;

import jakarta.inject.Provider;
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
                return get(Ref.of(type));
            }

            @Override
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> provider.get(this));
            }

        };
    }

    // 深度优先遍历 检查 component 的依赖的访问记录
    // visiting 保存正在被访问的记录，如果发现正在被访问的记录再次被访问，说明存在循环依赖
    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencyTypes()) {
            Ref ref = Ref.of(dependency);
            // 如果依赖的类型不存在，就提前停止递归
            if (!providers.containsKey(ref.getComponent())) throw new DependencyNotFoundException(component, ref.getComponent());
            if (!ref.isContainer()) {
                if (visiting.contains(ref.getComponent())) throw new CyclicDependenciesException(visiting);
                visiting.push(ref.getComponent());
                checkDependencies(ref.getComponent(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencyTypes() {
            return List.of();
        }
    }

}
