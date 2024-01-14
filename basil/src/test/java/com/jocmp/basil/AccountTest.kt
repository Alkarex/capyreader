package com.jocmp.basil

import EditFeedForm
import com.jocmp.basil.accounts.LocalAccountDelegate
import com.jocmp.basil.db.Database
import com.jocmp.feedfinder.FeedFinder
import io.mockk.EqMatcher
import io.mockk.coEvery
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountTest {
    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private lateinit var database: Database

    private val defaultEntry = AddFeedForm(
        url = THE_VERGE_URL,
        name = "The Verge",
        folderTitles = listOf()
    )

    @Before
    fun setup() {
        mockkConstructor(FeedFinder::class)
        mockkConstructor(LocalAccountDelegate::class)

        coEvery {
            constructedWith<FeedFinder>(EqMatcher(THE_VERGE_URL)).find()
        } returns FeedFinder.Result.Success(listOf(TheVergeFeed()))

        coEvery {
            constructedWith<FeedFinder>(EqMatcher(ARS_TECHNICA_URL)).find()
        } returns FeedFinder.Result.Success(listOf(ArsTechnicaFeed()))

        coEvery {
            anyConstructed<LocalAccountDelegate>().fetchAll(any())
        } returns emptyList()

        database = InMemoryDatabaseProvider.forAccount("777")
    }

    private fun buildAccount(id: String = "777", path: File = folder.newFile()): Account {
        return Account(
            id = id,
            path = path.toURI(),
            database = database,
        )
    }

    @Test
    fun opmlFile_endsWithSubscriptions() {
        val accountPath = folder.newFile()

        val account = buildAccount(id = "777", path = accountPath)

        assertContains(account.opmlFile.path.toString(), Regex("/subscriptions.opml$"))
    }

    @Test
    fun constructor_loadsExistingFeeds() {
        val accountPath = folder.newFile()
        val accountID = "777"

        runBlocking {
            val previousInstance = buildAccount(id = accountID, path = accountPath)
            previousInstance.addFolder(title = "Test Title")
            previousInstance.addFeed(
                AddFeedForm(
                    url = THE_VERGE_URL,
                    name = "The Verge",
                    folderTitles = listOf(),
                )
            )
        }

        val account = buildAccount(id = accountID, path = accountPath)
        val accountTitle = account.folders.first().title

        assertEquals(expected = "Test Title", actual = accountTitle)
        assertEquals(expected = 1, actual = account.feeds.size)
    }

    @Test
    fun addFeed_singleTopLevelFeed() {
        val accountPath = folder.newFile()
        val account = buildAccount(id = "777", path = accountPath)
        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = listOf(),
        )

        runBlocking { account.addFeed(entry) }

        assertEquals(expected = account.feeds.size, actual = 1)
        assertEquals(expected = account.folders.size, actual = 0)

        val feed = account.feeds.first()
        assertEquals(expected = entry.name, actual = entry.name)
        assertEquals(expected = entry.url, actual = feed.feedURL)
    }

    @Test
    fun addFeed_newFolder() {
        val accountPath = folder.newFile()
        val account = buildAccount(id = "777", path = accountPath)
        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = listOf("Tech"),
        )

        runBlocking { account.addFeed(entry) }

        assertEquals(expected = account.feeds.size, actual = 0)
        assertEquals(expected = account.folders.size, actual = 1)

        val feed = account.folders.first().feeds.first()
        assertEquals(expected = entry.name, actual = entry.name)
        assertEquals(expected = entry.url, actual = feed.feedURL)
    }

    @Test
    fun addFeed_existingFolders() {
        val accountPath = folder.newFile()
        val account = buildAccount(id = "777", path = accountPath)
        runBlocking { account.addFolder("Tech") }

        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = listOf("Tech"),
        )

        runBlocking { account.addFeed(entry) }

        assertEquals(expected = account.feeds.size, actual = 0)
        assertEquals(expected = account.folders.size, actual = 1)

        val feed = account.folders.first().feeds.first()
        assertEquals(expected = entry.name, actual = feed.name)
        assertEquals(expected = entry.url, actual = feed.feedURL)
    }

    @Test
    fun addFeed_multipleFolders() {
        val accountPath = folder.newFile()
        val account = buildAccount(id = "777", path = accountPath)
        runBlocking { account.addFolder("Tech") }

        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = listOf("Tech", "Culture"),
        )

        runBlocking { account.addFeed(entry) }

        assertEquals(expected = account.feeds.size, actual = 0)
        assertEquals(expected = account.folders.size, actual = 2)

        val techFeed = account.folders.first().feeds.first()
        val cultureFeed = account.folders.first().feeds.first()
        assertEquals(expected = entry.name, actual = techFeed.name)
        assertEquals(expected = entry.url, actual = techFeed.feedURL)
        assertEquals(techFeed, cultureFeed)
    }

    @Test
    fun removeFeed_topLevelFeed() {
        val account = buildAccount()
        runBlocking {
            account.addFeed(
                AddFeedForm(
                    url = "https://theverge.com/rss/index.xml",
                    name = "The Verge",
                    folderTitles = listOf(),
                )
            )
        }

        val feed = account.feeds.find { it.name == "The Verge" }!!

        assertEquals(expected = 1, account.feeds.size)

        runBlocking { account.removeFeed(feedID = feed.id) }

        assertEquals(expected = 0, account.feeds.size)
    }

    @Test
    fun editFeed_topLevelFeed() {
        val account = buildAccount()
        val feed = runBlocking { account.addFeed(defaultEntry) }.getOrNull()!!

        val feedName = "The Verge Mobile"

        val editedFeed = runBlocking {
            account.editFeed(EditFeedForm(feedID = feed.id, name = feedName))
        }.getOrNull()!!

        assertEquals(expected = feedName, actual = editedFeed.name)
    }

    @Test
    fun editFeed_nestedFeed() {
        val account = buildAccount()
        val feed = runBlocking {
            account.addFeed(defaultEntry.copy(folderTitles = listOf("Tech")))
        }.getOrNull()!!

        val feedName = "The Verge Mobile"

        runBlocking {
            account.editFeed(EditFeedForm(feedID = feed.id, name = feedName, folderTitles = listOf("Tech")))
        }

        val renamedFeed = account.folders.first().feeds.first()

        assertEquals(expected = feedName, actual = renamedFeed.name)
    }

    @Test
    fun editFeed_movedFeedFromTopLevelToFolder() {
        val account = buildAccount()
        val feed = runBlocking {
            account.addFeed(defaultEntry)
        }.getOrNull()!!

        val feedName = "The Verge Mobile"

        runBlocking {
            account.editFeed(EditFeedForm(feedID = feed.id, name = feedName, folderTitles = listOf("Tech")))
        }

        assertTrue(account.feeds.isEmpty())
        assertEquals(expected = 1, actual = account.folders.size)

        val renamedFeed = account.folders.first().feeds.first()

        assertEquals(expected = feedName, actual = renamedFeed.name)
    }

    @Test
    fun editFeed_movedFeedFromFolderToTopLevel() {
        val account = buildAccount()

        val feed = runBlocking {
            account.addFeed(defaultEntry)
        }.getOrNull()!!

        val otherFeed = runBlocking {
            account.addFeed(
                AddFeedForm(
                    url = ARS_TECHNICA_URL,
                    name = "Ars Technica",
                    folderTitles = listOf("Tech")
                )
            )
        }.getOrNull()!!

        val feedName = "The Verge"

        runBlocking {
            account.editFeed(EditFeedForm(feedID = feed.id, name = feedName, folderTitles = listOf()))
        }

        assertEquals(expected = 1, actual = account.feeds.size)
        assertEquals(expected = 1, actual = account.folders.size)

        val movedFeed = account.feeds.first()
        val existingFeed = account.folders.first().feeds.first()

        assertEquals(expected = feedName, actual = movedFeed.name)
        assertEquals(expected = otherFeed.name, actual = existingFeed.name)
    }

    @Test
    fun editFeed_movedFeedFromFolderToTopLevelWithOtherFeeds() {
        val account = buildAccount()
        val feed = runBlocking {
            account.addFeed(defaultEntry)
        }.getOrNull()!!

        val feedName = "The Verge Mobile"

        runBlocking {
            account.editFeed(EditFeedForm(feedID = feed.id, name = feedName, folderTitles = listOf("Tech")))
        }

        assertTrue(account.feeds.isEmpty())
        assertEquals(expected = 1, actual = account.folders.size)

        val renamedFeed = account.folders.first().feeds.first()

        assertEquals(expected = feedName, actual = renamedFeed.name)
    }

    @Test
    fun findFeed_topLevelFeed() {
        val account = buildAccount(id = "777", path = folder.newFile())

        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = emptyList()
        )

        val feedID = runBlocking { account.addFeed(entry) }.getOrNull()!!.id

        val result = account.findFeed(feedID)!!

        assertEquals(expected = feedID, actual = result.id)
    }

    @Test
    fun findFeed_nestedFeed() {
        val account = buildAccount(id = "777", path = folder.newFile())

        val entry = defaultEntry.copy(folderTitles = listOf("Tech", "Culture"))

        val feedID = runBlocking { account.addFeed(entry) }.getOrNull()!!.id

        val result = account.findFeed(feedID)!!

        assertEquals(expected = feedID, actual = result.id)
    }

    @Test
    fun findFeed_feedDoesNotExist() {
        val account = buildAccount(id = "777", path = folder.newFile())

        val result = account.findFeed("missing")

        assertNull(result)
    }

    @Test
    fun findFolder_existingFolder() {
        val account = buildAccount(id = "777", path = folder.newFile())

        val entry = AddFeedForm(
            url = "https://theverge.com/rss/index.xml",
            name = "The Verge",
            folderTitles = listOf("Tech", "Culture")
        )

        runBlocking { account.addFeed(entry) }

        val result = account.findFolder("Tech")!!

        assertEquals(expected = "Tech", actual = result.title)
    }

    @Test
    fun findFolder_folderDoesNotExist() {
        val account = buildAccount(id = "777", path = folder.newFile())

        val result = account.findFolder("Tech")

        assertNull(result)
    }
}
