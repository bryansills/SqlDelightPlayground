package ninja.bryansills.sqldelightplayground

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return Database(driver = driver)
}

suspend fun Database.preloadDatabase(
    numberOfSongs: Int = 1000,
    numberOfPlaylists: Int = 250,
    songsPerPlaylist: IntRange = (10..25)
) {
    val queries = this.songQueries

    val currentSongs = queries.get_all_songs().awaitAsList()
    if (currentSongs.isNotEmpty()) {
        return
    }

    this.transaction {
        repeat(numberOfSongs) { songIndex ->
            val albumExternalId = (3000 + songIndex).toString()
            queries.insert_song(
                externalId = (1000 + songIndex).toString(),
                name = "song #$songIndex",
                albumExternalId = albumExternalId
            )

            queries.insert_album(
                externalId = albumExternalId,
                name = "album #$songIndex, but cool"
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
}