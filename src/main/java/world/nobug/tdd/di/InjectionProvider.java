package world.nobug.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

class InjectionProvider<T> implements ComponentProvider<T> {
    private Injectable<Constructor<T>> injectConstructor;
    private List<Injectable<Method>> injectMethods;
    private List<Injectable<Field>> injectFields;


    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();

        this.injectConstructor = getInjectConstructor(component);
        this.injectMethods = getInjectMethods(component);
        this.injectFields = getInjectFields(component);

        if (injectFields.stream().map(Injectable::element).anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();

    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of((Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> getDefaultConstructor(component)));
    }

    private static <T> List<Injectable<Method>> getInjectMethods(Class<T> component) {
        List<Method> injectMethods1 = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(m, methods))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
        Collections.reverse(injectMethods1);
        return injectMethods1.stream().map(Injectable::of).toList();
    }

    private static <T> List<Injectable<Field>> getInjectFields(Class<T> component) {
        return InjectionProvider.<Field>traverse(component,
                        (injectField, current) -> injectable(current.getDeclaredFields()).toList())
                .stream().map(Injectable::of).toList();
    }

    @Override
    public T get(Context context) {
        try {
            // 根据构造函数的参数，获取依赖的实例
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> field : injectFields)
                field.element.set(instance, field.toDependencies(context)[0]);
            for (Injectable<Method> method : injectMethods)
                method.element().invoke(instance, method.toDependencies(context));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return Stream.concat(Stream.concat(Stream.of(injectConstructor), injectFields.stream()), injectMethods.stream())
                .flatMap(i -> Arrays.stream(i.required)).toList();
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required){

        static <Element extends Executable> Injectable<Element> of(Element method) {
            ComponentRef<?>[] dependencies = Arrays.stream(method.getParameters()).map(Injectable::toComponentRef)
                    .toArray(ComponentRef<?>[]::new);
            return new Injectable<>(method, dependencies);
        }

        static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef[]{toComponentRef(field)});
        }

        private static ComponentRef<?> toComponentRef(Field field) {
            Annotation qualifier = getQualifier(field);
            return ComponentRef.of(field.getGenericType(), qualifier);
        }

        private static ComponentRef<?> toComponentRef(Parameter parameter) {
            Annotation qualifier = getQualifier(parameter);
            return ComponentRef.of(parameter.getParameterizedType(), qualifier);
        }

        private static Annotation getQualifier(AnnotatedElement parameter) {
            List<Annotation> qualifiers = Arrays.stream(parameter.getAnnotations())
                    .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
            if (qualifiers.size() > 1) throw new IllegalComponentException();
            return qualifiers.stream().findFirst().orElse(null);
        }

        Object[] toDependencies(Context context) {
            return Arrays.stream(required).map(context::get).map(Optional::get).toArray();
        }
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

    private static <Type> Constructor<Type> getDefaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
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

}
