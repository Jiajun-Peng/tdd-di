package world.nobug.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    private Type container;
    private Component component;

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    ComponentRef(Class<ComponentType> component) {
        init(component, null);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType) {
            this.container = ((ParameterizedType) type).getRawType();
            Class<?> componentType = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
            this.component = new Component(componentType, qualifier);
        } else {
            this.component = new Component((Class<?>) type, qualifier);
        }
    }

    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component);
    }

    static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    static ComponentRef of(Type type, Annotation qualifier) {
        return new ComponentRef(type, qualifier);
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public boolean isContainer() {
        return container != null;
    }

    // 这里不用 getComponent 是为了更接近于 record 的语义
    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
