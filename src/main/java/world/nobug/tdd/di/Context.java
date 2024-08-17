package world.nobug.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {

    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

    class Ref<ComponentType> {
        private Type container;
        private Class<?> component;

        Ref(ParameterizedType type) {
            this.container = type.getRawType();
            this.component = (Class<?>) type.getActualTypeArguments()[0];
        }

        Ref(Class<ComponentType> component) {
            this.component = component;
        }

        static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
            return new Ref<>(component);
        }

        static Ref of(Type type) {
            if (type instanceof ParameterizedType) {
                return new Ref((ParameterizedType) type);
            }
            return new Ref((Class<?>) type);
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return container != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Ref ref = (Ref) o;
            return Objects.equals(container, ref.container) && Objects.equals(component, ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }
}
