package world.nobug.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CyclicDependenciesException extends RuntimeException{
    private Set<Component> components = new HashSet<>();

    public CyclicDependenciesException(List<Component> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class<?>[]::new);
    }
}
