package ninja.bryansills.sqldelightplayground

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterBase
import app.cash.sqldelight.TransactionCallbacks
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class TransformableKeyedQueryPagingSource<Key : Any, DatabaseRowType : Any, RowType : Any>(
    private val queryProvider: (beginInclusive: Key, endExclusive: Key?) -> Query<DatabaseRowType>,
    private val transform: (List<DatabaseRowType>) -> List<RowType>,
    private val pageBoundariesProvider: (anchor: Key?, limit: Long) -> Query<Key>,
    private val transacter: TransacterBase,
    private val context: CoroutineContext,
) : PagingSource<Key, RowType>(), Query.Listener {

    // Start the code from `QueryPagingSource`
    protected var currentQuery: Query<DatabaseRowType>? by Delegates.observable(null) { _, old, new ->
        old?.removeListener(this)
        new?.addListener(this)
    }

    init {
        registerInvalidatedCallback {
            currentQuery?.removeListener(this)
            currentQuery = null
        }
    }

    override fun queryResultsChanged() = invalidate()
    // End the code from `QueryPagingSource`

    // Start the code from `KeyedQueryPagingSource`
    private var pageBoundaries: List<Key>? = null
    override val jumpingSupported: Boolean get() = false

    override fun getRefreshKey(state: PagingState<Key, RowType>): Key? {
        val boundaries = pageBoundaries ?: return null
        val last = state.pages.lastOrNull() ?: return null
        val keyIndexFromNext = last.nextKey?.let { boundaries.indexOf(it) - 1 }
        val keyIndexFromPrev = last.prevKey?.let { boundaries.indexOf(it) + 1 }
        val keyIndex = keyIndexFromNext ?: keyIndexFromPrev ?: return null

        return boundaries.getOrNull(keyIndex)
    }

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, RowType> {
        return withContext(context) {
            try {
                val getPagingSourceLoadResult: TransactionCallbacks.() -> LoadResult<Key, RowType> = {
                    val boundaries = pageBoundaries
                        ?: pageBoundariesProvider(params.key, params.loadSize.toLong())
                            .executeAsList()
                            .also { pageBoundaries = it }

                    val key = params.key ?: boundaries.first()

                    require(key in boundaries)

                    val keyIndex = boundaries.indexOf(key)
                    val previousKey = boundaries.getOrNull(keyIndex - 1)
                    val nextKey = boundaries.getOrNull(keyIndex + 1)
                    val databaseResults = queryProvider(key, nextKey)
                        .also { currentQuery = it }
                        .executeAsList()

                    val results = transform(databaseResults)

                    LoadResult.Page(
                        data = results,
                        prevKey = previousKey,
                        nextKey = nextKey,
                    ) as LoadResult<Key, RowType>
                }
                when (transacter) {
                    is Transacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
                    is SuspendingTransacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
                }
            } catch (e: Exception) {
                if (e is IllegalArgumentException) throw e
                LoadResult.Error<Key, RowType>(e) as LoadResult<Key, RowType>
            }
        }
    }
    // End the code from `KeyedQueryPagingSource`
}