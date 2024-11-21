import kotlinx.coroutines.*
import app.cash.sqldelight.async.coroutines.awaitAsList
import ninja.bryansills.sqldelightplayground.DriverFactory
import ninja.bryansills.sqldelightplayground.createDatabase
import kotlin.system.exitProcess

fun main() {
    val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    mainScope.launchBlocking {
        val database = createDatabase(DriverFactory())
        val queries = database.songQueries

        val numberOfSongs = 1000
        val numberOfPlaylists = 250
        val songsPerPlaylist = (10..25)

        database.transaction {
            repeat(numberOfSongs) { songIndex ->
                queries.insert_song(
                    externalId = (1000 + songIndex).toString(),
                    name = "song #$songIndex"
                )
            }

            repeat(numberOfPlaylists) { playlistIndex ->
                val playlistExternalId = (5000 + playlistIndex).toString()
                queries.insert_playlist(
                    externalId = playlistExternalId,
                    name = "playlist #$playlistIndex"
                )

                val songsForThisPlaylist = (0 until numberOfSongs)
                    .shuffled()
                    .take(songsPerPlaylist.random())

                songsForThisPlaylist.forEach { rawSongIndex ->
                    val songExternalId = (1000 + rawSongIndex).toString()
                    queries.insert_playlist_song(
                        songExternalId = songExternalId,
                        playlistExternalId = playlistExternalId
                    )
                }
            }
        }

        val results = queries.get_all_songs().awaitAsList()
        println(results.toString())
    }
}

/**
 * Do some work and then kill the process. We don't want Android Studio to think that we are still
 * waiting for work to finish.
 */
private fun CoroutineScope.launchBlocking(block: suspend CoroutineScope.() -> Unit) {
    try {
        runBlocking(this.coroutineContext) {
            block()
            this.cancel("Time to die.")
        }
    } catch (ex: Exception) {
        if (ex !is CancellationException) {
            ex.printStackTrace()
        }
    }

    exitProcess(0)
}
