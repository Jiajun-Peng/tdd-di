package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
public class InjectionTest {

    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructorInjection {
        // No args constructor
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            config.bind(Component.class, ComponentWithDefaultConstructor.class);

            Component instance = config.getContext().get(Component.class).get();

            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        // with dependencies
        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, dependency);

            Component instance = config.getContext().get(Component.class).get();
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
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "Hello World!");

            Component instance = config.getContext().get(Component.class).get();
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

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {
        }

        // field injection
        @Test
        public void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(
                    FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
            FieldInjection.ComponentWithFieldInjection component =
                    config.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();

            assertSame(dependency, component.dependency);
        }

        // field injection in subclass
        @Test
        public void should_inject_dependency_via_superclass_inject_filed() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.SubclassWithFieldInjection.class,
                    FieldInjection.SubclassWithFieldInjection.class);
            FieldInjection.SubclassWithFieldInjection component =
                    config.getContext().get(FieldInjection.SubclassWithFieldInjection.class).get();

            assertSame(dependency, component.dependency);
        }

        // provide dependencies information for field injection
        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider =
                    new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);

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
                    () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
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
            config.bind(
                    MethodInjection.InjectMethodWithNoDependencies.class,
                    MethodInjection.InjectMethodWithNoDependencies.class);
            MethodInjection.InjectMethodWithNoDependencies instance =
                    config.getContext().get(MethodInjection.InjectMethodWithNoDependencies.class).get();

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
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(
                    MethodInjection.InjectMethodWithDependencies.class,
                    MethodInjection.InjectMethodWithDependencies.class);
            MethodInjection.InjectMethodWithDependencies instance =
                    config.getContext().get(MethodInjection.InjectMethodWithDependencies.class).get();

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

        static class SubclassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            config.bind(MethodInjection.SubclassWithInjectMethod.class, MethodInjection.SubclassWithInjectMethod.class);
            MethodInjection.SubclassWithInjectMethod
                    instance = config.getContext().get(MethodInjection.SubclassWithInjectMethod.class).get();

            assertEquals(1, instance.superCalled);
            assertEquals(2, instance.subCalled);
        }

        static class SubclassWithOverrideInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_superclass_inject_method_with_inject() {
            config.bind(
                    MethodInjection.SubclassWithOverrideInjectMethod.class,
                    MethodInjection.SubclassWithOverrideInjectMethod.class);
            MethodInjection.SubclassWithOverrideInjectMethod instance =
                    config.getContext().get(MethodInjection.SubclassWithOverrideInjectMethod.class).get();

            assertEquals(1, instance.superCalled);
        }

        static class SubclassWithOverrideInjectMethodWithoutInject extends
                MethodInjection.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_superclass_inject_method_without_inject() {
            config.bind(MethodInjection.SubclassWithOverrideInjectMethodWithoutInject.class,
                    MethodInjection.SubclassWithOverrideInjectMethodWithoutInject.class);
            MethodInjection.SubclassWithOverrideInjectMethodWithoutInject instance =
                    config.getContext().get(MethodInjection.SubclassWithOverrideInjectMethodWithoutInject.class).get();

            assertEquals(0, instance.superCalled);
        }


        // include dependencies from inject methods
        @Test
        public void should_include_method_dependency_in_dependencies() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependencies> provider =
                    new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependencies.class);

            assertArrayEquals(new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray(Class<?>[]::new));
        }


        // throw exception if type parameter defined
        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }
    }
}
