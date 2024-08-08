package world.nobug.tdd.di;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            assertSame(instance, context.get(Component.class));
        }

        // TODO: abstract class
        // TODO: interface

        @Nested
        public class ConstructorInjection{
            // No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }
            // TODO: with dependencies
            // TODO: A -> B -> C
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

class ComponentWithDefaultConstructor implements Component{
    public ComponentWithDefaultConstructor(){
    }
}
