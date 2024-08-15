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

    Dependency dependency = Mockito.mock(Dependency.class);
    Context context = Mockito.mock(Context.class);

    @BeforeEach
    public void setUp() {
        Mockito.when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            // No args constructor
            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                Component instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            // with dependencies
            @Test
            public void should_inject_dependency_via_inject_constructor() {
                ComponentWithInjectConstructor instance = new InjectionProvider<>(ComponentWithInjectConstructor.class).get(context);

                assertNotNull(instance);
                assertSame(dependency, instance.dependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<ComponentWithInjectConstructor> provider =
                        new InjectionProvider<>(ComponentWithInjectConstructor.class);

                assertArrayEquals(new Class<?>[]{Dependency.class},
                        provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectConstructor {

            // abstract class
            abstract class AbstractComponent implements Component {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(AbstractComponent.class));
            }

            // interface
            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(Component.class));
            }

            // sad path
            // multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(ComponentWithMultiInjectConstructors.class));
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(
                        ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }
        }

    }

    @Nested
    public class FieldInjection {

        @Nested
        class Injection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            // field injection
            @Test
            public void should_inject_dependency_via_field() {

                ComponentWithFieldInjection component =
                        new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            // field injection in subclass
            @Test
            public void should_inject_dependency_via_superclass_inject_filed() {

                SubclassWithFieldInjection component =
                        new InjectionProvider<>(SubclassWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            // provide dependencies information for field injection
            @Test
            public void should_include_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFieldInjection> provider =
                        new InjectionProvider<>(ComponentWithFieldInjection.class);

                assertArrayEquals(new Class<?>[]{Dependency.class},
                        provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectFields {
            // throw exception if filed is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {
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
                InjectMethodWithNoDependencies instance =
                        new InjectionProvider<>(InjectMethodWithNoDependencies.class).get(context);

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
                InjectMethodWithDependencies instance =
                        new InjectionProvider<>(InjectMethodWithDependencies.class).get(context);

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
                SubclassWithInjectMethod instance =
                        new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);

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
                SubclassWithOverrideInjectMethod instance =
                        new InjectionProvider<>(SubclassWithOverrideInjectMethod.class).get(context);

                assertEquals(1, instance.superCalled);
            }

            static class SubclassWithOverrideInjectMethodWithoutInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_superclass_inject_method_without_inject() {
                SubclassWithOverrideInjectMethodWithoutInject instance =
                        new InjectionProvider<>(SubclassWithOverrideInjectMethodWithoutInject.class).get(
                                context);

                assertEquals(0, instance.superCalled);
            }


            // include dependencies from inject methods
            @Test
            public void should_include_dependency_from_inject_method() {
                InjectionProvider<InjectMethodWithDependencies> provider = new InjectionProvider<>(InjectMethodWithDependencies.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectMethods {
            // throw exception if type parameter defined
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

    }

    static class ComponentWithDefaultConstructor implements Component{
        public ComponentWithDefaultConstructor(){
        }
    }

    static class ComponentWithInjectConstructor implements Component{
        private Dependency dependency;

        @Inject
        public ComponentWithInjectConstructor(Dependency dependency){
            this.dependency = dependency;
        }

        // 用于测试验证dependency是否被注入
        public Dependency dependency() {
            return dependency;
        }
    }

    static class ComponentWithMultiInjectConstructors implements Component{

        @Inject
        public ComponentWithMultiInjectConstructors(String name, Double value){
        }

        @Inject
        public ComponentWithMultiInjectConstructors(String name){
        }
    }

    static class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {

        public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
        }
    }
}
