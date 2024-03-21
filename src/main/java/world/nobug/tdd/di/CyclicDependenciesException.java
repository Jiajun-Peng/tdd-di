package world.nobug.tdd.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesException(Class<?> component) {
        components.add(component);
    }

    public CyclicDependenciesException(Class<?> componentType, CyclicDependenciesException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public Class<?>[] getComponents() {
        return components.toArray(new Class<?>[0]);
    }
}
