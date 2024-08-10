package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

public class ContainerTest {
    Context context;

    @BeforeEach
    public void setUp(){
        context = new Context();
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
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class).get());
        }

        // TODO: abstract class
        // TODO: interface

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isEmpty());
        }


        @Nested
        public class ConstructorInjection{
            // No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            // with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // A -> B -> C
            @Test
            public void should_bind_type_to_a_class_with_inject_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "Hello World!");

                Component instance = context.get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = context.get(Dependency.class).get();
                assertNotNull(dependency);

                assertEquals("Hello World!", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // sad path
            // multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class);
                });

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }
            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class); // 缺失 String 类型的依赖

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class);
                });

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            // cyclic dependencies
            @Test // A -> B -> A
            public void should_throw_exception_if_cyclic_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception =
                        assertThrows(CyclicDependenciesException.class, () -> context.get(Component.class));

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }
            @Test // A -> B -> C -> A
            public void should_throw_exception_if_transitive_cyclic_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                context.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                assertThrows(CyclicDependenciesException.class, () -> context.get(Component.class));
            }

        }

        @Nested
        public class FieldInjection{

        }

        @Nested
        public class MethodInjection{

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
