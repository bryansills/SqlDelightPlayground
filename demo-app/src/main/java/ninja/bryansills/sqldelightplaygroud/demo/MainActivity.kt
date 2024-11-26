package ninja.bryansills.sqldelightplaygroud.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ninja.bryansills.sqldelightplaygroud.demo.ui.theme.SqlDelightPlaygroundTheme
import ninja.bryansills.sqldelightplayground.Database
import ninja.bryansills.sqldelightplayground.DriverFactory
import ninja.bryansills.sqldelightplayground.TransformableKeyedQueryPagingSource
import ninja.bryansills.sqldelightplayground.createDatabase
import ninja.bryansills.sqldelightplayground.preloadDatabase

class MainActivity : ComponentActivity() {

    private lateinit var database: Database
    private var hasLoadedDatabase by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = createDatabase(DriverFactory(this.applicationContext))
        lifecycleScope.launch {
            database.preloadDatabase()
            hasLoadedDatabase = true
        }

        enableEdgeToEdge()
        setContent {
            SqlDelightPlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasLoadedDatabase) {
                        val transformedPagingSource = remember {
                            TransformableKeyedQueryPagingSource(
                                transacter = database.songQueries,
                                context = Dispatchers.IO,
                                pageBoundariesProvider = { anchor: String?, limit: Long ->
                                    database.songQueries.keyed_page_boundaries_songs(limit, anchor)
                                },
                                queryProvider = { beginInclusive: String, endExclusive: String? ->
                                    database.songQueries.get_keyed_paged_songs_with_album_name_with_playlists(beginInclusive, endExclusive)
                                },
                                transform = { databaseRows ->
                                    databaseRows
                                        .groupBy { it.id }
                                        .values
                                        .mapNotNull { rowsForOneSong ->
                                            if (rowsForOneSong.isNotEmpty()) {
                                                val oneRow = rowsForOneSong.first()
                                                SongWithPlaylistsItIsOn(
                                                    name = oneRow.name,
                                                    albumName = oneRow.albumName,
                                                    playedAt = oneRow.played_at,
                                                    playlistsItIsOn = rowsForOneSong.map { it.playlistName }
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                }
                            )
                        }
                        val pager = remember {
                            Pager(
                                config = PagingConfig(pageSize = 25)
                            ) { transformedPagingSource }
                        }
                        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

                        LazyColumn(modifier = Modifier.padding(innerPadding)) {
                            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                                item {
                                    Text(
                                        text = "Waiting for items to load from the backend",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    )
                                }
                            }

                            items(count = lazyPagingItems.itemCount) { index ->
                                val song = lazyPagingItems[index]!!

                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(text = "Title: ${song.name}")
                                    Text(text = "Album Title: ${song.albumName}")
                                    Text(text = "Played at: ${song.playedAt}")
                                    Text(text = "On playlists: ${song.playlistsItIsOn.joinToString()}")
                                }
                            }

                            if (lazyPagingItems.loadState.append == LoadState.Loading) {
                                item {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Text(text = "loading")
                        }
                    }
                }
            }
        }
    }
}

data class SongWithPlaylistsItIsOn(
    val name: String,
    val albumName: String,
    val playedAt: String,
    val playlistsItIsOn: List<String>
)