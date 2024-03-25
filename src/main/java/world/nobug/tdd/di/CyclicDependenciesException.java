package world.nobug.tdd.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesException(Class<?> component) {
        components.add(component);
    }

    public CyclicDependenciesException(Class<?> componentType, CyclicDependenciesException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public CyclicDependenciesException(Stack<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.toArray(new Class<?>[0]);
    }
}
