package io.getstream.chat.android.client.dependency

import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.logging.StreamLog
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal class ExternalDependencies : DependencyResolver {

    private val logger = StreamLog.getLogger("Chat:ExtDependencies")

    private val dependencies = ConcurrentHashMap<KClass<out Any>, Any>()

    @InternalStreamChatApi
    override fun <T : Any> resolveDependency(klass: KClass<T>): T? {
        val result = dependencies[klass] as? T
        logger.i {
            "[resolveDependency] klass: ${klass.simpleName}, result: $result, dependencies.size: ${dependencies.size}"
        }
        return result
    }

    @PublishedApi
    internal fun <T: Any> extendWith(klass: KClass<T>, dependency: T): Boolean {
        if (dependencies.contains(dependency)) return false
        dependencies[klass] = dependency
        logger.i { "[extendWith] klass: '$klass'" }
        return true
    }
}