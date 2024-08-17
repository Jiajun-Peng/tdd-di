package world.nobug.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    default Optional get(Type type) {
        return get(Ref.of(type));
    }

    Optional get(Ref ref);

    class Ref {
        private Type container;
        private Class<?> component;

        Ref(ParameterizedType type) {
            this.container = type.getRawType();
            this.component = (Class<?>) type.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
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
    }
}
