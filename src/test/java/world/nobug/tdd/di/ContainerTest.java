package world.nobug.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    @Nested
    public class ComponentConstruction{

        ContextConfig config;

        @BeforeEach
        public void setUp() {
            config = new ContextConfig();
        }

        // instance
        @Test
        public void should_bind_type_to_a_specific_instance(){
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Component.class).get());
        }
        // TODO: abstract class
        // TODO: interface

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_defined(){
//            assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection{
            // No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor(){
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = config.getContext().get(Component.class).get();

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }
            // with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor(){
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }


            // A -> B -> C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies(){
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "Hello, World!");

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("Hello, World!", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // sad path
            // multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided(){
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided(){
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            public void should_throw_exception_if_dependencies_not_found(){
                config.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext().get(Component.class).get());

                // 当组件未找到时，希望能确定是哪个组件未找到，所以需要在异常中记录未找到的组件信息
                assertEquals(Dependency.class, exception.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependencies_not_found(){
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext().get(Component.class));

                // 如果是更深层的组件的依赖未找到，那么仅仅反馈一个String类型的依赖未找到是不够的
                assertEquals(String.class, exception.getDependency());
                // 是DependencyWithInjectConstructor中的String类型的依赖未找到，所以此时的DependencyWithInjectConstructor是组件，且组件的类型是Dependency
                assertEquals(Dependency.class, exception.getComponent());
            }

            // cyclic dependencies
            @Test
            public void should_throw_exception_if_cyclic_dependencies_found(){
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext().get(Component.class));

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            // transitive cyclic dependencies：A -> B -> C -> A
            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found(){
                config.bind(Component.class, ComponentWithInjectConstructor.class); // Component依赖于Dependency
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class); // Dependency依赖于AnotherDependency
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class); // AnotherDependency依赖于Component

                CyclicDependenciesException exception = assertThrows(CyclicDependenciesException.class, () -> config.getContext().get(Component.class));

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());
                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }

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
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component{
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component{

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency{
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component{
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyDependedOnComponent implements Dependency{
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency{
    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency{
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    public AnotherDependency getAnotherDependency() {
        return anotherDependency;
    }
}