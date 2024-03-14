package world.nobug.tdd.di;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<Class<?>, Object> components = new HashMap<>();
    private Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        components.put(type, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        if (components.containsKey(type))
            return (ComponentType) components.get(type);
        Class<?> implementation = componentImplementations.get(type);
        try {
            return (ComponentType) implementation.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        componentImplementations.put(type, implementation);
    }
}
