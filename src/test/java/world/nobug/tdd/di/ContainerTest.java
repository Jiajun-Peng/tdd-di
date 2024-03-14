package world.nobug.tdd.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import world.nobug.tee.di.Context;

import static org.junit.jupiter.api.Assertions.assertSame;

public class ContainerTest {

    // 组件构造相关的测试类
    @Nested
    public class ComponentConstruction{

        interface Component{
        }

        // TODO: instance
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
            // TODO: No args constructor
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
