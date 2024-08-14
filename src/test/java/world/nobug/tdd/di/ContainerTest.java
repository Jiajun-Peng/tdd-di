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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp(){
        config = new ContextConfig();
    }

    // 组件构造相关的测试类
    @Nested
    public class ComponentConstruction{

        // instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            // 创建一个实现了 Component 接口的匿名内部类实例
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Component.class).get());
        }

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }


        @Nested
        public class ConstructorInjection{
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

            // TODO: abstract class
            abstract class AbstractComponent implements Component{
                @Inject
                public AbstractComponent() {
                }
            }
            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
            }

            // TODO: interface

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
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }
            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class); // 缺失 String 类型的依赖

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            // cyclic dependencies
            @Test // A -> B -> A
            public void should_throw_exception_if_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception =
                        assertThrows(CyclicDependenciesException.class, () -> config.getContext());

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }
            @Test // A -> B -> C -> A
            public void should_throw_exception_if_transitive_cyclic_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesException exception =
                        assertThrows(CyclicDependenciesException.class, () -> config.getContext());

                List<Class<?>> components = Arrays.stream(exception.getComponents()).toList();

                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }

        }

        @Nested
        public class FieldInjection{
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }
            // field injection
            @Test
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();

                assertSame(dependency, component.dependency);
            }
            // field injection in subclass
            @Test
            public void should_inject_dependency_via_superclass_inject_filed() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);
                SubclassWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();

                assertSame(dependency, component.dependency);
            }

            // provide dependencies information for field injection
            @Test
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                        new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


            // TODO: throw exception if filed is final

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
                config.bind(InjectMethodWithNoDependencies.class, InjectMethodWithNoDependencies.class);
                InjectMethodWithNoDependencies instance = config.getContext().get(InjectMethodWithNoDependencies.class).get();

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
                config.bind(InjectMethodWithDependencies.class, InjectMethodWithDependencies.class);
                InjectMethodWithDependencies instance = config.getContext().get(InjectMethodWithDependencies.class).get();

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
                config.bind(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);
                SubclassWithInjectMethod instance = config.getContext().get(SubclassWithInjectMethod.class).get();

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
                config.bind(SubclassWithOverrideInjectMethod.class, SubclassWithOverrideInjectMethod.class);
                SubclassWithOverrideInjectMethod instance = config.getContext().get(SubclassWithOverrideInjectMethod.class).get();

                assertEquals(1, instance.superCalled);
            }

            static class SubclassWithOverrideInjectMethodWithoutInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }
            @Test
            public void should_only_call_once_if_subclass_override_superclass_inject_method_without_inject() {
                config.bind(SubclassWithOverrideInjectMethodWithoutInject.class, SubclassWithOverrideInjectMethodWithoutInject.class);
                SubclassWithOverrideInjectMethodWithoutInject instance = config.getContext().get(SubclassWithOverrideInjectMethodWithoutInject.class).get();

                assertEquals(0, instance.superCalled);
            }


            // include dependencies from inject methods
            @Test
            public void should_include_method_dependency_in_dependencies() {
                ConstructorInjectionProvider<InjectMethodWithDependencies> provider =
                        new ConstructorInjectionProvider<>(InjectMethodWithDependencies.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


            // TODO: throw exception if type parameter defined
        }

    }

    // 依赖选择相关的测试类
    @Nested
    public class DependenciesSelection{

    }

    // 生命周期管理相关的测试类
    @Nested
    public class LifecycleManagement{

    }
}


interface Component{
}

interface Dependency{
}

interface AnotherDependency{
}

class ComponentWithDefaultConstructor implements Component{
    public ComponentWithDefaultConstructor(){
    }
}

class ComponentWithInjectConstructor implements Component{
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency){
        this.dependency = dependency;
    }

    // 用于测试验证dependency是否被注入
    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component{

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value){
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name){
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {

    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}


class DependencyWithInjectConstructor implements Dependency{
    // 直接使用字符串类型，不新建接口，简化开发
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency){
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency{
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component){
        this.component = component;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency{
    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component){
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency{
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency){
        this.anotherDependency = anotherDependency;
    }
}
