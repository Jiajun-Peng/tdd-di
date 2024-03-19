package world.nobug.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    @Nested
    public class ComponentConstruction{

        Context context;

        @BeforeEach
        public void setUp() {
            context = new Context();
        }

        // instance
        @Test
        public void should_bind_type_to_a_specific_instance(){
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));
        }
        // TODO: abstract class
        // TODO: interface

        // TODO: component does not exist
        @Test
        public void should_throw_exception_if_component_does_not_exist(){
            assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));
        }

        @Nested
        public class ConstructorInjection{
            // No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor(){
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }
            // with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor(){
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }


            // A -> B -> C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies(){
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "Hello, World!");

                Component instance = context.get(Component.class);
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
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided(){
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            public void should_throw_exception_if_dependencies_not_found(){
                context.bind(Component.class, ComponentWithInjectConstructor.class);

                assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));
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