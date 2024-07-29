package NoMathExpectation.NMEBoot.util

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath

inline fun <reified T : Any> mutableStorageOf(path: String, default: T): MutableStorage<T> =
    NotNullKStoreStorage.of(path, default)

inline fun <reified T : Any> storageOf(path: String, default: T): Storage<T> = mutableStorageOf(path, default)

inline fun <reified T : Any> mutableNullableStorageOf(path: String, default: T? = null): MutableStorage<T?> =
    NullableKStoreStorage.of(path, default)

inline fun <reified T : Any> nullableStorageOf(path: String, default: T? = null): Storage<T?> =
    mutableNullableStorageOf(path, default)

inline fun <reified K, reified V> mutableMapStorageOf(path: String, noinline defaultCompute: (K) -> V): MutableMapStorage<K, V> =
    MapKStoreStorage.of(path, defaultCompute)

@RequiresOptIn("This api is for internal use only.")
@Retention(AnnotationRetention.BINARY)
annotation class InternalStorageApi

interface Storage<out T> {
    val path: String

    suspend fun reload() {}
    suspend fun get(): T
    suspend fun referenceUpdate(block: suspend (T) -> Unit): T
}

interface MutableStorage<T> : Storage<T> {
    suspend fun set(value: T)
    suspend fun update(block: suspend (T) -> T): T
    suspend fun reset()

    override suspend fun referenceUpdate(block: suspend (T) -> Unit) = update {
        block(it)
        it
    }
}

open class NullableKStoreStorage<T : @Serializable Any> @InternalStorageApi constructor(
    override val path: String,
    private val store: KStore<T>,
) : MutableStorage<T?> {
    override suspend fun get() = store.get()

    override suspend fun set(value: T?) = store.set(value)

    protected val mutex = Mutex()

    override suspend fun update(block: suspend (T?) -> T?): T? {
        mutex.withLock {
            val value = block(get())
            set(value)
            return value
        }
    }

    override suspend fun reset() = store.reset()

    @OptIn(InternalStorageApi::class)
    companion object {
        inline fun <reified T : @Serializable Any> of(path: String, default: T? = null) =
            NullableKStoreStorage(path, storeOf(path.toPath(), default))
    }
}

open class NotNullKStoreStorage<T : @Serializable Any> @InternalStorageApi constructor(
    override val path: String,
    private val store: KStore<T>,
    val default: T
) : MutableStorage<T> {
    override suspend fun get() = store.get() ?: default

    override suspend fun set(value: T) = store.set(value)

    protected val mutex = Mutex()

    override suspend fun update(block: suspend (T) -> T): T {
        mutex.withLock {
            val value = block(get())
            set(value)
            return value
        }
    }

    override suspend fun reset() = store.reset()

    @OptIn(InternalStorageApi::class)
    companion object {
        inline fun <reified T : @Serializable Any> of(path: String, default: T) =
            NotNullKStoreStorage(path, storeOf(path.toPath(), default), default)
    }
}

interface MutableMapStorage<K, V> : Storage<MutableMap<K, V>> {
    suspend fun containsKey(key: K) = get().containsKey(key)
    suspend fun containsValue(value: V) = get().containsValue(value)
    suspend fun get(key: K): V
    suspend fun set(key: K, value: V) {
        referenceUpdate { it[key] = value }
    }
    suspend fun getOrPut(key: K, compute: suspend (K) -> V): V
    suspend fun update(key: K, block: suspend (V) -> V): V
}

@OptIn(InternalStorageApi::class)
class MapKStoreStorage<K : @Serializable Any?, V : @Serializable Any?> @InternalStorageApi constructor(
    path: String,
    store: KStore<MutableMap<K, V>>,
    val defaultCompute: (K) -> V
) : NotNullKStoreStorage<MutableMap<K, V>>(path, store, mutableMapOf()), MutableMapStorage<K, V> {
    override suspend fun get(key: K) = mutex.withLock {
        get().getOrPut(key) { defaultCompute(key) }
    }

    override suspend fun getOrPut(key: K, compute: suspend (K) -> V) = mutex.withLock {
        val map = get()
        val value = map[key]
        if (value != null) {
            return@withLock value
        }

        val computed = compute(key)
        map[key] = computed
        set(map)
        return@withLock computed
    }

    override suspend fun update(key: K, block: suspend (V) -> V) = mutex.withLock {
        val map = get()
        val value = block(get(key))
        map[key] = value
        set(map)
        value
    }

    companion object {
        inline fun <reified K : @Serializable Any?, reified V : @Serializable Any?> of(
            path: String,
            noinline defaultCompute: (K) -> V
        ) =
            MapKStoreStorage(path, storeOf(path.toPath(), mutableMapOf()), defaultCompute)
    }
}

suspend inline fun <K, V> MutableMapStorage<K, V>.referenceUpdate(
    key: K,
    crossinline default: suspend (K) -> V,
    crossinline block: suspend (V?) -> Unit
) = update(key) {
    val value = it ?: default(key)
    block(value)
    value
}