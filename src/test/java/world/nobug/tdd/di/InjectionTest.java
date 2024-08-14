package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Nested
public class InjectionTest {

    ContextConfig config;
    Dependency dependency = Mockito.mock(Dependency.class);
    Context context = Mockito.mock(Context.class);

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
        config.bind(Dependency.class, dependency);

        Mockito.when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {
        // No args constructor
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);

            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        // with dependencies
        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        // abstract class
        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class));
        }

        // interface
        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(Component.class));
        }

        // A -> B -> C
        @Test
        public void should_bind_type_to_a_class_with_inject_transitive_dependencies() {
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "Hello World!");

            Mockito.when(context.get(eq(Dependency.class)))
                    .thenReturn(Optional.of(new DependencyWithInjectConstructor("Hello World!")));

            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);

            Dependency dependency = config.getContext().get(Dependency.class).get();
            assertNotNull(dependency);

            assertEquals("Hello World!", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        // sad path
        // multi inject constructors
        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class));
        }

        // no default constructor and inject constructor
        @Test
        public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(
                    ComponentWithNoInjectConstructorNorDefaultConstructor.class));
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider =
                    new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);

            assertArrayEquals(new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray(Class<?>[]::new));
        }

    }

    private <T, I extends T> T getComponent(Class<T> type, Class<I> implementation) {
        ConstructorInjectionProvider<I> provider = new ConstructorInjectionProvider<>(implementation);
        return provider.get(context);
    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
        }

        // field injection
        @Test
        public void should_inject_dependency_via_field() {

            ComponentWithFieldInjection component = getComponent(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

            assertSame(dependency, component.dependency);
        }

        // field injection in subclass
        @Test
        public void should_inject_dependency_via_superclass_inject_filed() {

            SubclassWithFieldInjection component = getComponent(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);

            assertSame(dependency, component.dependency);
        }

        // provide dependencies information for field injection
        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                    new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);

            assertArrayEquals(new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray(Class<?>[]::new));
        }


        // throw exception if filed is final
        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_field_is_final() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
        }

    }

    @Nested
    public class MethodInjection {
        static class InjectMethodWithNoDependencies {
            boolean called = false; // 用于验证方法是否被调用

            @Inject
            void install() {
                called = true;
            }
        }

        // inject method with no dependencies will be called
        @Test
        public void should_call_inject_method_with_no_dependencies() {
            InjectMethodWithNoDependencies instance = getComponent(InjectMethodWithNoDependencies.class, InjectMethodWithNoDependencies.class);

            assertTrue(instance.called);
        }

        static class InjectMethodWithDependencies {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        // inject method with dependencies will be injected
        @Test
        public void should_call_inject_method_with_dependencies() {
            InjectMethodWithDependencies instance = getComponent(InjectMethodWithDependencies.class, InjectMethodWithDependencies.class);

            assertSame(dependency, instance.dependency);
        }

        // override inject method from superclass
        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            SubclassWithInjectMethod instance = getComponent(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);

            assertEquals(1, instance.superCalled);
            assertEquals(2, instance.subCalled);
        }

        static class SubclassWithOverrideInjectMethod extends SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_superclass_inject_method_with_inject() {
            SubclassWithOverrideInjectMethod instance = getComponent(SubclassWithOverrideInjectMethod.class, SubclassWithOverrideInjectMethod.class);

            assertEquals(1, instance.superCalled);
        }

        static class SubclassWithOverrideInjectMethodWithoutInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_superclass_inject_method_without_inject() {
            SubclassWithOverrideInjectMethodWithoutInject instance = getComponent(SubclassWithOverrideInjectMethodWithoutInject.class, SubclassWithOverrideInjectMethodWithoutInject.class);

            assertEquals(0, instance.superCalled);
        }


        // include dependencies from inject methods
        @Test
        public void should_include_method_dependency_in_dependencies() {
            ConstructorInjectionProvider<InjectMethodWithDependencies> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependencies.class);

            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }


        // throw exception if type parameter defined
        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
        }
    }
}
