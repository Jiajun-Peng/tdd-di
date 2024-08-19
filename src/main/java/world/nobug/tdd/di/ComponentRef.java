package world.nobug.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    private Type container;
    private Component component;
    private Class<?> componentType;
    private Annotation qualifier;

    ComponentRef(Type type, Annotation qualifier) {
        init(type);
        this.qualifier = qualifier;
    }

    ComponentRef(Class<ComponentType> component) {
        init(component);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
        init(type);
    }

    private void init(Type type) {
        if (type instanceof ParameterizedType) {
            this.container = ((ParameterizedType) type).getRawType();
            this.componentType = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            this.componentType = (Class<?>) type;
        }
    }

    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component);
    }

    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    static ComponentRef of(Type type) {
        if (type instanceof ParameterizedType) {
            return new ComponentRef(type, null);
        }
        return new ComponentRef((Class<?>) type);
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public boolean isContainer() {
        return container != null;
    }

    public Annotation getQualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentRef ref = (ComponentRef) o;
        return Objects.equals(container, ref.container) && Objects.equals(componentType, ref.componentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, componentType);
    }
}
