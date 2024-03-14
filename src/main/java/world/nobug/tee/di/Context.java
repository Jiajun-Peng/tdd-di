package world.nobug.tee.di;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<Class<?>, Object> components = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        components.put(type, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) components.get(type);
    }
}
