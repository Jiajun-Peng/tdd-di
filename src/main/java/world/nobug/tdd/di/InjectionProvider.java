package world.nobug.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();

        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) throw new IllegalComponentException();
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) throw new IllegalComponentException();
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {

        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(m, methods))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (injectField, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(finder.apply(members, current));
            current = current.getSuperclass();
        }
        return members;
    }

    private static <Type> Constructor<Type> getInjectConstructor(
            Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> getDefaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> getDefaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            // 根据构造函数的参数，获取依赖的实例
            T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
            for (Field field : injectFields)
                field.set(instance, toDependency(context, field));
            for (Method method : injectMethods)
                method.invoke(instance, toDependencies(context, method));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return Stream.concat(
                        Stream.concat(Arrays.stream(injectConstructor.getParameters()).map(this::toComponentRef),
                                injectFields.stream().map(this::toComponentRef)),
                        injectMethods.stream().flatMap(m -> Arrays.stream(m.getParameters())).map(this::toComponentRef))
                .toList();
    }

    private ComponentRef toComponentRef(Field field) {
        Annotation qualifier =
                Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                        .findFirst().orElse(null);
        return ComponentRef.of(field.getGenericType(), qualifier);
    }

    private ComponentRef<?> toComponentRef(Parameter parameter) {
        Annotation qualifier =
                Arrays.stream(parameter.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                        .findFirst().orElse(null);
        return ComponentRef.of(parameter.getParameterizedType(), qualifier);
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return Arrays.stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static Predicate<Method> isOverride(Method m) {
        return im -> im.getName().equals(m.getName()) &&
                Arrays.equals(im.getParameterTypes(), m.getParameterTypes());
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return Arrays.stream(component.getDeclaredMethods())
                .filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(isOverride(m));
    }

    private static boolean isOverrideByInjectMethod(Method m, List<Method> injectMethods) {
        return injectMethods.stream().noneMatch(isOverride(m));
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, field.getGenericType());
    }

    private static <T> Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getParameters()).map(p -> toDependency(context, p.getParameterizedType()))
                .toArray();
    }

    private static Object toDependency(Context context, Type type) {
        return context.get(ComponentRef.of(type)).get();
    }

}
