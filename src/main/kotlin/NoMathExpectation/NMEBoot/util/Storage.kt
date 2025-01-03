package NoMathExpectation.NMEBoot.util

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

inline fun <reified T : Any> mutableStorageOf(path: String, default: T): MutableStorage<T> =
    NotNullKStoreStorage.of(path, default)

inline fun <reified T : Any> storageOf(path: String, default: T): Storage<T> = mutableStorageOf(path, default)

inline fun <reified T : Any> mutableNullableStorageOf(path: String, default: T? = null): MutableStorage<T?> =
    NullableKStoreStorage.of(path, default)

inline fun <reified T : Any> nullableStorageOf(path: String, default: T? = null): Storage<T?> =
    mutableNullableStorageOf(path, default)

inline fun <reified K, reified V> mutableMapStorageOf(
    path: String,
    noinline defaultCompute: (K) -> V
): MutableMapStorage<K, V> =
    MapKStoreStorage.of(path, defaultCompute)

@RequiresOptIn("This api is for internal use only.")
@Retention(AnnotationRetention.BINARY)
annotation class InternalStorageApi

@InternalStorageApi
val storageJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

interface Storage<out T> {
    val path: String

    suspend fun reload() {}
    suspend fun get(): T
    suspend fun <R> referenceUpdate(block: suspend (T) -> R): R
}

interface MutableStorage<T> : Storage<T> {
    suspend fun set(value: T)
    suspend fun update(block: suspend (T) -> T): T
    suspend fun reset()
}

open class NullableKStoreStorage<T : @Serializable Any> @InternalStorageApi constructor(
    override val path: String,
    private val store: KStore<T>,
) : MutableStorage<T?> {
    override suspend fun get() = store.get()

    override suspend fun set(value: T?) = store.set(value)

    protected val mutex = Mutex()

    override suspend fun update(block: suspend (T?) -> T?): T? = mutex.withLock {
        val value = block(get())
        set(value)
        return value
    }

    override suspend fun <R> referenceUpdate(block: suspend (T?) -> R): R = mutex.withLock {
        val value = get()
        val result = block(value)
        set(value)
        return result
    }

    override suspend fun reset() = store.reset()

    @OptIn(InternalStorageApi::class)
    companion object {
        inline fun <reified T : @Serializable Any> of(path: String, default: T? = null): NullableKStoreStorage<T> {
            File(path).parentFile.mkdirs()
            return NullableKStoreStorage(path, storeOf(Path(path), default, json = storageJson))
        }

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

    override suspend fun update(block: suspend (T) -> T): T = mutex.withLock {
        val value = block(get())
        set(value)
        return value
    }

    override suspend fun <R> referenceUpdate(block: suspend (T) -> R): R = mutex.withLock {
        val value = get()
        val result = block(value)
        set(value)
        return result
    }

    override suspend fun reset() = store.reset()

    @OptIn(InternalStorageApi::class)
    companion object {
        inline fun <reified T : @Serializable Any> of(path: String, default: T): NotNullKStoreStorage<T> {
            File(path).parentFile.mkdirs()
            return NotNullKStoreStorage(path, storeOf(Path(path), default, json = storageJson), default)
        }

    }
}

interface MutableMapStorage<K, V> : Storage<MutableMap<K, V>> {
    suspend fun containsKey(key: K) = get().containsKey(key)
    suspend fun containsValue(value: V) = get().containsValue(value)
    suspend fun get(key: K): V
    suspend fun set(key: K, value: V) {
        referenceUpdate { it[key] = value }
    }

    suspend fun clear() = referenceUpdate { it.clear() }

    suspend fun getOrPut(key: K, compute: suspend (K) -> V): V
    suspend fun update(key: K, block: suspend (V) -> V): V
    suspend fun <R> referenceUpdate(key: K, block: suspend (V) -> R): R
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
        val value = map.getOrPut(key) { defaultCompute(key) }
        map[key] = block(value)
        set(map)
        value
    }

    override suspend fun <R> referenceUpdate(key: K, block: suspend (V) -> R): R = mutex.withLock {
        val map = get()
        val value = map.getOrPut(key) { defaultCompute(key) }
        val result = block(value)
        set(map)
        result
    }

    companion object {
        inline fun <reified K : @Serializable Any?, reified V : @Serializable Any?> of(
            path: String,
            noinline defaultCompute: (K) -> V
        ): MapKStoreStorage<K, V> {
            File(path).parentFile.mkdirs()
            return MapKStoreStorage(path, storeOf(Path(path), mutableMapOf(), json = storageJson), defaultCompute)
        }
    }
}