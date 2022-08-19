package io.getstream.chat.android.client.dependency

import io.getstream.chat.android.core.internal.InternalStreamChatApi
import io.getstream.logging.StreamLog
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal class ExternalDependencies : DependencyResolver {

    private val dependencies = ConcurrentHashMap<KClass<out Any>, Any>()

    @InternalStreamChatApi
    override fun <T : Any> resolveDependency(klass: KClass<T>): T? {
        return dependencies[klass] as? T
    }

    @PublishedApi
    internal fun <T: Any> extendWith(klass: KClass<T>, dependency: T): Boolean {
        if (dependencies.contains(dependency)) return false
        dependencies[klass] = dependency
        return true
    }
}