package world.nobug.tdd.di;

import jakarta.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
        BiFunction<List<Method>, Class<?>, List<Method>> function = (methods, current) -> getList(component, current, methods);

        List<Method> injectMethods = traverse1(component, function);
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Method> traverse1(Class<T> component, BiFunction<List<Method>, Class<?>, List<Method>> function) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(function.apply(injectMethods, current));
            current = current.getSuperclass();
        }
        return injectMethods;
    }

    private static <T> List<Method> getList(Class<T> component, Class<?> current, List<Method> injectMethods) {
        return injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(m, injectMethods))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList();
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        BiFunction<List<Field>, Class<?>, List<Field>> function = InjectionProvider::getList;

        List<Field> injectFields = traverse(component, function);
        return injectFields;
    }

    private static <T> List<Field> traverse(Class<T> component, BiFunction<List<Field>, Class<?>, List<Field>> function) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(function.apply(injectFields, current));
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static List<Field> getList(List<Field> injectFields, Class<?> current) {
        return injectable(current.getDeclaredFields()).toList();
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
    public List<Class<?>> getDependencies() {
        Stream<? extends Class<?>> b = injectFields.stream().map(Field::getType);
        Stream<? extends Class<?>> a = Arrays.stream(injectConstructor.getParameters()).map(Parameter::getType);
        Stream<Class<?>> c = injectMethods.stream().flatMap(m -> Arrays.stream(m.getParameterTypes()));
        Stream<Class<?>> concat = Stream.concat(a, b);
        return Stream.concat(concat, c).collect(Collectors.toList());
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
        return context.get(field.getType()).get();
    }

    private static <T> Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getParameterTypes())
                .map(t -> context.get(t).get()).toArray();
    }
}
