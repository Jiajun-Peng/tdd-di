package world.nobug.tdd.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    @Nested
    public class ComponentConstruction{

        interface Component{
        }

        static class ComponentWithDefaultConstructor implements Component{
            public ComponentWithDefaultConstructor() {
            }
        }

        // instance
        @Test
        public void should_bind_type_to_a_specific_instance(){
            Context context = new Context();

            Component instance = new Component() {
            };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));
        }
        // TODO: abstract class
        // TODO: interface

        @Nested
        public class ConstructorInjection{
            // No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor(){
                Context context = new Context();

                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }
            // TODO: with dependencies
            // TODO: A -> B -> C
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
