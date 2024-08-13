package world.nobug.tdd.di;

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<T> component) {
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
    }

    private List<Method> getInjectMethods(Class<T> component) {
        Class<T> current = component;
        List<Method> injectMethods = new ArrayList<>();
        while (current != Object.class) {
            injectMethods.addAll(Arrays.stream(current.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Inject.class))
                    .filter(m -> injectMethods.stream().noneMatch(im -> im.getName().equals(m.getName()) &&
                            Arrays.equals(im.getParameterTypes(), m.getParameterTypes())))
                    .filter(m -> Arrays.stream(component.getDeclaredMethods())
                            .filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                            .noneMatch(m1 -> m1.getName().equals(m.getName()) &&
                                    Arrays.equals(m1.getParameterTypes(), m.getParameterTypes())))
                    .toList());
            current = (Class<T>) current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> Constructor<Type> getInjectConstructor(
            Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    @Override
    public T get(Context context) {
        try {
            // 根据构造函数的参数，获取依赖的实例
            Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray(Object[]::new);
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields)
                field.set(instance, context.get(field.getType()).get());
            for (Method method : injectMethods) {
                method.invoke(instance, Arrays.stream(method.getParameterTypes()).map(t -> context.get(t).get()).toArray());
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        Stream<? extends Class<?>> b = injectFields.stream().map(Field::getType);
        Stream<? extends Class<?>> a = Arrays.stream(injectConstructor.getParameters()).map(Parameter::getType);
        Stream<Class<?>> c = injectMethods.stream().flatMap(m -> Arrays.stream(m.getParameterTypes()));
        Stream<Class<?>> concat = Stream.concat(a, b);
        return Stream.concat(concat, c).collect(Collectors.toList());
    }
}
