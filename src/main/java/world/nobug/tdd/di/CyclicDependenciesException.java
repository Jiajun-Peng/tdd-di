package world.nobug.tdd.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesException(Class<?> componentType) {
        components.add(componentType);
    }

    public CyclicDependenciesException(Class<?> componentType, Class<?>[] components) {
        this.components.add(componentType);
        this.components.addAll(Set.of(components));
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
